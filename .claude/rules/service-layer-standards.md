---
description: Service layer responsibilities and transactional boundaries
paths:
  - "**/*.java"
---

# Service Layer Standards

## Service Layer Responsibilities

- All business logic must reside in the service layer.
- Services must not directly expose entities to controllers.
- Services may return DTOs or domain-specific results.
- Business rule checks (e.g. entity not found) belong in the service layer. These are not input validation.

### Read Operations

```java
// No @Transactional needed
public List<ItemResponse> findAll() { ... }
```

### Write Operations

```java
// @Transactional required for create, update, delete
@Transactional
public ItemResponse create(CreateItemRequest request) { ... }
```

### Business Rule Checks

- Use custom exceptions for domain and application-level errors.

```java
public class ItemNotFoundException extends RuntimeException {
    public ItemNotFoundException(Long id) {
        super("Item not found: " + id);
    }
}

public ItemResponse findById(Long id) {
    return repository.findById(id)
            .map(ItemMapper::toResponse)
            .orElseThrow(() -> new ItemNotFoundException(id));
}
```

## Mapper

- A static utility class with static methods only, for all entity-to-DTO conversions.
- Not a Spring `@Component` bean.
- Naming: `XxxMapper` (e.g. `ItemMapper` for the `Item` entity).

### Mapping

```java
public class ItemMapper {
    public static ItemResponse toResponse(Item item) {
        return new ItemResponse(item.getId(), item.getName());
    }

    public static Item toEntity(CreateItemRequest request) {
        var item = new Item();
        item.setName(request.name());
        return item;
    }
}
```

### Multi-Argument Constructor Calls

- When a constructor call has multiple arguments, put each argument on its own line.

```java
// Wrong
return new ItemSummary(item.getId(), item.getName(), item.getDescription(),
        item.getPrice(), item.getCategory(), item.getCreatedAt());

// Correct
return new ItemSummary(
        item.getId(),
        item.getName(),
        item.getDescription(),
        item.getPrice(),
        item.getCategory(),
        item.getCreatedAt()
);
```

## Client

- A Spring `@Component` using `RestClient` to call a dependent service.
- Naming: `XxxClient` (e.g. `InventoryClient` for calls to a dependent inventory service).
- The `RestClient` bean itself is built in a `@Configuration` class, not inside the client.
- Every remote service gets exactly one dedicated client class, backed by exactly one `RestClient` bean. Never share one `RestClient` or one client class across multiple remote services.

```java
@Configuration
public class RestClientConfig {

    @Bean
    public RestClient inventoryRestClient(RestClient.Builder builder, RemoteServiceProperties properties) {
        return builder.baseUrl(properties.inventoryBaseUrl()).build();
    }

    @Bean
    public RestClient pricingRestClient(RestClient.Builder builder, RemoteServiceProperties properties) {
        return builder.baseUrl(properties.pricingBaseUrl()).build();
    }
}
```

```java
@Component
public class InventoryClient {

    private final RestClient restClient;

    public InventoryClient(RestClient inventoryRestClient) {
        this.restClient = inventoryRestClient;
    }

    public List<Long> getAvailableItemIds() {
        return restClient.get()
                .uri("/api/inventory/available")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }
}
```

```java
@Component
public class PricingClient {

    private final RestClient restClient;

    public PricingClient(RestClient pricingRestClient) {
        this.restClient = pricingRestClient;
    }

    public BigDecimal getPrice(Long itemId) {
        return restClient.get()
                .uri("/api/pricing/{itemId}", itemId)
                .retrieve()
                .body(BigDecimal.class);
    }
}
```
