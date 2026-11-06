---
description: Java Coding Standards
paths:
  - "**/*.java"
---

# Java Coding Standards

## Null Handling
- **Never return `null`.**
- Use `Optional<T>` for method return types where a value may be absent. Do not use `Optional` as method parameters or fields.
- Return empty immutable collections instead of `null`.

Example:
```java
return Optional.empty();
return Collections.emptyList(); // Or List.of()
return Collections.emptySet();  // Or Set.of()
return Collections.emptyMap();  // Or Map.of()
```

- Use `Objects.isNull()` and `Objects.nonNull()` for null checks — `!= null` and `== null` are forbidden.

Example:
```java
// Wrong
object != null
object == null

// Correct
Objects.nonNull(object)
Objects.isNull(object)
```

## Local Variables & Types

* Use `var` for all local variable declarations where the concrete type is obvious from the initialization.
* When using `var` with generic factory methods, always provide an explicit type parameter — without it, the compiler infers `Object`.

Example:
```java
// Wrong — infers List<Object>
var list = Collections.emptyList();

// Correct — explicit type parameter required with var
var list = Collections.<String>emptyList();
```

* Program to interfaces rather than concrete implementations for class members, return types, and method parameters — not local variables (use `var` for those).

Example:
```java
// Wrong
private ArrayList<String> names;
public ArrayList<String> getNames() { ... }

// Correct
private List<String> names;
public List<String> getNames() { ... }
```

## Records & Data Carriers

* Prefer `record` types for all immutable data carrier objects. Do not use standard classes with boilerplate getters for read-only data.
* Records with 2 or more fields must be declared with each field on its own line, aligned to the opening parenthesis.

Example:
```java
// Wrong
record ItemSummary(Long id, String name, Double price, String category) {}

// Correct
record ItemSummary(Long id,
                   String name,
                   Double price,
                   String category) {
}
```

## Streams & Functional Code

* Prefer the Stream API over imperative loops (`for`/`while`) for collection processing.
* Always write stream chains in multiline format — one operation per line. Never chain stream operations on a single line.
* Keep streams readable; extract complex lambda bodies into private helper methods or method references.

Example:
```java
// Wrong
var results = items.stream().filter(Item::isActive).toList();

// Correct
var results = items.stream()
        .filter(Item::isActive)
        .toList();
```

## Modern Java Features

* Prefer modern Java language features over legacy alternatives.
* Use pattern matching for `instanceof` to eliminate verbose casting.

Example:
```java
// Wrong
if (shape instanceof Circle) {
    Circle c = (Circle) shape;
    return c.radius();
}

// Correct
if (shape instanceof Circle c) {
    return c.radius();
}
```

* Prefer arrow syntax (`->`) switch expressions over traditional colon (`:`) statement switches.

Example:
```java
return switch (status) {
    case ENABLED -> "Enabled";
    case DISABLED -> "Disabled";
};
```
