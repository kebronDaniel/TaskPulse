# TaskPulse - Build Manifest (Everything to Create, by Week)

> Companion to `ARCHITECTURE.md`. This is the exhaustive checklist of every artifact - classes, interfaces, enums, records, configs, migrations, resources, and tests - needed to build TaskPulse, grouped into Week 1 -> Week 4 and the task (T1-T16) that produces it.
>
> Encoding: UTF-8 / ASCII-only, no BOM (opens cleanly in IntelliJ).
>
> Legend: [ ] = to do. `(W1)` next to an item means "first created in Week 1". Base Java package is `com.taskpulse`.

---

## Summary - Counts by Type

| Type | W1 | W2 | W3 | W4 | Total |
|---|---|---|---|---|---|
| Build / project files | 9 | 0 | 0 | 4 | 13 |
| Config classes (`config/`) | 1 | 1 | 0 | 2 | 4 |
| Entities (`domain/**`) | 9 | 0 | 1 | 1 | 11 |
| Enums | 4 | 0 | 0 | 1 | 5 |
| Controllers (`api/**`) | 0 | 3 | 1 | 1 | 5 |
| Services | 0 | 4 | 3 | 2 | 9 |
| Repositories | 0 | 4 | 2 | 2 | 8 |
| DTOs / records | 2 | 5 | 5 | 1 | 13 |
| Mappers | 0 | 2 | 1 | 0 | 3 |
| Security components | 0 | 5 | 0 | 1 | 6 |
| AOP | 0 | 3 | 0 | 0 | 3 |
| Exceptions / error model | 0 | 5 | 1 | 0 | 6 |
| Events (`event/`) | 0 | 0 | 0 | 3 | 3 |
| Infrastructure | 0 | 0 | 1 | 1 | 2 |
| Flyway migrations | 2 | 0 | 1 | 0 | 3 |
| Resource files | 4 | 0 | 0 | 2 | 6 |
| Test classes / resources | 0 | 0 | 8 | 3 | 11 |

> Counts are guidance, not law - split or merge classes as the code demands.

---

# WEEK 1 - Java Foundations (T1-T4)

Goal: project skeleton, domain model, schema, and Java 17 idioms. No Spring "magic" beyond bootstrap.

## T1 - Bootstrap the project

Build / project files
- [ ] `pom.xml` - Spring Boot 3.2+ parent, Java 17; dependencies: web, data-jpa, security, validation, actuator, flyway-core, spring-kafka, data-redis, lombok, mapstruct, springdoc-openapi-starter-webmvc-ui
- [ ] `docker-compose.yml` - services: `postgres` (postgres:15-alpine, healthcheck pg_isready), `redis` (redis:7-alpine), `zookeeper`, `kafka` (confluentinc/cp-kafka:7.5.0), `app` (depends_on condition: service_healthy)
- [ ] `.mvn/wrapper/maven-wrapper.properties` - so any machine can build without Maven installed
- [ ] `.env` - local-only env vars, `spring.profiles.active=dev` (git-ignored)
- [ ] `.gitignore` - target/, .env, .idea/, *.iml
- [ ] `.dockerignore` - target/, .git/, *.md, .env (added now, used in T16)

Source
- [ ] `com.taskpulse.TaskPulseApplication` - `@SpringBootApplication` main class

Resource files
- [ ] `src/main/resources/application.yml` - datasource, redis, kafka blocks using `${ENV_VAR:default}` placeholders
- [ ] `src/main/resources/application-dev.yml` - dev profile
- [ ] `src/main/resources/application-prod.yml` - prod profile (server.error.include-stacktrace=never)

Verify: `mvn spring-boot:run`, then `GET /actuator/health` returns `{status: UP}`.

## T2 - Domain entities

Source - base
- [ ] `domain/common/BaseEntity` - `@MappedSuperclass`, UUID id, createdAt, updatedAt, `@EntityListeners(AuditingEntityListener)`, id-only equals/hashCode
- [ ] `config/AuditConfig` - `@EnableJpaAuditing`, `AuditorAware<UUID>` bean reading SecurityContextHolder

Source - entities (all extend BaseEntity)
- [ ] `domain/user/User` - email (unique), passwordHash, role, passwordChangedAt, version; manual Builder
- [ ] `domain/workspace/Workspace` - name, owner (User)
- [ ] `domain/project/Project` - name, workspace
- [ ] `domain/task/Task` - title, status, priority, dueDate, assignee, project, commentCount, deletedAt, `@Version`
- [ ] `domain/comment/Comment` - body, task, author
- [ ] `domain/notification/Notification` - type, read, recipient, task
- [ ] `security/RefreshToken` - tokenHash, expiresAt, revoked, user
- [ ] `domain/audit/AuditLog` - entityType, entityId, action, actorId, oldValue (JSONB), newValue (JSONB)

