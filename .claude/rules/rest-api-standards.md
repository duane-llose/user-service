---
description: REST API design, validation, exception handling, and error response standards
paths:
  - "**/*.java"
---

# REST API Design

## Request & Response Models

- DTOs are Java `record` types, used for all API request and response models.

```java
// Request
public record CreateItemRequest(String name) {
}

// Response
public record ItemResponse(Long id,
                            String name) {
}
```

- When an endpoint accepts multiple query parameters, group them into a Java `record`. Spring automatically binds query parameters to record fields.

```java
// Query Parameters
// GET /api/items/search?name=foo&category=bar
record SearchRequest(String name,
                     String category) {
}
```

## Validation

- All input validation must use Jakarta Validation annotations on the request DTO. Never manual `if` checks in the controller.

```java
public record CreateOrderRequest(@NotBlank String customerName,
                                  @Email String customerEmail,
                                  @Positive BigDecimal totalAmount,
                                  @Min(1) Integer quantity,
                                  @Size(max = 500) String notes) {
}
```

- For cross-field validation (e.g. at least one field must be present), use `@AssertTrue` on a method inside the record.

```java
record SearchRequest(String name,
                     String category) {

    @AssertTrue(message = "At least one search parameter is required")
    public boolean isAtLeastOnePresent() {
        return Objects.nonNull(name) || Objects.nonNull(category);
    }
}
```

- For a constraint on a single `@RequestParam` or `@PathVariable` (not grouped into a record), place the Jakarta Validation annotation directly on the parameter. Never add `@Validated` on the controller class, Spring MVC validates these natively since Spring Framework 6.1.

```java
@GetMapping
public List<ItemResponse> getByIds(@RequestParam @Size(max = 100) List<Long> ids) { ... }
```

## Endpoint Design

- APIs must follow REST conventions using correct HTTP methods (`GET`, `POST`, `PUT`, `DELETE`).
- APIs must return appropriate HTTP status codes.

### GET

```java
@GetMapping("/{id}")
public ItemResponse getById(@PathVariable Long id) { ... } // 200
```

- For an endpoint using a grouped query-parameter record, add `@Valid` to trigger validation.

```java
@GetMapping
public List<ItemResponse> search(@Valid SearchRequest request) { ... } // 200
```

### POST

```java
@PostMapping
@ResponseStatus(HttpStatus.CREATED)
public ItemResponse create(@Valid @RequestBody CreateItemRequest request) { ... } // 201
```

### PUT

```java
@PutMapping("/{id}")
public ItemResponse update(@PathVariable Long id, @Valid @RequestBody UpdateItemRequest request) { ... } // 200
```

### DELETE

```java
@DeleteMapping("/{id}")
@ResponseStatus(HttpStatus.NO_CONTENT)
public void delete(@PathVariable Long id) { ... } // 204
```

## Exception Handling & Error Responses

- All exceptions must be handled centrally using `@RestControllerAdvice` in the `advice` package.
- `ResponseStatusException` is forbidden. Always use a custom exception handled by `GlobalExceptionHandler`.
- API error responses must follow RFC 7807 `ProblemDetail` format.
- Stack traces or internal system details must never be exposed to clients.

### Handling a Custom Exception

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ItemNotFoundException.class)
    public ProblemDetail handleNotFound(ItemNotFoundException ex) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Not Found");
        return problem;
    }
}
```

### Handling Validation Errors

- `@Valid` throws `MethodArgumentNotValidException`.
- `@RequestParam`/`@PathVariable` violations throw `HandlerMethodValidationException`.

```java
@ExceptionHandler({MethodArgumentNotValidException.class, HandlerMethodValidationException.class})
public ProblemDetail handleValidation(Exception ex) {
    var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed");
    problem.setTitle("Bad Request");
    return problem;
}
```
