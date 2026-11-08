package com.vinsguru.netflux.user;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jwt.SignedJWT;
import com.vinsguru.netflux.user.dto.LoginRequest;
import com.vinsguru.netflux.user.dto.LoginResponse;
import com.vinsguru.netflux.user.dto.RegisterRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.text.ParseException;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@AutoConfigureRestTestClient
@ActiveProfiles("test")
@Sql(scripts = "/sql/delete-all.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
@SqlMergeMode(SqlMergeMode.MergeMode.MERGE)
class UserApiTest {

    @Autowired
    RestTestClient restTestClient;

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

    @Test
    @DisplayName("Should return 400 ProblemDetail when a register field is blank")
    void shouldReturn400WhenRegisterFieldBlank() {
        var problem = restTestClient.post()
                .uri("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new RegisterRequest("alice", " ", ""))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(ProblemDetail.class)
                .returnResult()
                .getResponseBody();

        assertThat(problem.getStatus()).isEqualTo(400);
        assertThat(problem.getDetail()).isEqualTo("Validation failed");
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

    @Test
    @DisplayName("Should return 400 ProblemDetail when login fields are blank")
    void shouldReturn400WhenLoginFieldBlank() {
        var problem = restTestClient.post()
                .uri("/api/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new LoginRequest("", ""))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(ProblemDetail.class)
                .returnResult()
                .getResponseBody();

        assertThat(problem.getStatus()).isEqualTo(400);
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
