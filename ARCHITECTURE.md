# TaskPulse - System Architecture & Design

> Stack: Java 17 | Spring Boot 3.2 | PostgreSQL 15 | Redis 7 | Apache Kafka | Resilience4j | Docker | GitHub Actions
>
> Purpose of this document: a single, build-ready reference that describes WHAT TaskPulse is, HOW its pieces fit together, and IN WHAT ORDER to build it. Every diagram maps to concrete classes and files so you can go from design to code without guessing.
>
> Encoding: UTF-8, ASCII-only content (safe to open in IntelliJ with any encoding setting). No BOM.

---

## Table of Contents

1. [How to Read This Document](#1-how-to-read-this-document)
2. [System Context (C4 Level 1)](#2-system-context-c4-level-1)
3. [Container View (C4 Level 2)](#3-container-view-c4-level-2)
4. [UML Class Diagram - Domain Model](#4-uml-class-diagram---domain-model)
5. [UML Class Diagram - Layered Application](#5-uml-class-diagram---layered-application)
6. [Entity Relationship Diagram (ERD)](#6-entity-relationship-diagram-erd)
7. [Sequence Diagrams](#7-sequence-diagrams)
8. [Event-Driven Architecture (Kafka)](#8-event-driven-architecture-kafka)
9. [Resilience & Rate Limiting](#9-resilience--rate-limiting)
10. [Transaction & Data-Access Strategy](#10-transaction--data-access-strategy)
11. [Observability Stack](#11-observability-stack)
12. [Deployment & CI/CD](#12-deployment--cicd)
13. [Project Package Structure](#13-project-package-structure)
14. [Build Roadmap (T1-T16)](#14-build-roadmap-t1-t16)
15. [Key Technology Decisions](#15-key-technology-decisions)

---

## 1. How to Read This Document

- Diagram types used: Mermaid `graph`, `classDiagram`, `erDiagram`, `sequenceDiagram`, and `stateDiagram-v2`. They render natively on GitHub/GitLab, in VS Code (Markdown Preview Mermaid Support), IntelliJ (Markdown plugin has Mermaid built in - enable it in Settings > Languages & Frameworks > Markdown), and Obsidian.
- UML vs ERD: Sections 4 & 5 are UML class diagrams (object/code structure - fields, methods, visibility, inheritance, dependencies). Section 6 is the ERD (physical database tables - columns, types, keys, cardinality).
- Visibility markers in UML: `+` public, `-` private, `#` protected, `~` package-private. `<<abstract>>`, `<<Entity>>`, `<<interface>>`, `<<enumeration>>` are stereotypes (Mermaid renders these inside guillemets automatically).
- Building order: follow the Build Roadmap (Section 14) - each task (T1-T16) names the exact files to create and which diagram to consult.

---

## 2. System Context (C4 Level 1)

The big picture: who uses TaskPulse and which external systems it talks to.

```mermaid
graph TB
    USER["End User<br/>(web / mobile client)"]
    ADMIN["Admin User<br/>(elevated role)"]

    subgraph boundary["TaskPulse Platform"]
        APP["TaskPulse API<br/>(Spring Boot monolith,<br/>modular packages)"]
    end

    EMAIL["External Email<br/>Notification Service"]
    METRICS["Prometheus / Grafana<br/>(monitoring)"]
    TRACE["Zipkin<br/>(tracing backend)"]

    USER -->|"REST/JSON over HTTPS<br/>JWT Bearer auth"| APP
    ADMIN -->|"admin endpoints<br/>(@PreAuthorize ADMIN)"| APP
    APP -->|"send notification<br/>(circuit-broken call)"| EMAIL
    APP -->|"scrape /actuator/prometheus"| METRICS
    APP -->|"export spans"| TRACE
```

---

## 3. Container View (C4 Level 2)

The runtime processes (containers) and how they communicate. Mirrors `docker-compose.yml`.

```mermaid
graph TB
    CLIENT["Client"]

    subgraph compose["docker-compose network"]
        direction TB
        APP["app<br/>Spring Boot :8080<br/>eclipse-temurin:17-jre-alpine"]
        PG[("postgres :5432<br/>postgres:15-alpine<br/>healthcheck: pg_isready")]
        RD[("redis :6379<br/>redis:7-alpine")]
        ZK["zookeeper :2181"]
        KF[("kafka :9092<br/>confluentinc/cp-kafka:7.5.0")]
        PROM["prometheus :9090"]
        GRAF["grafana :3000"]
    end

    CLIENT -->|HTTPS| APP
    APP -->|JDBC| PG
    APP -->|RESP / Lettuce| RD
    APP -->|Kafka protocol| KF
    KF --> ZK
    PROM -->|scrape| APP
    GRAF -->|query| PROM

    APP -. "depends_on: condition service_healthy" .-> PG
```

> Note: the `app` service uses `depends_on` with `condition: service_healthy` on Postgres so it never boots before the database is accepting connections.

---

## 4. UML Class Diagram - Domain Model

Object-oriented structure of the entities, their inheritance from a shared `BaseEntity`, and enums. This is the blueprint for the `domain/**` packages (tasks T2, T4).

```mermaid
classDiagram
    direction TB

    class BaseEntity {
        <<abstract>>
        #UUID id
        #Instant createdAt
        #Instant updatedAt
        +equals(Object) boolean
        +hashCode() int
    }

    class User {
        <<Entity>>
        -String email
        -String passwordHash
        -Role role
        -Instant passwordChangedAt
        -Long version
        +getAuthorities() Collection
    }

    class Workspace {
        <<Entity>>
        -String name
        -User owner
        +addProject(Project) void
    }

    class Project {
        <<Entity>>
        -String name
        -Workspace workspace
    }

    class Task {
        <<Entity>>
        -String title
        -TaskStatus status
        -TaskPriority priority
        -LocalDate dueDate
        -User assignee
        -Project project
        -int commentCount
        -Instant deletedAt
        -Long version
        +isOverdue() boolean
        +incrementCommentCount() void
    }

    class Comment {
        <<Entity>>
        -String body
        -Task task
        -User author
    }

    class Notification {
        <<Entity>>
        -NotificationType type
        -boolean read
        -User recipient
        -Task task
    }

    class RefreshToken {
        <<Entity>>
        -String tokenHash
        -Instant expiresAt
        -boolean revoked
        -User user
    }

    class AuditLog {
        <<Entity>>
        -String entityType
        -UUID entityId
        -String action
        -UUID actorId
        -JsonNode oldValue
        -JsonNode newValue
    }

    class Role {
        <<enumeration>>
        USER
        ADMIN
    }
    class TaskStatus {
        <<enumeration>>
        TODO
        IN_PROGRESS
        DONE
    }
    class TaskPriority {
        <<enumeration>>
        LOW
        MEDIUM
        HIGH
        CRITICAL
    }
    class NotificationType {
        <<enumeration>>
        TASK_CREATED
        TASK_UPDATED
        TASK_ASSIGNED
        TASK_DELETED
    }

    BaseEntity <|-- User
    BaseEntity <|-- Workspace
    BaseEntity <|-- Project
    BaseEntity <|-- Task
    BaseEntity <|-- Comment
    BaseEntity <|-- Notification
    BaseEntity <|-- RefreshToken
    BaseEntity <|-- AuditLog

    User "1" --> "*" Workspace : owns
    Workspace "1" --> "*" Project : contains
    Project "1" --> "*" Task : contains
    User "1" --> "*" Task : assigned
    Task "1" --> "*" Comment : has
    User "1" --> "*" Comment : authors
    User "1" --> "*" Notification : receives
    Task "1" --> "*" Notification : triggers
    User "1" --> "*" RefreshToken : holds

    User ..> Role
    Task ..> TaskStatus
    Task ..> TaskPriority
    Notification ..> NotificationType
```

Design rules captured here (from T2):
- `BaseEntity` is `@MappedSuperclass` carrying `id` (UUID), `createdAt`, `updatedAt` with `@CreatedDate`/`@LastModifiedDate` auditing.
- `equals()`/`hashCode()` are based ONLY on `id` (never all fields) to stay Hibernate-proxy-safe.
- Enums persisted with `@Enumerated(EnumType.STRING)` - never `ORDINAL`.
- `Task` carries `@Version` for optimistic locking and `deletedAt` for soft delete.
- No Lombok `@Data` on entities (avoids `toString()` triggering lazy loading).

---

## 5. UML Class Diagram - Layered Application

How a request flows through Controller -> Service -> Repository, plus the security, AOP, caching, and event collaborators. This is the blueprint for `api/**`, `domain/**`, `security/**`, `aop/**`, `infrastructure/**`.

```mermaid
classDiagram
    direction LR

    class TaskController {
        <<RestController>>
        -TaskService taskService
        +create(TaskRequest) ResponseEntity~TaskResponse~
        +list(TaskFilterParams, Pageable) Page~TaskResponse~
        +getById(UUID) TaskResponse
        +patch(UUID, TaskPatchRequest) TaskResponse
        +delete(UUID) ResponseEntity~Void~
    }

    class TaskService {
        <<Service>>
        -TaskRepository repo
        -TaskMapper mapper
        -TaskCacheService cache
        -TaskEventPublisher publisher
        +findById(UUID) TaskResponse
        +findAll(TaskFilterParams, Pageable) Page~TaskResponse~
        +create(TaskRequest) TaskResponse
        +update(UUID, TaskPatchRequest) TaskResponse
        +softDelete(UUID) void
    }

    class TaskRepository {
        <<interface>>
        +findById(UUID) Optional~Task~
        +findAll(Specification, Pageable) Page~Task~
    }

    class TaskSpecifications {
        <<Component>>
        +hasStatus(TaskStatus) Specification~Task~
        +assignedTo(UUID) Specification~Task~
        +dueBetween(LocalDate, LocalDate) Specification~Task~
    }

    class TaskMapper {
        <<Mapper>>
        +toResponse(Task) TaskResponse
        +toEntity(TaskRequest) Task
    }

    class TaskCacheService {
        <<Service>>
        +get(UUID) TaskResponse
        +evict(UUID) void
    }

    class TaskEventPublisher {
        <<Component>>
        -KafkaTemplate template
        +publish(TaskEvent) void
    }

    class JwtAuthFilter {
        <<Component>>
        -JwtService jwtService
        +doFilterInternal(req, res, chain) void
    }

    class JwtService {
        <<Service>>
        +generateAccessToken(UserDetails) String
        +generateRefreshToken(UUID) String
        +validate(String) boolean
        +extractUserId(String) UUID
    }

    class LoggingAspect {
        <<Aspect>>
        +logExecution(ProceedingJoinPoint) Object
        +logException(JoinPoint, Throwable) void
    }

    class GlobalExceptionHandler {
        <<RestControllerAdvice>>
        +handleNotFound() ProblemDetail
        +handleValidation() ProblemDetail
        +handleOptimisticLock() ProblemDetail
        +handleGeneric() ProblemDetail
    }

    TaskController --> TaskService : delegates
    TaskService --> TaskRepository : queries
    TaskService --> TaskMapper : maps
    TaskService --> TaskCacheService : reads/evicts
    TaskService --> TaskEventPublisher : emits events
    TaskRepository ..> TaskSpecifications : uses
    JwtAuthFilter --> JwtService : validates
    JwtAuthFilter ..> TaskController : authenticates before
    LoggingAspect ..> TaskService : Around advice
    GlobalExceptionHandler ..> TaskController : advises
```

> The same Controller -> Service -> Repository pattern repeats for Auth, Workspace, Project, Comment, and Notification. `TaskService` shows the most collaborators (cache + events), so it is the canonical template. Annotations applied in code: `TaskController` is `@RestController @RequestMapping("/api/v1/tasks")`; `TaskMapper` is `@Mapper(componentModel="spring")`.

---

## 6. Entity Relationship Diagram (ERD)

Physical database schema produced by the Flyway migrations (tasks T3, T10). Types are PostgreSQL types.

```mermaid
erDiagram
    USERS {
        uuid id PK "gen_random_uuid()"
        varchar email UK "NOT NULL"
        varchar password_hash "NOT NULL"
        varchar role "NOT NULL"
        timestamptz password_changed_at "nullable"
        timestamptz created_at "NOT NULL"
        timestamptz updated_at "NOT NULL"
        bigint version "DEFAULT 0"
    }

    REFRESH_TOKENS {
        uuid id PK
        uuid user_id FK "REFERENCES users(id)"
        varchar token_hash UK "SHA-256, NOT NULL"
        timestamptz expires_at "NOT NULL"
        boolean revoked "DEFAULT false"
    }

    WORKSPACES {
        uuid id PK
        uuid owner_id FK "REFERENCES users(id)"
        varchar name "NOT NULL"
        timestamptz created_at "NOT NULL"
        timestamptz updated_at "NOT NULL"
    }

    PROJECTS {
        uuid id PK
        uuid workspace_id FK "REFERENCES workspaces(id)"
        varchar name "NOT NULL"
        timestamptz created_at "NOT NULL"
        timestamptz updated_at "NOT NULL"
    }

    TASKS {
        uuid id PK
        uuid project_id FK "REFERENCES projects(id)"
        uuid assignee_id FK "REFERENCES users(id), nullable"
        varchar title "NOT NULL"
        varchar status "NOT NULL"
        varchar priority "NOT NULL"
        date due_date "nullable"
        integer comment_count "DEFAULT 0"
        timestamptz deleted_at "soft delete, NULL = active"
        bigint version "DEFAULT 0"
        timestamptz created_at "NOT NULL"
        timestamptz updated_at "NOT NULL"
    }

    COMMENTS {
        uuid id PK
        uuid task_id FK "REFERENCES tasks(id)"
        uuid author_id FK "REFERENCES users(id)"
        text body "NOT NULL"
        timestamptz created_at "NOT NULL"
    }

    NOTIFICATIONS {
        uuid id PK
        uuid recipient_id FK "REFERENCES users(id)"
        uuid task_id FK "REFERENCES tasks(id)"
        varchar type "NOT NULL"
        boolean read "DEFAULT false"
        timestamptz created_at "NOT NULL"
    }

    AUDIT_LOG {
        uuid id PK
        varchar entity_type "NOT NULL"
        uuid entity_id "NOT NULL"
        varchar action "NOT NULL"
        uuid actor_id "nullable"
        jsonb old_value "nullable"
        jsonb new_value "nullable"
        timestamptz created_at "NOT NULL"
    }

    PROCESSED_EVENTS {
        uuid event_id PK "Kafka dedup key"
        timestamptz processed_at "NOT NULL"
    }

    USERS ||--o{ REFRESH_TOKENS : "issues"
    USERS ||--o{ WORKSPACES : "owns"
    WORKSPACES ||--o{ PROJECTS : "contains"
    PROJECTS ||--o{ TASKS : "contains"
    USERS |o--o{ TASKS : "assigned (nullable)"
    TASKS ||--o{ COMMENTS : "has"
    USERS ||--o{ COMMENTS : "authors"
    USERS ||--o{ NOTIFICATIONS : "receives"
    TASKS ||--o{ NOTIFICATIONS : "triggers"
```

Indexing & constraint strategy (V2 migration):

| Index / Constraint | Definition | Why |
|---|---|---|
| idx_tasks_status | tasks(status) | Filter tasks by status (board columns) |
| idx_tasks_assignee | tasks(assignee_id) | "My tasks" queries |
| idx_tasks_due_date | tasks(due_date) | Overdue / upcoming queries |
| idx_tasks_active | tasks(deleted_at) WHERE deleted_at IS NULL | Partial index - smaller & faster for the common soft-delete filter |
| uq_users_email_active | UNIQUE(email) WHERE deleted_at IS NULL | Enforce email uniqueness only among active users (allows re-registration after soft delete) |
| uq_processed_events | UNIQUE(event_id) | Kafka consumer idempotency / dedup |

---

## 7. Sequence Diagrams

### 7.1 Authentication & Token Rotation (T5)

```mermaid
sequenceDiagram
    autonumber
    participant C as Client
    participant RLF as RateLimitFilter
    participant JAF as JwtAuthFilter
    participant AC as AuthController
    participant AS as AuthService
    participant DB as PostgreSQL
    participant RD as Redis

    C->>RLF: POST /auth/login {email, password}
    RLF->>RD: INCR rate_limit:{ip} (Lua atomic)
    RD-->>RLF: count <= 100, allow
    RLF->>JAF: forward (public route, no token required)
    JAF->>AC: forward
    AC->>AS: authenticate(email, password)
    AS->>DB: SELECT * FROM users WHERE email = ?
    DB-->>AS: user row
    AS->>AS: BCrypt.matches(raw, passwordHash)
    AS->>AS: accessToken = HS256, sub=userId, exp=15m
    AS->>AS: refreshToken raw -> SHA-256 hash
    AS->>DB: INSERT refresh_tokens (token_hash, expires_at=7d)
    AS-->>C: 200 {accessToken, refreshToken}

    Note over C,RD: later - protected call
    C->>RLF: GET /api/v1/tasks (Authorization Bearer access)
    RLF->>RD: INCR rate_limit:{userId}
    RLF->>JAF: forward
    JAF->>JAF: validate signature + expiry
    JAF->>DB: load UserDetails
    JAF->>JAF: set SecurityContextHolder
    JAF-->>C: request proceeds to controller

    Note over C,RD: token refresh (rotation)
    C->>AC: POST /auth/refresh {refreshToken}
    AC->>AS: refresh(token)
    AS->>DB: SELECT WHERE token_hash=? AND NOT revoked AND expires_at>now
    AS->>DB: UPDATE old token SET revoked=true
    AS->>DB: INSERT new refresh token
    AS-->>C: 200 {newAccess, newRefresh}
```

### 7.2 Task Update - Optimistic Lock + Cache + Event (T9, T11, T13)

```mermaid
sequenceDiagram
    autonumber
    participant C as Client
    participant TC as TaskController
    participant LA as LoggingAspect
    participant TS as TaskService
    participant TR as TaskRepository
    participant PG as PostgreSQL
    participant RD as Redis
    participant EP as TaskEventPublisher
    participant KF as Kafka

    C->>TC: PATCH /api/v1/tasks/{id} {title, status}
    TC->>LA: Around intercept (start timer)
    LA->>TS: update(id, patch)
    activate TS
    TS->>TR: findById(id) @EntityGraph(assignee, project)
    TR->>PG: SELECT ... (single JOIN, no N+1)
    PG-->>TS: Task (version = N)
    TS->>TS: apply non-null fields (partial update)
    TS->>TR: save(task)
    TR->>PG: UPDATE ... WHERE id=? AND version=N
    alt version matches
        PG-->>TS: 1 row updated (version -> N+1)
    else stale version
        PG-->>TS: 0 rows -> OptimisticLockException
        TS-->>TC: retry up to 3x or 409 Conflict
    end
    TS->>RD: CacheEvict tasks::id
    TS->>EP: publish(TaskEvent UPDATED)
    EP->>KF: send(key=workspaceId, topic=task-events)
    deactivate TS
    TS-->>LA: TaskResponse
    LA-->>TC: TaskResponse (log elapsed ms)
    TC-->>C: 200 OK
```

---

## 8. Event-Driven Architecture (Kafka)

Asynchronous notification pipeline (task T13). Events are small records keyed by `workspaceId` so ordering is preserved per workspace.

```mermaid
graph LR
    subgraph prod["Producer"]
        TS["TaskService<br/>(on create/update/delete)"]
        EP["TaskEventPublisher<br/>KafkaTemplate"]
    end

    subgraph kafka["Apache Kafka"]
        T1["topic: task-events<br/>key = workspaceId"]
        DLT["topic: task-events.DLT"]
    end

    subgraph cons["Consumers"]
        EC["TaskEventConsumer<br/>@KafkaListener<br/>groupId=notification-service"]
        DLM["DLT Monitor<br/>@KafkaListener<br/>groupId=dlq-monitor"]
    end

    subgraph store["Persistence"]
        PE[("processed_events<br/>(idempotency)")]
        NT[("notifications")]
    end

    TS --> EP --> T1
    T1 -->|"at-least-once"| EC
    EC -->|"INSERT event_id<br/>(skip if duplicate)"| PE
    EC -->|"persist for assignee"| NT
    EC -->|"after 3 failed retries<br/>DeadLetterPublishingRecoverer"| DLT
    DLT --> DLM
```

Guarantees: at-least-once delivery + idempotent consumer (dedup table) = effectively-once processing. The `@Transactional` consumer only commits the Kafka offset after the DB write succeeds.

---

## 9. Resilience & Rate Limiting

Outbound calls (for example email) are wrapped in a layered Resilience4j stack; inbound traffic is throttled per user (task T14).

```mermaid
graph TB
    subgraph inbound["Inbound - Rate Limiting"]
        REQ["Incoming request"]
        RLF["RateLimitFilter<br/>(OncePerRequestFilter)"]
        LUA["Redis Lua (atomic):<br/>count = INCR key<br/>if count==1 then EXPIRE 60<br/>if count>100 then 429 Retry-After"]
        REQ --> RLF --> LUA
    end

    subgraph outbound["Outbound - Resilience4j (decorator order)"]
        direction TB
        CB["@CircuitBreaker<br/>window=10, threshold=50pct, open=30s"]
        RT["@Retry<br/>maxAttempts=3, exp backoff + jitter"]
        TL["@TimeLimiter<br/>2s timeout"]
        CALL["ExternalServiceClient.sendEmail()"]
        FB["Fallback:<br/>queue for async retry"]
        CB --> RT --> TL --> CALL
        CB -. "OPEN: fail fast" .-> FB
    end
```

Circuit-breaker state machine:

```mermaid
stateDiagram-v2
    [*] --> CLOSED
    CLOSED --> OPEN : failureRate >= 50pct
    OPEN --> HALF_OPEN : after waitDuration (30s)
    HALF_OPEN --> CLOSED : test calls succeed
    HALF_OPEN --> OPEN : test calls fail
```

---

## 10. Transaction & Data-Access Strategy

```mermaid
graph TB
    subgraph tx["@Transactional boundaries"]
        W["Write methods<br/>@Transactional (REQUIRED)"]
        R["Read methods<br/>@Transactional(readOnly=true)<br/>FlushMode=MANUAL"]
        N["AuditService.log()<br/>@Transactional(REQUIRES_NEW)<br/>commits even if caller rolls back"]
    end

    subgraph hib["Hibernate techniques"]
        OL["Optimistic locking<br/>@Version + @Retryable(3)"]
        EG["@EntityGraph / JOIN FETCH<br/>(N+1 prevention)"]
        SD["Soft delete<br/>@Where(deleted_at IS NULL)"]
        SP["Dynamic filters<br/>JpaSpecificationExecutor"]
    end

    W --> OL
    W --> EG
    W --> SD
    R --> SP
```

> Self-invocation caveat: `@Transactional`/AOP advice only applies through the Spring proxy. An internal `this.method()` call bypasses it - split such calls into separate beans or inject a self-reference.

---

## 11. Observability Stack

```mermaid
graph LR
    subgraph app["TaskPulse App"]
        MDC["MDC filter<br/>correlationId / traceId"]
        LOG["Logback JSON<br/>(logstash-logback-encoder)"]
        MIC["Micrometer<br/>counters + @Timed histograms"]
        ACT["Actuator<br/>/health /metrics /prometheus<br/>+ custom Kafka health"]
        BR["Micrometer Tracing (Brave)"]
    end

    subgraph backends["Backends"]
        PR[("Prometheus")]
        GR["Grafana<br/>(RED dashboards)"]
        ZK["Zipkin"]
        ELK["Log aggregator<br/>(ELK / Datadog)"]
    end

    MDC --> LOG --> ELK
    MIC --> ACT --> PR --> GR
    BR --> ZK
```

---

## 12. Deployment & CI/CD

```mermaid
graph LR
    DEV["Developer<br/>git push"] --> PR["Pull Request to main"]

    subgraph gha["GitHub Actions (ci.yml)"]
        direction TB
        CO["checkout@v4"]
        JV["setup-java@v4<br/>Java 17 + Maven cache"]
        VR["mvn -B verify<br/>(unit + IT + e2e + JaCoCo >= 80pct)"]
        BLD["docker build (multi-stage)"]
        PUSH["push to GHCR<br/>(main only, tag = latest + git-SHA)"]
        CO --> JV --> VR --> BLD --> PUSH
    end

    subgraph img["Multi-stage Dockerfile"]
        direction TB
        ST1["Stage 1 builder<br/>maven:3.9-eclipse-temurin-17<br/>cache deps then mvn package"]
        ST2["Stage 2 runtime<br/>eclipse-temurin:17-jre-alpine<br/>-XX:+UseContainerSupport<br/>-XX:MaxRAMPercentage=75.0"]
        ST1 --> ST2
    end

    PR --> CO
    BLD -.-> ST1
```

---

## 13. Project Package Structure

```
taskpulse/
  docs/
    ARCHITECTURE.md                 <- this document
  docker-compose.yml                <- postgres, redis, zookeeper, kafka, app
  Dockerfile                        <- multi-stage build
  pom.xml
  .github/workflows/ci.yml
  src/main/java/com/taskpulse/
    TaskPulseApplication.java
    api/
      auth/          AuthController, AuthRequest, AuthResponse
      workspace/     WorkspaceController, WorkspaceRequest/Response
      project/       ProjectController
      task/          TaskController, TaskRequest, TaskResponse, TaskFilterParams
      notification/  NotificationController
    domain/
      common/        BaseEntity
      user/          User, Role, UserRepository, UserService
      workspace/     Workspace, WorkspaceRepository, WorkspaceService
      project/       Project, ProjectRepository, ProjectService
      task/          Task, TaskStatus, TaskPriority, TaskRepository,
                     TaskSpecifications, TaskService, TaskMapper
      comment/       Comment, CommentRepository, CommentService
      notification/  Notification, NotificationType, NotificationService
    security/        JwtService, JwtAuthFilter, RateLimitFilter,
                     WorkspaceSecurityService, RefreshToken, RefreshTokenRepository
    config/          SecurityConfig, RedisConfig, KafkaConfig,
                     ObservabilityConfig, AuditConfig
    aop/             LoggingAspect, AuditAspect, @Auditable
    event/           TaskEvent, TaskEventPublisher, TaskEventConsumer
    infrastructure/
      cache/         TaskCacheService
      resilience/    ExternalServiceClient
    exception/       GlobalExceptionHandler, ApiError,
                     ResourceNotFoundException, BusinessRuleException
  src/main/resources/
    db/migration/    V1__init_schema.sql, V2__add_indexes.sql, V3__audit_log.sql
    application.yml, application-dev.yml, application-prod.yml
    logback-spring.xml
```

---

## 14. Build Roadmap (T1-T16)

Build the project in this order. Each row says which diagram to consult and which files to produce.

| Week | Task | Build | Diagram refs |
|---|---|---|---|
| W1 | T1 Bootstrap | pom.xml, TaskPulseApplication, docker-compose.yml | Section 3 Container |
| W1 | T2 Domain entities | BaseEntity + all @Entity classes & enums | Section 4 Domain UML |
| W1 | T3 Flyway migrations | V1__init_schema.sql | Section 6 ERD |
| W1 | T4 Java 17 / collections | DTO records, Stream-based service stubs | Section 4 |
| W2 | T5 Security + JWT | JwtService, JwtAuthFilter, SecurityConfig, AuthController | Section 7.1 Auth seq |
| W2 | T6 Workspace/Project REST | WorkspaceController/Service, @PreAuthorize | Section 5 Layered UML |
| W2 | T7 Exception handling | GlobalExceptionHandler, ApiError (RFC 7807) | Section 5 |
| W2 | T8 AOP logging/audit | LoggingAspect, AuditAspect | Section 5, Section 10 |
| W3 | T9 Task CRUD + JPA | TaskRepository, TaskSpecifications, TaskController | Section 5, Section 7.2 |
| W3 | T10 Transactions | @Transactional on services, V3__audit_log.sql | Section 10 |
| W3 | T11 Redis caching | RedisConfig, TaskCacheService | Section 7.2 |
| W3 | T12 Test suite | unit / @WebMvcTest / @DataJpaTest / Testcontainers e2e | all |
| W4 | T13 Kafka events | TaskEvent, TaskEventPublisher/Consumer, KafkaConfig | Section 8 Kafka |
| W4 | T14 Rate limit + circuit breaker | RateLimitFilter, ExternalServiceClient | Section 9 Resilience |
| W4 | T15 Observability | ObservabilityConfig, logback-spring.xml | Section 11 Observability |
| W4 | T16 Docker + CI | Dockerfile, .github/workflows/ci.yml, README.md | Section 12 CI/CD |

---

## 15. Key Technology Decisions

| Concern | Choice | Rationale |
|---|---|---|
| Primary DB | PostgreSQL 15 | ACID, UUID PKs, partial indexes, JSONB audit values |
| Schema migrations | Flyway | Versioned, checksum-verified, reproducible environments |
| Caching | Redis 7 | Sub-ms reads; doubles as the rate-limit token store |
| Async messaging | Apache Kafka | Per-workspace ordering, replay, DLQ |
| Auth | JWT (HS256) + rotating refresh tokens | Stateless and horizontally scalable; short access TTL limits theft window |
| ORM | Spring Data JPA / Hibernate | Specifications for dynamic queries; @EntityGraph kills N+1 |
| Redis serialization | Jackson JSON | Schema-flexible, debuggable, version-tolerant vs Java serialization |
| Resilience | Resilience4j | Composable CircuitBreaker + Retry + TimeLimiter + fallback |
| Metrics | Micrometer -> Prometheus -> Grafana | Vendor-neutral; RED dashboards |
| Tracing | Brave -> Zipkin | W3C traceparent; correlates logs across boundaries |
| Logging | Logback + logstash-encoder | Structured JSON with MDC correlationId |
| Container | Multi-stage Docker (JRE Alpine) | Small image; cgroup-aware JVM memory |
| CI | GitHub Actions + JaCoCo >= 80pct | Fail-fast; immutable artifacts tagged by Git SHA |
| Soft delete | deleted_at + @Where + partial unique index | Auto-filtered queries; re-registration after delete |
| Concurrency | Optimistic @Version + @Retryable | Low overhead on the read path; retry on conflict |

---

Document maintained at `taskpulse/docs/ARCHITECTURE.md`. Update diagrams alongside code changes so the design stays the single source of truth.