Enums
- [ ] `domain/user/Role` - USER, ADMIN
- [ ] `domain/task/TaskStatus` - TODO, IN_PROGRESS, DONE
- [ ] `domain/task/TaskPriority` - LOW, MEDIUM, HIGH, CRITICAL
- [ ] `domain/notification/NotificationType` - TASK_CREATED, TASK_UPDATED, TASK_ASSIGNED, TASK_DELETED

Rules: `@Enumerated(STRING)`; never `@Data` on entities; relationships use `mappedBy`; cascade deliberately.

## T3 - Flyway migration scripts

DB migrations
- [ ] `resources/db/migration/V1__init_schema.sql` - users, refresh_tokens, workspaces, projects, tasks, comments, notifications
- [ ] `resources/db/migration/V2__add_indexes.sql` - idx_tasks_status, idx_tasks_assignee, idx_tasks_due_date, partial idx on deleted_at, partial unique idx on users(email) WHERE deleted_at IS NULL

(V3__audit_log.sql is created in Week 3 / T10.)

Config: set `spring.flyway.locations=classpath:db/migration`, `validate-on-migrate=true`.

## T4 - Java 17 & collections practice

DTOs / records
- [ ] `api/task/TaskResponse` - record (id, title, status, priority, assignee summary, dueDate, createdAt)
- [ ] `api/user/UserSummary` - record (id, email/name) used inside other responses

Source (stubs, fleshed out later)
- [ ] `domain/task/TaskService` (initial stubs) - Stream API grouping, EnumMap counts, Optional.orElseThrow, switch expressions, Comparator chaining

---

# WEEK 2 - Spring & Security (T5-T8)

Goal: full JWT auth, REST endpoints for workspace/project, global error handling, AOP.

## T5 - Spring Security + JWT auth

Dependencies
- [ ] add `io.jsonwebtoken:jjwt-api` + `jjwt-impl` + `jjwt-jackson` (0.12.x) to pom.xml

Security components (`security/`)
- [ ] `JwtService` - generateAccessToken (15m, HS256), generateRefreshToken (7d, store SHA-256 hash), validate, extractUserId
- [ ] `JwtAuthFilter` - extends OncePerRequestFilter; extract Bearer, validate, load user, set SecurityContext
- [ ] `UserDetailsServiceImpl` - implements UserDetailsService (loadUserByUsername)
- [ ] `RefreshTokenRepository` - `interface JpaRepository<RefreshToken, UUID>`; findByTokenHash, revoke-by-user
- [ ] `RefreshTokenService` - issue, rotate, revoke-all

Config
- [ ] `config/SecurityConfig` - SecurityFilterChain (csrf disabled, STATELESS, permit `/api/v1/auth/**`, authenticate rest, addFilterBefore JwtAuthFilter), `@EnableMethodSecurity`, PasswordEncoder (BCrypt) bean, AuthenticationManager bean

API (`api/auth/`)
- [ ] `AuthController` - POST /auth/register, /auth/login, /auth/refresh, /auth/logout
- [ ] `AuthService` - register, authenticate, refresh, logout
- [ ] `RegisterRequest` - record (email, password, ...)
- [ ] `LoginRequest` - record (email, password)
- [ ] `AuthResponse` - record (accessToken, refreshToken, expiresIn)

## T6 - Workspace & Project REST endpoints

API (`api/workspace/`, `api/project/`)
- [ ] `WorkspaceController` - POST/GET /api/v1/workspaces (201 on create, Page on list)
- [ ] `ProjectController` - CRUD under workspace
- [ ] `WorkspaceRequest`, `WorkspaceResponse` - records
- [ ] `ProjectRequest`, `ProjectResponse` - records

Services / repositories
- [ ] `domain/workspace/WorkspaceService`
- [ ] `domain/workspace/WorkspaceRepository`
- [ ] `domain/project/ProjectService`
- [ ] `domain/project/ProjectRepository`

Security / validation
- [ ] `security/WorkspaceSecurityService` - `@Component`, isMember(workspaceId, auth) for `@PreAuthorize("@workspaceSecurityService.isMember(...)")`
- [ ] `validation/UniqueWorkspaceName` - annotation + `UniqueWorkspaceNameValidator` (ConstraintValidator, injects repository)

Mappers
- [ ] `domain/workspace/WorkspaceMapper` - MapStruct `@Mapper(componentModel="spring")`
- [ ] `domain/project/ProjectMapper`

## T7 - Global exception handling & API error model

Exceptions (`exception/`)
- [ ] `GlobalExceptionHandler` - `@RestControllerAdvice`; maps 404/403/400/409/500 to RFC 7807 ProblemDetail
- [ ] `ApiError` - shared error shape / builder (if not relying solely on ProblemDetail)
- [ ] `ResourceNotFoundException` - 404
- [ ] `UnauthorizedException` - 403
- [ ] `BusinessRuleException` - 409 / 422

