# CLAUDE.md

## Project Overview
`user-service` manages user accounts, authentication, and watchlist. Part of the Netflux distributed system.

## Root Package
`com.vinsguru.netflux.user`

## Security
- Issues JWT (RS256) on login, signed with a private key.
- Passwords hashed with BCrypt.

## Active Tech Stack
- **Backend:** Java 25, Spring Boot 4.x, Maven
- **Database:** PostgreSQL (via Testcontainers for local dev and testing)
- **Testing:** JUnit 5, Testcontainers

## Core Operational Commands

### Development & Build Lifecycle
- Build project:
```bash
./mvnw clean compile
```
- Package production artifact:
```bash
./mvnw clean package
```
- Run locally (via TestApplication + Testcontainers):
```bash
./mvnw spring-boot:test-run -Dspring-boot.run.profiles=test
```

### Testing Lifecycle
- Run all tests:
```bash
./mvnw test
```
- Run a single test class:
```bash
./mvnw test -Dtest=ClassName
```
