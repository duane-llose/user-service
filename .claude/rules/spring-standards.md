---
description: Spring standards for dependency injection, logging, and configuration
paths:
  - "**/*.java"
---

# Spring Standards

## Dependency Injection

- Use constructor injection exclusively. Applies to all Spring components (controllers, services, repositories, etc.).
- Field injection (`@Autowired` on fields) is forbidden.
- Dependencies must be declared as `private final` fields.

```java
// Wrong
@Autowired
private ItemRepository repository;

// Correct
private final ItemRepository repository;

public ItemService(ItemRepository repository) {
    this.repository = repository;
}
```

## Configuration Management

- All infrastructure and framework configuration must reside in the `config` package.
- Prefer `@ConfigurationProperties` for type-safe configuration binding. Avoid scattered `@Value` annotations across the codebase.

```java
@ConfigurationProperties(prefix = "app.mail")
public record MailProperties(String host,
                             int port) {
}
```

- Register configuration property classes once via `@ConfigurationPropertiesScan` on the main application class.

```java
@SpringBootApplication
@ConfigurationPropertiesScan
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

## Logging

- Never use `System.out.println` or `System.err.println`.
- Use SLF4J with Logback for all logging.

```java
private static final Logger log = LoggerFactory.getLogger(ItemService.class);

log.info("Fetching item {}", id);
log.warn("Item {} not found, returning empty", id);
log.error("Failed to process item {}", id, ex);
```
