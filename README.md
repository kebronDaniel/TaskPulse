# TaskPulse

TaskPulse is a production-oriented task management REST API built with Java 17 and Spring Boot. It demonstrates backend patterns commonly expected in mid-level Java interviews, including transactional persistence, JWT security, caching, asynchronous event delivery, resilience, observability, containerization, and continuous integration.

## Technology stack

- Java 17
- Spring Boot 3
- Spring Security and JWT
- Spring Data JPA and PostgreSQL
- Redis caching and distributed rate limiting
- Apache Kafka
- Transactional outbox pattern
- Resilience4j
- Micrometer, Prometheus, and Zipkin
- Flyway
- Testcontainers, JUnit 5, and Mockito
- Docker Compose
- GitHub Actions and GHCR

## Architecture highlights

- Stateless JWT authentication and role-based authorization
- Optimistic locking and soft deletion
- Dynamic task search with JPA Specifications
- Redis-backed caching and atomic rate limiting
- At-least-once Kafka delivery through a transactional outbox
- Circuit-breaker-aware outbox processing
- Structured JSON logging with correlation and trace identifiers
- Prometheus metrics and distributed tracing
- Multi-stage, non-root Docker image
- CI-enforced tests and minimum 80% line coverage
