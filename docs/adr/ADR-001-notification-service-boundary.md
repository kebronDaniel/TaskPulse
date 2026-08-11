# ADR-001: Extract Notification Service

- Status: Accepted
- Date: 2026-08-11

## Context

TaskPulse currently owns task management, users, workspaces, projects, the transactional outbox, and an unused notification domain model.

Notifications have different operational characteristics from task management:

- Notification processing is asynchronous.
- Temporary notification outages must not block task operations.
- Notification delivery may require independent retry and scaling policies.
- Kafka already provides a durable integration boundary through TaskPulse's transactional outbox.
- Future channels such as email may introduce provider-specific resilience and delivery tracking.

Adding a Kafka listener inside the existing TaskPulse process would create an event-driven component, but it would not provide an independently deployable microservice.

## Decision

Create an independently deployable Notification Service in the TaskPulse monorepo.

Notification Service will:

- Run as a separate Spring Boot process.
- Produce a separate executable JAR and Docker image.
- Own its PostgreSQL database and Flyway migrations.
- Consume versioned TaskPulse integration events from Kafka.
- Use a dedicated Kafka consumer group.
- Store TaskPulse identifiers as UUID values rather than cross-service JPA relationships.
- Process events idempotently using `eventId`.
- Expose notification-specific REST endpoints.
- Validate authentication independently.
- Publish its own health, metrics, logs, and traces.

TaskPulse will remain the owner of users, workspaces, projects, tasks, and task-event publication.

Notification Service will not read from or write to the TaskPulse database.