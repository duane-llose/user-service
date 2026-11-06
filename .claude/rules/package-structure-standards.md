---
description: Package structure and layer responsibilities
paths:
  - "**/*.java"
---

# Package Structure & Layer Responsibilities

All backend source code must reside under the root package defined in `CLAUDE.md` using a consistent layer-based structure.

| Package | Responsibility |
|---|---|
| `config` | Spring and third-party configuration classes. |
| `controller` | REST API endpoints. Request handling and delegation only. No business logic. |
| `dto` | API request and response models. Immutable records. |
| `entity` | JPA persistence models mapped to database tables. |
| `repository` | Spring Data JPA interfaces annotated with `@Repository`. Must not be used directly from controllers. Also contains JPA Specification classes for dynamic queries. |
| `service` | Business logic and transactional boundaries. |
| `mapper` | Conversion between Entity and DTO. |
| `exceptions` | Custom domain and application exceptions. |
| `advice` | Global exception handling using `@RestControllerAdvice`. |
| `util` | Stateless helper and utility classes. |
| `client` | Spring components using `RestClient` to call dependent services. |
