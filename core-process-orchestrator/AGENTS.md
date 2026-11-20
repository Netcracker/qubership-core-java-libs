# AGENTS.md — core-process-orchestrator

## Module

Persistent, dependency-aware task scheduling and execution framework. Tasks and states are stored in a relational database via `db-scheduler` so they survive process restarts.

## Build

```bash
mvn verify
```

## Architecture

- **`ProcessDefinition`** — declares a set of named tasks and their dependency edges (DAG).
- **`ProcessOrchestrator`** — entry point: submits a `ProcessDefinition`, drives execution respecting dependencies, handles timeouts.
- **`TaskExecutorService`** — pluggable interface; implementations provide actual business logic.
- Persistence: HikariCP 7.0.2 + `db-scheduler` 16.7.1. Task payloads serialised with Jackson 2.21.2.

## Key Dependencies

- `db-scheduler` 16.7.1, HikariCP 7.0.2, Jackson 2.21.2
- Tests: JUnit Jupiter 6.0.3, Awaitility 4.3.0

## Extension Guide

- New task type → implement `TaskExecutorService`, register in `ProcessDefinition`.
- Task payloads must be Jackson-serialisable (needed for persistence and restart recovery).

## Notes

- No Spring/Quarkus dependency — deliberately framework-agnostic.
