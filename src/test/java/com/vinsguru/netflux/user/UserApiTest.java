package com.vinsguru.netflux.user;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jwt.SignedJWT;
import com.vinsguru.netflux.user.config.RsaKeyProperties;
import com.vinsguru.netflux.user.config.TestRsaKeys;
import com.vinsguru.netflux.user.dto.LoginRequest;
import com.vinsguru.netflux.user.dto.LoginResponse;
import com.vinsguru.netflux.user.dto.RegisterRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.time.Duration;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@AutoConfigureRestTestClient
@ActiveProfiles("test")
@Sql(scripts = "/sql/delete-all.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
@SqlMergeMode(SqlMergeMode.MergeMode.MERGE)
class UserApiTest {

    private static final String VALIDATION_FAILED = "Validation failed";
    private static final String MALFORMED_BODY = "Malformed or missing request body";

    @Autowired
    RestTestClient restTestClient;

    @Autowired
    RsaKeyProperties rsaKeyProperties;

    @Test
    @DisplayName("Should register a user with a hashed password that can then log in")
    void shouldRegisterUser() {
        restTestClient.post()
                .uri("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new RegisterRequest("alice", "secret", "Alice"))
                .exchange()
                .expectStatus().isCreated()
                .expectBody().isEmpty();

        var response = login(new LoginRequest("alice", "secret"));

        assertThat(response.name()).isEqualTo("Alice");
        assertThat(response.token()).isNotBlank();
    }

    @Test
    @DisplayName("Should return 409 ProblemDetail when the username already exists")
    @Sql(scripts = "/sql/insert-users.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void shouldReturn409WhenUsernameExists() {
        var problem = restTestClient.post()
                .uri("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new RegisterRequest("john", "other", "Other John"))
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody(ProblemDetail.class)
                .returnResult()
                .getResponseBody();

        assertThat(problem.getStatus()).isEqualTo(409);
        assertThat(problem.getTitle()).isEqualTo("Conflict");
    }

    @Test
    @DisplayName("Should treat usernames as case-sensitive when registering")
    @Sql(scripts = "/sql/insert-users.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void shouldRegisterUsernameWithDifferentCase() {
        restTestClient.post()
                .uri("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new RegisterRequest("John", "secret", "Capital John"))
                .exchange()
                .expectStatus().isCreated();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidRegisterRequests")
    @DisplayName("Should return 400 ProblemDetail for an invalid register request")
    void shouldReturn400ForInvalidRegisterRequest(String scenario, String body, String expectedDetail) {
        var problem = postExpecting400("/api/users/register", body);

        assertThat(problem.getStatus()).isEqualTo(400);
        assertThat(problem.getTitle()).isEqualTo("Bad Request");
        assertThat(problem.getDetail()).isEqualTo(expectedDetail);
    }

    @Test
    @DisplayName("Should return an RS256 JWT with sub, iat and a 1 hour expiry on login")
    @Sql(scripts = "/sql/insert-users.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void shouldLoginAndReturnToken() throws ParseException {
        var response = login(new LoginRequest("john", "password"));

        assertThat(response.name()).isEqualTo("John Doe");

        var jwt = SignedJWT.parse(response.token());
        var claims = jwt.getJWTClaimsSet();
        assertThat(jwt.getHeader().getAlgorithm()).isEqualTo(JWSAlgorithm.RS256);
        assertThat(claims.getSubject()).isEqualTo("1000");
        assertThat(Duration.between(claims.getIssueTime().toInstant(), claims.getExpirationTime().toInstant()))
                .isEqualTo(Duration.ofHours(1));
    }

    @Test
    @DisplayName("Should sign the login token with the configured RSA private key")
    @Sql(scripts = "/sql/insert-users.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void shouldSignTokenWithConfiguredKey() {
        var decoder = NimbusJwtDecoder.withPublicKey(TestRsaKeys.publicKeyOf(rsaKeyProperties.privateKey()))
                .build();

        var response = login(new LoginRequest("john", "password"));

        // decode() verifies the RS256 signature and rejects the token if it was signed with any other key
        var jwt = decoder.decode(response.token());
        assertThat(jwt.getSubject()).isEqualTo("1000");
    }

    @Test
    @DisplayName("Should reject the login token when verified with a different RSA key")
    @Sql(scripts = "/sql/insert-users.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void shouldRejectTokenVerifiedWithOtherKey() throws Exception {
        var otherKey = (RSAPublicKey) KeyPairGenerator.getInstance("RSA").generateKeyPair().getPublic();
        var decoder = NimbusJwtDecoder.withPublicKey(otherKey)
                .build();

        var response = login(new LoginRequest("john", "password"));

        assertThatThrownBy(() -> decoder.decode(response.token()))
                .isInstanceOf(BadJwtException.class)
                .hasMessageContaining("Signed JWT rejected");
    }

    @Test
    @DisplayName("Should return 401 ProblemDetail for a wrong password")
    @Sql(scripts = "/sql/insert-users.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void shouldReturn401ForWrongPassword() {
        var problem = loginExpecting401(new LoginRequest("john", "wrong"));

        assertThat(problem.getStatus()).isEqualTo(401);
        assertThat(problem.getDetail()).isEqualTo("Invalid credentials");
    }

    @Test
    @DisplayName("Should return the same 401 ProblemDetail for an unknown username")
    void shouldReturn401ForUnknownUsername() {
        var problem = loginExpecting401(new LoginRequest("nobody", "password"));

        assertThat(problem.getStatus()).isEqualTo(401);
        assertThat(problem.getDetail()).isEqualTo("Invalid credentials");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidLoginRequests")
    @DisplayName("Should return 400 ProblemDetail for an invalid login request")
    @Sql(scripts = "/sql/insert-users.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void shouldReturn400ForInvalidLoginRequest(String scenario, String body, String expectedDetail) {
        var problem = postExpecting400("/api/users/login", body);

        assertThat(problem.getStatus()).isEqualTo(400);
        assertThat(problem.getTitle()).isEqualTo("Bad Request");
        assertThat(problem.getDetail()).isEqualTo(expectedDetail);
    }

    static Stream<Arguments> invalidRegisterRequests() {
        return Stream.of(
                Arguments.of("username missing", """
                        {"password": "secret", "name": "Alice"}""", VALIDATION_FAILED),
                Arguments.of("username blank", """
                        {"username": " ", "password": "secret", "name": "Alice"}""", VALIDATION_FAILED),
                Arguments.of("password missing", """
                        {"username": "alice", "name": "Alice"}""", VALIDATION_FAILED),
                Arguments.of("password blank", """
                        {"username": "alice", "password": "", "name": "Alice"}""", VALIDATION_FAILED),
                Arguments.of("name missing", """
                        {"username": "alice", "password": "secret"}""", VALIDATION_FAILED),
                Arguments.of("name blank", """
                        {"username": "alice", "password": "secret", "name": " "}""", VALIDATION_FAILED),
                Arguments.of("empty object", "{}", VALIDATION_FAILED),
                Arguments.of("malformed JSON", "{\"username\": \"alice\",", MALFORMED_BODY),
                Arguments.of("empty body", "", MALFORMED_BODY)
        );
    }

    // uses the seeded user "john", so a validation bypass would surface as 200 or 401 instead of 400
    static Stream<Arguments> invalidLoginRequests() {
        return Stream.of(
                Arguments.of("username missing", """
                        {"password": "password"}""", VALIDATION_FAILED),
                Arguments.of("username blank", """
                        {"username": " ", "password": "password"}""", VALIDATION_FAILED),
                Arguments.of("password missing", """
                        {"username": "john"}""", VALIDATION_FAILED),
                Arguments.of("password blank", """
                        {"username": "john", "password": ""}""", VALIDATION_FAILED),
                Arguments.of("empty object", "{}", VALIDATION_FAILED),
                Arguments.of("malformed JSON", "{\"username\": \"john\",", MALFORMED_BODY),
                Arguments.of("empty body", "", MALFORMED_BODY)
        );
    }

    private ProblemDetail postExpecting400(String uri, String body) {
        return restTestClient.post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(ProblemDetail.class)
                .returnResult()
                .getResponseBody();
    }

    private LoginResponse login(LoginRequest request) {
        return restTestClient.post()
                .uri("/api/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody(LoginResponse.class)
                .returnResult()
                .getResponseBody();
    }

    private ProblemDetail loginExpecting401(LoginRequest request) {
        return restTestClient.post()
                .uri("/api/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody(ProblemDetail.class)
                .returnResult()
                .getResponseBody();
    }
}
