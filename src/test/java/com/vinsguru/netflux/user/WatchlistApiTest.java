package com.vinsguru.netflux.user;

import com.vinsguru.netflux.user.dto.AddToWatchlistRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@AutoConfigureRestTestClient
@ActiveProfiles("test")
@Sql(scripts = "/sql/insert-watchlist.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/sql/delete-all.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class WatchlistApiTest {

    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String WATCHLIST_URI = "/api/users/watchlist";
    private static final String VALIDATION_FAILED = "Validation failed";
    private static final String MALFORMED_BODY = "Malformed or missing request body";

    @Autowired
    RestTestClient restTestClient;

    @Test
    @DisplayName("Should return only the user's movie ids in insertion order")
    void shouldReturnWatchlistInInsertionOrder() {
        assertThat(getWatchlist(1L)).containsExactly(300L, 100L);
    }

    @Test
    @DisplayName("Should return an empty list for a user with no entries")
    void shouldReturnEmptyWatchlist() {
        assertThat(getWatchlist(99L)).isEmpty();
    }

    @Test
    @DisplayName("Should add a movie to the watchlist")
    void shouldAddMovie() {
        addMovie(1L, 700L);

        assertThat(getWatchlist(1L)).containsExactly(300L, 100L, 700L);
    }

    @Test
    @DisplayName("Should return 201 without creating a duplicate when the movie is already in the watchlist")
    void shouldAddMovieIdempotently() {
        addMovie(1L, 300L);

        assertThat(getWatchlist(1L)).containsExactly(300L, 100L);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidAddRequests")
    @DisplayName("Should return 400 ProblemDetail and leave the watchlist unchanged for an invalid add request")
    void shouldReturn400ForInvalidAddRequest(String scenario, String body, String expectedDetail) {
        var problem = restTestClient.post()
                .uri(WATCHLIST_URI)
                .header(USER_ID_HEADER, "1")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(ProblemDetail.class)
                .returnResult()
                .getResponseBody();

        assertBadRequest(problem, expectedDetail);
        assertThat(getWatchlist(1L)).containsExactly(300L, 100L);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("requestsWithoutUserIdHeader")
    @DisplayName("Should return 400 ProblemDetail when the X-User-Id header is missing")
    void shouldReturn400WhenUserIdHeaderMissing(Function<RestTestClient, RestTestClient.ResponseSpec> request) {
        var problem = request.apply(restTestClient)
                .expectStatus().isBadRequest()
                .expectBody(ProblemDetail.class)
                .returnResult()
                .getResponseBody();

        assertBadRequest(problem, "Missing required header: " + USER_ID_HEADER);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("requestsWithNonNumericUserIdHeader")
    @DisplayName("Should return 400 ProblemDetail when the X-User-Id header is not numeric")
    void shouldReturn400WhenUserIdHeaderNotNumeric(Function<RestTestClient, RestTestClient.ResponseSpec> request) {
        var problem = request.apply(restTestClient)
                .expectStatus().isBadRequest()
                .expectBody(ProblemDetail.class)
                .returnResult()
                .getResponseBody();

        assertBadRequest(problem, "Invalid value for: " + USER_ID_HEADER);
        assertThat(getWatchlist(1L)).containsExactly(300L, 100L);
    }

    @Test
    @DisplayName("Should remove a movie from the watchlist")
    void shouldRemoveMovie() {
        deleteMovie(1L, 300L);

        assertThat(getWatchlist(1L)).containsExactly(100L);
    }

    @Test
    @DisplayName("Should return 204 when the movie is not in the watchlist")
    void shouldRemoveMovieIdempotently() {
        deleteMovie(1L, 999L);

        assertThat(getWatchlist(1L)).containsExactly(300L, 100L);
    }

    @ParameterizedTest(name = "movieId = {0}")
    @MethodSource("invalidPathMovieIds")
    @DisplayName("Should return 400 ProblemDetail and leave the watchlist unchanged for an invalid path movieId")
    void shouldReturn400ForInvalidPathMovieId(String movieId, String expectedDetail) {
        var problem = restTestClient.delete()
                .uri(WATCHLIST_URI + "/{movieId}", movieId)
                .header(USER_ID_HEADER, "1")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(ProblemDetail.class)
                .returnResult()
                .getResponseBody();

        assertBadRequest(problem, expectedDetail);
        assertThat(getWatchlist(1L)).containsExactly(300L, 100L);
    }

    static Stream<Arguments> invalidAddRequests() {
        return Stream.of(
                Arguments.of("movieId missing", "{}", VALIDATION_FAILED),
                Arguments.of("movieId null", """
                        {"movieId": null}""", VALIDATION_FAILED),
                Arguments.of("movieId zero", """
                        {"movieId": 0}""", VALIDATION_FAILED),
                Arguments.of("movieId negative", """
                        {"movieId": -5}""", VALIDATION_FAILED),
                Arguments.of("movieId not numeric", """
                        {"movieId": "abc"}""", MALFORMED_BODY),
                Arguments.of("malformed JSON", "{\"movieId\": ", MALFORMED_BODY),
                Arguments.of("empty body", "", MALFORMED_BODY)
        );
    }

    static Stream<Named<Function<RestTestClient, RestTestClient.ResponseSpec>>> requestsWithoutUserIdHeader() {
        return Stream.of(
                Named.of("GET watchlist", client -> client.get()
                        .uri(WATCHLIST_URI)
                        .exchange()),
                Named.of("POST watchlist", client -> client.post()
                        .uri(WATCHLIST_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new AddToWatchlistRequest(700L))
                        .exchange()),
                Named.of("DELETE watchlist movie", client -> client.delete()
                        .uri(WATCHLIST_URI + "/{movieId}", 300)
                        .exchange())
        );
    }

    static Stream<Named<Function<RestTestClient, RestTestClient.ResponseSpec>>> requestsWithNonNumericUserIdHeader() {
        return Stream.of(
                Named.of("GET watchlist", client -> client.get()
                        .uri(WATCHLIST_URI)
                        .header(USER_ID_HEADER, "abc")
                        .exchange()),
                Named.of("POST watchlist", client -> client.post()
                        .uri(WATCHLIST_URI)
                        .header(USER_ID_HEADER, "abc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new AddToWatchlistRequest(700L))
                        .exchange()),
                Named.of("DELETE watchlist movie", client -> client.delete()
                        .uri(WATCHLIST_URI + "/{movieId}", 300)
                        .header(USER_ID_HEADER, "abc")
                        .exchange())
        );
    }

    static Stream<Arguments> invalidPathMovieIds() {
        return Stream.of(
                Arguments.of("0", VALIDATION_FAILED),
                Arguments.of("-1", VALIDATION_FAILED),
                Arguments.of("abc", "Invalid value for: movieId")
        );
    }

    private void assertBadRequest(ProblemDetail problem, String expectedDetail) {
        assertThat(problem.getStatus()).isEqualTo(400);
        assertThat(problem.getTitle()).isEqualTo("Bad Request");
        assertThat(problem.getDetail()).isEqualTo(expectedDetail);
    }

    private List<Long> getWatchlist(Long userId) {
        return restTestClient.get()
                .uri("/api/users/watchlist")
                .header(USER_ID_HEADER, String.valueOf(userId))
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<List<Long>>() {})
                .returnResult()
                .getResponseBody();
    }

    private void addMovie(Long userId, Long movieId) {
        restTestClient.post()
                .uri("/api/users/watchlist")
                .header(USER_ID_HEADER, String.valueOf(userId))
                .contentType(MediaType.APPLICATION_JSON)
                .body(new AddToWatchlistRequest(movieId))
                .exchange()
                .expectStatus().isCreated()
                .expectBody().isEmpty();
    }

    private void deleteMovie(Long userId, Long movieId) {
        restTestClient.delete()
                .uri("/api/users/watchlist/{movieId}", movieId)
                .header(USER_ID_HEADER, String.valueOf(userId))
                .exchange()
                .expectStatus().isNoContent();
    }
}
