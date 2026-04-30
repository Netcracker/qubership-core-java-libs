# AGENTS.md — core-blue-green-state-monitor-quarkus

## Module

Quarkus CDI integration of `blue-green-state-monitor-java`. Replaces the Spring auto-configuration with Quarkus Arc producers and Quarkus lifecycle event listeners.

## Build

```bash
mvn verify
```

## Extension Guide

- Consul watch and lock logic lives in `core-blue-green-state-monitor/blue-green-state-monitor-java` — do not duplicate it here.
- New CDI producer → add to `src/main/java/` following existing Arc `@Produces` pattern.

## Notes

- Quarkus version: 3.33.1.
