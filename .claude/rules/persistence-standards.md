---
description: Persistence standards for entities, repositories, dynamic queries, and PostgreSQL arrays
paths:
  - "**/*.java"
---

# Persistence Standards

## Entity

- Entities are persistence-only models, never exposed via API responses.

```java
@Entity
public class Item {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private BigDecimal price;
    private LocalDateTime createdAt;

    @Column(name = "is_active")
    private boolean active;

    @Enumerated(EnumType.STRING)
    private ItemStatus status;

    // getters and setters
}
```

## Repository

- Repository layer must never be exposed outside the service layer.
- Extend `JpaSpecificationExecutor` as well when the repository needs dynamic queries.

```java
@Repository
public interface ItemRepository extends JpaRepository<Item, Long>, JpaSpecificationExecutor<Item> {
}
```

## Dynamic Queries

- Use `JpaSpecificationExecutor` for dynamic filtering. Never build JPQL/native queries by string concatenation.
- Each filter condition is a static method in a dedicated `Specifications` class.
- A `null` parameter returns `cb.conjunction()` (no-op predicate). Never skip the predicate entirely.

### Specification

```java
public class ItemSpecifications {
    public static Specification<Item> hasName(String name) {
        return (root, query, cb) ->
                Objects.isNull(name) ? cb.conjunction() : cb.like(root.get("name"), "%" + name + "%");
    }

    public static Specification<Item> hasCategory(String category) {
        return (root, query, cb) ->
                Objects.isNull(category) ? cb.conjunction() : cb.equal(root.get("category"), category);
    }
}
```

### Combining Specifications

```java
var spec = Specification.<Item>where(ItemSpecifications.hasName(name))
                        .and(ItemSpecifications.hasCategory(category));
var results = itemRepository.findAll(spec);
```

## PostgreSQL Array Columns

- Map `text[]` columns to `List<String>` using `@JdbcTypeCode(SqlTypes.ARRAY)` and `@Column(columnDefinition = "text[]")`.
- To filter by array membership, use Hibernate's built-in `array_position` function in a `Specification`. Never create a custom SQL function for this.

### Mapping

```java
@JdbcTypeCode(SqlTypes.ARRAY)
@Column(columnDefinition = "text[]")
private List<String> tags;
```

### Filtering by Array Membership

```java
public static Specification<Item> hasTag(String tag) {
    return (root, query, cb) ->
            Objects.isNull(tag) ? cb.conjunction() :
            cb.greaterThan(cb.function("array_position", Integer.class, root.get("tags"), cb.literal(tag)), 0);
}
```
