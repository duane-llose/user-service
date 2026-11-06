---
description: Integration testing standards and guidelines
paths:
  - "**/src/test/**/*.java"
---

# Integration Testing Standards

## Test Scope

- Do not mock application layers (controller, service, repository).
- Only mock external dependencies that cannot run locally.
- Tests must validate full request → service → database flow.

## Test Structure

- Follow Arrange → Act → Assert pattern.
- Keep test cases focused on a single behavior.
- Use `@DisplayName` with a clear sentence alongside a short camelCase method name. Do not use underscores or overly long method names.

Example:
```java
@Test
@DisplayName("Should return all items")
void shouldReturnAllItems() { ... }

@Test
@DisplayName("Should return 404 ProblemDetail when ID does not exist")
void shouldReturn404WhenNotFound() { ... }
```

## Test Data

- Do not use `@Transactional` on tests. Prefer `@Sql` scripts to set up and tear down test data.

```java
@Test
@Sql(scripts = "/sql/insert-items.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/sql/delete-items.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
void shouldReturnAllItems() { ... }
```

## API Testing Approach

- Use `RestTestClient` to test REST endpoints.
- Use a random port for full application context testing.
- Assert responses using DTO contracts, not entity structures.
- Validate both success and error responses.
- Test class names must end with `ApiTest`.
- `@AutoConfigureRestTestClient` is required whenever `RestTestClient` is used in the test class.

### Setup

```java
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@AutoConfigureRestTestClient
class SearchApiTest {

    @Autowired
    RestTestClient restTestClient;

    // test methods go here
}
```

### URI Placeholders

```java
restTestClient.get()
        .uri("/api/items/{id}", 1)
        .exchange();

restTestClient.get()
        .uri("/api/items/{id}/reviews/{reviewId}", 1, 5)
        .exchange();
```

### Extracting the Response Body

```java
// Single object
ItemResponse item = restTestClient.get()
        .uri("/api/items/{id}", 1)
        .exchange()
        .expectBody(ItemResponse.class)
        .returnResult()
        .getResponseBody();

// List of objects
List<ItemResponse> items = restTestClient.get()
        .uri("/api/items")
        .exchange()
        .expectBody(new ParameterizedTypeReference<List<ItemResponse>>() {})
        .returnResult()
        .getResponseBody();
```

### GET

```java
var results = restTestClient.get()
        .uri("/api/items")
        .exchange()
        .expectStatus().isOk()
        .expectBody(new ParameterizedTypeReference<List<ItemResponse>>() {})
        .returnResult()
        .getResponseBody();

assertThat(results).hasSize(3);
```

### POST

```java
var item = restTestClient.post()
        .uri("/api/items")
        .contentType(MediaType.APPLICATION_JSON)
        .body(new CreateItemRequest("name"))
        .exchange()
        .expectStatus().isOk()
        .expectBody(ItemResponse.class)
        .returnResult()
        .getResponseBody();

assertThat(item.id()).isNotNull();
```

### PUT

```java
var item = restTestClient.put()
        .uri("/api/items/{id}", 1)
        .contentType(MediaType.APPLICATION_JSON)
        .body(new UpdateItemRequest("newName"))
        .exchange()
        .expectStatus().isOk()
        .expectBody(ItemResponse.class)
        .returnResult()
        .getResponseBody();

assertThat(item.name()).isEqualTo("newName");
```

### DELETE

```java
restTestClient.delete()
        .uri("/api/items/{id}", 1)
        .exchange()
        .expectStatus().isNoContent();
```

## Error Response Validation

- Error responses must follow RFC 7807 `ProblemDetail`.
- Tests must assert correct HTTP status codes and error structure.

Example:
```java
var problem = restTestClient.get()
        .uri("/api/items/{id}", 999)
        .exchange()
        .expectStatus().isNotFound()
        .expectBody(ProblemDetail.class)
        .returnResult()
        .getResponseBody();

assertThat(problem.getStatus()).isEqualTo(404);
```

## Mocking Outbound HTTP Calls (RestClient)

When the service under test calls external services via `RestClient`, use `MockRestServiceServer`. No extra dependency needed, it is included in `spring-boot-starter-test`.

- Add `@AutoConfigureMockRestServiceServer` to the test class. Spring Boot automatically applies the mock to all `RestClient.Builder` beans.
- Set up expectations in the same order as the actual outbound calls are made.
- Call `mockServer.verify()` at the end of each test to assert all expected calls were made.

### Setup

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@AutoConfigureMockRestServiceServer
class ItemApiTest {

    @Autowired
    RestTestClient restTestClient;

    @Autowired
    MockRestServiceServer mockServer;

    // test methods go here
}
```

### Mocking a Response

```java
mockServer.expect(requestTo(containsString("/api/external/items")))
        .andRespond(withSuccess("""
                [{"id": 1, "name": "Item A"}]
                """, MediaType.APPLICATION_JSON));
```

### Verifying the Call Was Made

```java
mockServer.verify();
```

### Mocking Multiple Outbound RestClient Calls

- If the service under test calls several external services (or several endpoints) within a single request, one `MockRestServiceServer` handles all of them.
- Queue expectations in the exact order the calls are actually made.

```java
@Test
@DisplayName("Should aggregate data from multiple external calls")
void shouldAggregateDataFromMultipleCalls() {
    mockServer.expect(requestTo(containsString("/api/external-a/items")))
            .andRespond(withSuccess("""
                    {"id": 1}
                    """, MediaType.APPLICATION_JSON));

    mockServer.expect(requestTo(containsString("/api/external-b/items")))
            .andRespond(withSuccess("""
                    {"id": 2}
                    """, MediaType.APPLICATION_JSON));

    var response = restTestClient.get()
            .uri("/api/aggregate")
            .exchange()
            .expectStatus().isOk()
            .expectBody(AggregateResponse.class)
            .returnResult()
            .getResponseBody();

    mockServer.verify();
}
```