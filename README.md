# TaskPulse

TaskPulse is a production-oriented task management REST API built with Java 17 and Spring Boot. It demonstrates transactional persistence, JWT security, caching, asynchronous event delivery, resilience, observability, containerization, and continuous integration.

## Technology stack

- Java 17 and Spring Boot 3
- Spring Security and JWT
- Spring Data JPA and PostgreSQL
- Redis caching and distributed rate limiting
- Apache Kafka and the transactional outbox pattern
- Resilience4j
- Micrometer, Prometheus, and Zipkin
- Flyway
- JUnit 5, Mockito, JaCoCo, and Testcontainers
- Docker Compose
- GitHub Actions and GHCR

## Architecture highlights

- Stateless JWT authentication and role-based authorization
- Optimistic locking and soft deletion
- Dynamic task search with JPA Specifications
- Redis-backed caching and atomic rate limiting
- At-least-once Kafka delivery through a transactional outbox
- Circuit-breaker-aware outbox processing
- Structured logging with correlation, trace, and event identifiers
- Prometheus metrics and distributed tracing
- Multi-stage, non-root Docker image
- CI-enforced tests and minimum 80% line coverage

## Run with Docker Compose

### Prerequisites

- Docker with Compose support
- Git
- OpenSSL

### Configuration

Create the local environment file:

```bash
cp .env.example .env
```

Generate a JWT signing key:

```bash
openssl rand -base64 64
```

Add the generated key and a local database password to `.env`.

### Start the stack

```bash
docker compose up -d --build
docker compose ps
```

Verify TaskPulse:

```bash
curl http://localhost:8080/actuator/health
```

Expected response:

```json
{"status":"UP"}
```

### Local services

| Service | Address |
|---|---|
| TaskPulse API | `http://localhost:8080` |
| PostgreSQL | `localhost:5433` |
| Redis | `localhost:6379` |
| Kafka | `localhost:9092` |
| Zipkin | `http://localhost:9411/zipkin/` |

Stop the stack:

```bash
docker compose down
```

To also delete local PostgreSQL and Redis data:

```bash
docker compose down --volumes
```

> Removing volumes permanently deletes locally persisted development data.

## Local development

Start only the infrastructure:

```bash
docker compose up -d postgres redis zookeeper kafka zipkins
```

Run TaskPulse from the project directory:

```bash
DB_URL=jdbc:postgresql://localhost:5433/taskPulse \
./mvnw spring-boot:run
```

## Tests and coverage

Run the complete verification lifecycle:

```bash
DB_URL=jdbc:postgresql://localhost:5433/taskPulse \
./mvnw clean verify
```

The test suite includes unit, controller, security, repository, integration, optimistic-locking, outbox, and resilience tests.

The JaCoCo report is generated at:

```text
target/site/jacoco/index.html
```

Maven verification fails when total line coverage falls below 80%.

## Environment variables

| Variable | Required | Description |
|---|---:|---|
| `DB_NAME` | Yes | PostgreSQL database name |
| `DB_USERNAME` | Yes | PostgreSQL application user |
| `DB_PASSWORD` | Yes | PostgreSQL password |
| `JWT_SECRET` | Yes | JWT signing key; use at least 32 random bytes |
| `JWT_EXPIRATION_MS` | No | Token lifetime; defaults to 900000 ms |
| `REDIS_HOST` | No | Redis hostname |
| `REDIS_PORT` | No | Redis port; defaults to 6379 |
| `KAFKA_BOOTSTRAP_SERVERS` | No | Kafka broker addresses |
| `ZIPKIN_ENDPOINT` | No | Zipkin span-ingestion endpoint |
| `TRACING_SAMPLING_PROBABILITY` | No | Trace sampling probability |
| `OUTBOX_BATCH_SIZE` | No | Events claimed per publishing batch |
| `OUTBOX_MAX_ATTEMPTS` | No | Failures allowed before an event becomes failed |

Never commit `.env` or production credentials.

## Continuous integration

GitHub Actions runs for pull requests and pushes to `main`.

The pipeline:

1. Starts ephemeral PostgreSQL and Redis services.
2. Configures Java 17 and Maven dependency caching.
3. Generates an ephemeral JWT test key.
4. Runs `./mvnw -B verify`.
5. Enforces at least 80% line coverage.
6. Builds the multi-stage Docker image.
7. Publishes to GHCR only from `main`.

Published images receive two tags:

```text
ghcr.io/<owner>/<repository>:<git-sha>
ghcr.io/<owner>/<repository>:latest
```

Deployments should prefer the immutable Git SHA tag or image digest. The mutable `latest` tag is provided for convenience.