Cross-cutting
- [ ] `web/CorrelationIdFilter` - request-scoped filter; generate UUID, put in MDC, add `X-Correlation-Id` response header (shared with T15)

## T8 - Logging aspect & Spring AOP

AOP (`aop/`)
- [ ] `LoggingAspect` - `@Around` on `execution(* com.taskpulse.domain..*Service.*(..))`; log name, sanitized args, elapsed; `@AfterThrowing` for exceptions
- [ ] `AuditAspect` - `@After` on methods annotated `@Auditable`; write audit_log (uses AuditService in T10)
- [ ] `aop/Auditable` - custom annotation (action attribute)
- [ ] (helper) argument sanitizer - replace password/token/secret args with `***`

---

# WEEK 3 - Data Layer & Tests (T9-T12)

Goal: rich Task domain, transactions, Redis caching, and a full test suite.

## T9 - Task CRUD with advanced JPA queries

API (`api/task/`)
- [ ] `TaskController` - GET (paged + filtered), GET /{id}, POST, PATCH, DELETE
- [ ] `TaskRequest` - record (create payload)
- [ ] `TaskPatchRequest` - record/DTO for partial update (null = unchanged)
- [ ] `TaskFilterParams` - record (status, assignee, dueFrom, dueTo)

Domain (`domain/task/`)
- [ ] `TaskRepository` - `interface JpaRepository<Task,UUID>, JpaSpecificationExecutor<Task>`; `@EntityGraph` finder
- [ ] `TaskSpecifications` - `@Component`; hasStatus, assignedTo, dueBetween
- [ ] `TaskService` - finalize from W1 stubs: findById, findAll(spec, pageable), create, update, softDelete
- [ ] `TaskMapper` - MapStruct toResponse/toEntity

JPA features to wire in: `@EntityGraph` (N+1), `@Version` (optimistic), `@Where(deleted_at IS NULL)` (soft delete), Hibernate statistics in tests.

## T10 - Transactions deep-dive

DB migration
- [ ] `resources/db/migration/V3__audit_log.sql` - audit_log + processed_events tables

Domain (`domain/comment/`, `domain/audit/`)
- [ ] `CommentController` (`api/comment/`)
- [ ] `CommentService` - addCommentAndIncrementCount in one `@Transactional`
- [ ] `CommentRepository`
- [ ] `CommentRequest`, `CommentResponse` - records
- [ ] `domain/audit/AuditService` - logAction with `@Transactional(REQUIRES_NEW)`
- [ ] `domain/audit/AuditLogRepository`

Config / annotations
- [ ] enable retry: `@EnableRetry` (on a config class) + `@Retryable(ObjectOptimisticLockingFailureException, maxAttempts=3)` on update path
- [ ] apply `@Transactional` (writes) and `@Transactional(readOnly=true)` (reads) across services

## T11 - Redis caching layer

Config / infrastructure
- [ ] `config/RedisConfig` - RedisCacheManager, Jackson2JsonRedisSerializer (JavaTimeModule, FAIL_ON_UNKNOWN_PROPERTIES=false), per-cache TTL (tasks=10m, workspaces=5m)
- [ ] `infrastructure/cache/TaskCacheService` - `@Cacheable`/`@CachePut`/`@CacheEvict`; manual cache-aside for paged/filtered queries

Config: verify `/actuator/health` shows `redis: UP`.

## T12 - Full test suite

Test infra
- [ ] `pom.xml` additions - testcontainers (postgres, kafka), jacoco-maven-plugin (prepare-agent + report, enforce 80% in verify)
- [ ] `test/.../support/AbstractIntegrationTest` - base class with `@Testcontainers` + `@DynamicPropertySource`

Unit tests (`test/.../unit/`)
- [ ] `TaskServiceTest` - Mockito (`@Mock` repo, `@InjectMocks` service); edge cases
- [ ] `JwtServiceTest` - generation, expiry, tampered-token rejection

Slice / integration tests
- [ ] `TaskControllerIT` - `@WebMvcTest` + MockMvc; all status codes / body shapes (`@WithMockUser`)
- [ ] `AuthControllerIT` - `@WebMvcTest`
- [ ] `TaskRepositoryTest` - `@DataJpaTest`; Specifications, pagination, assert query count == 1 (no N+1)

E2E
- [ ] `e2e/TaskPulseE2ETest` - `@SpringBootTest(RANDOM_PORT)` + Testcontainers (real Postgres + Redis); register -> login -> CRUD -> assert 401 without token

Test resources
- [ ] `test/resources/sql/cleanup.sql` - `@Sql` cleanup between tests

---

# WEEK 4 - Async, Resilience & CI (T13-T16)

