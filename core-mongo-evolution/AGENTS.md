# AGENTS.md — core-mongo-evolution

## Module

MongoDB schema migration tool. Scans `@ChangeLog`-annotated classes, tracks applied versions in a dedicated collection, and applies pending migrations on startup (analogous to Flyway/Liquibase for MongoDB).

## Sub-modules

- **`mongo-evolution-java`** — pure Java implementation using `mongodb-driver-sync` 4.x; no Spring dependency.
- **`mongo-evolution-spring`** — Spring Boot auto-configuration wrapping the Java core with `MongoTemplate` support.
- **`mongo-evolution-report-aggregate`** — JaCoCo aggregate.

## Build

```bash
mvn verify
```

## Key Pattern

1. Annotate a class with `@ChangeLog(order = N)`.
2. Annotate migration methods with `@ChangeSet(order = "NNN", id = "unique-id", author = "name")`.
3. `MongoEvolution` / `SpringMongoEvolution` discovers and executes them in order, recording applied IDs.

## Extension Guide

- New migration → new `@ChangeLog` class or new `@ChangeSet` method in an existing one. IDs must be globally unique and must never be reused.
- Do not reorder or delete already-executed `@ChangeSet` entries — applied IDs are recorded in the DB.

## Notes

- Uses `mongodb-driver-sync` 4.x; `MongoCollection.find()` return types differ from 3.x.
