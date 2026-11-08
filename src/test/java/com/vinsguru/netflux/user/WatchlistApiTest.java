package com.vinsguru.netflux.user;

import com.vinsguru.netflux.user.dto.AddToWatchlistRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@AutoConfigureRestTestClient
@ActiveProfiles("test")
@Sql(scripts = "/sql/insert-watchlist.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/sql/delete-all.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class WatchlistApiTest {

    private static final String USER_ID_HEADER = "X-User-Id";

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

    @Test
    @DisplayName("Should return 400 ProblemDetail when movieId is missing")
    void shouldReturn400WhenMovieIdMissing() {
        var problem = postWatchlistExpecting400(new AddToWatchlistRequest(null));

        assertThat(problem.getDetail()).isEqualTo("Validation failed");
    }

    @Test
    @DisplayName("Should return 400 ProblemDetail when movieId is not positive")
    void shouldReturn400WhenMovieIdNotPositive() {
        var problem = postWatchlistExpecting400(new AddToWatchlistRequest(0L));

        assertThat(problem.getDetail()).isEqualTo("Validation failed");
    }

    @Test
    @DisplayName("Should return 400 ProblemDetail when the X-User-Id header is missing")
    void shouldReturn400WhenUserIdHeaderMissing() {
        var problem = restTestClient.get()
                .uri("/api/users/watchlist")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(ProblemDetail.class)
                .returnResult()
                .getResponseBody();

        assertThat(problem.getStatus()).isEqualTo(400);
    }

    @Test
    @DisplayName("Should return 400 ProblemDetail when the X-User-Id header is not numeric")
    void shouldReturn400WhenUserIdHeaderNotNumeric() {
        var problem = restTestClient.delete()
                .uri("/api/users/watchlist/{movieId}", 300)
                .header(USER_ID_HEADER, "abc")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(ProblemDetail.class)
                .returnResult()
                .getResponseBody();

        assertThat(problem.getStatus()).isEqualTo(400);
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

    @Test
    @DisplayName("Should return 400 ProblemDetail when the path movieId is not positive")
    void shouldReturn400WhenPathMovieIdNotPositive() {
        var problem = restTestClient.delete()
                .uri("/api/users/watchlist/{movieId}", 0)
                .header(USER_ID_HEADER, "1")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(ProblemDetail.class)
                .returnResult()
                .getResponseBody();

        assertThat(problem.getDetail()).isEqualTo("Validation failed");
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

    private ProblemDetail postWatchlistExpecting400(AddToWatchlistRequest request) {
        return restTestClient.post()
                .uri("/api/users/watchlist")
                .header(USER_ID_HEADER, "1")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(ProblemDetail.class)
                .returnResult()
                .getResponseBody();
    }
}