Goal: Kafka events, resilience patterns, observability, container + CI pipeline.

## T13 - Kafka event-driven notifications

Events (`event/`)
- [ ] `TaskEvent` - record (taskId, type, actorId, workspaceId, occurredAt)
- [ ] `TaskEventType` - enum (CREATED, UPDATED, DELETED) [or reuse NotificationType mapping]
- [ ] `TaskEventPublisher` - `@Component`; KafkaTemplate, publish key=workspaceId to `task-events`
- [ ] `TaskEventConsumer` - `@KafkaListener(topics="task-events", groupId="notification-service")`; idempotent; `@Transactional`
- [ ] `DltMonitorConsumer` - `@KafkaListener` on `task-events.DLT` for alerting

Config
- [ ] `config/KafkaConfig` - ProducerFactory (String + JsonSerializer), ConsumerFactory (TRUSTED_PACKAGES=com.taskpulse.event), DeadLetterPublishingRecoverer + error handler (3 retries -> DLT)

Domain / persistence (`domain/notification/`, `domain/event/`)
- [ ] `NotificationService` - create Notification for assignee from event
- [ ] `NotificationRepository`
- [ ] `api/notification/NotificationController` - list/mark-read for current user
- [ ] `domain/event/ProcessedEvent` (entity) - dedup row (event_id PK)
- [ ] `domain/event/ProcessedEventRepository`

(Wire `TaskEventPublisher` into `TaskService` create/update/delete.)

## T14 - Rate limiting + Resilience4j circuit breaker

Dependencies
- [ ] add `resilience4j-spring-boot3` to pom.xml

Security (`security/`)
- [ ] `RateLimitFilter` - extends OncePerRequestFilter; key by userId or X-Forwarded-For; returns 429 + Retry-After / X-RateLimit-* headers
- [ ] `resources/scripts/rate_limit.lua` - atomic INCR + EXPIRE Lua script

Infrastructure (`infrastructure/resilience/`)
- [ ] `ExternalServiceClient` - `@CircuitBreaker(name="emailService", fallbackMethod=...)` + `@Retry` + `@TimeLimiter` on sendEmail; fallback queues for async retry

Config
- [ ] application.yml - `resilience4j.circuitbreaker.instances.emailService` (slidingWindowSize=10, failureRateThreshold=50, waitDurationInOpenState=30s, permittedNumberOfCallsInHalfOpenState=3); retry + timelimiter instances

## T15 - Observability (metrics, tracing, structured logs)

Dependencies
- [ ] add `logstash-logback-encoder`, `micrometer-tracing-bridge-brave`, `zipkin-reporter-brave`, `micrometer-registry-prometheus`

Config / resources
- [ ] `config/ObservabilityConfig` - MeterRegistry customizations, custom counters (tasks.created, tasks.completed per workspace)
- [ ] `resources/logback-spring.xml` - JSON encoder for non-local profiles
- [ ] `web/MdcCorrelationFilter` - (the CorrelationIdFilter from T7) ensure MDC put/clear in finally
- [ ] `health/KafkaHealthIndicator` - implements HealthIndicator (Kafka connectivity)

Config: `management.endpoints.web.exposure.include=health,info,metrics,prometheus`; `management.tracing.sampling.probability` (1.0 dev / 0.1 prod); `@Timed` on key service methods.

## T16 - Docker, CI pipeline & README

Build / project files
- [ ] `Dockerfile` - multi-stage (builder: maven:3.9-eclipse-temurin-17, cache deps; runtime: eclipse-temurin:17-jre-alpine, -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0)
- [ ] `.dockerignore` - confirm (created in T1)
- [ ] `.github/workflows/ci.yml` - checkout@v4, setup-java@v4 (17 + maven cache), `mvn -B verify`, docker build, push to GHCR on main only, tag latest + git SHA (docker/metadata-action)
- [ ] `README.md` - architecture overview (link to docs/), prerequisites, quick start (docker-compose up), env-vars table, API endpoints table, design-decisions section, CI badge

CI policy: branch protection requiring the CI status check before merge to main.

---

## Cross-Week Dependency Notes

- Entities (W1/T2) are referenced everywhere later - build them first and correctly.
- `CorrelationIdFilter` is introduced in T7 (W2) and finalized for JSON logging in T15 (W4) - treat as one component, not two.
- `AuditAspect` (T8, W2) depends on `AuditService` + `audit_log` table (T10, W3) - the aspect can be stubbed in W2 and wired in W3.
- `TaskEventPublisher` (T13, W4) is injected into `TaskService` (W3) - add the dependency when you reach W4.
- Test dependencies (Testcontainers, JaCoCo) are added in W3/T12 but exercise code from all prior weeks.

---

Document maintained at `taskpulse/docs/BUILD-MANIFEST.md`. Keep in sync with `ARCHITECTURE.md`.
