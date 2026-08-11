# TaskPulse Integration Contracts

This directory contains language-neutral contracts exchanged between independently deployable services.

## Principles

- Contracts describe integration messages, not internal JPA entities.
- Every published event has an explicit schema version.
- Existing required fields are not removed or reinterpreted within a version.
- New optional fields may be added only when older consumers can safely ignore them.
- Breaking changes require a new event or schema version.
- Consumers must tolerate unknown JSON fields.
- Event examples and schemas are reviewed like public APIs.
- TaskPulse owns task-event publication.
- Consumer services own their processing and persistence models.

## Planned contracts

```text
contracts/
└── task-events/
    └── v1/
        ├── task-assigned.schema.json
        ├── task-assigned.example.json
        └── README.md
```

The first contract will be `TaskAssignedV1`

## Java sharing policy

Services will not share domain entities or persistence models.

A generated or deliberately minimal contract library may be introduced later if duplication becomes costly, but each service must remain independently buildable and must not depend on another service's application module.