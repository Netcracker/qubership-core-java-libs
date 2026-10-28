# AGENTS.md — core-blue-green-state-monitor

## Module

Watches Consul KV for blue/green deployment state changes and provides lock management so services can pause work during a deployment flip.

## Sub-modules

- **`blue-green-state-monitor-java`** — pure Java core: Consul watch loop, state parser, lock API.
- **`blue-green-state-monitor-spring`** — Spring Boot auto-configuration: exposes `BlueGreenStatePublisher`, `GlobalMutexService`, `MicroserviceMutexService` beans and integrates with Spring lifecycle events.
- **`blue-green-state-monitor-report-aggregate`** — JaCoCo aggregate.

## Build

```bash
mvn verify -B -ntp -q                  # requires Docker (Testcontainers + real Consul)
mvn verify -B -ntp -q -DskipTests      # skip integration tests when Docker is unavailable
```

## Key Concepts

- **Global lock** (`GlobalMutexService`) — blocks all services until the operator clears it.
- **Microservice lock** (`MicroserviceMutexService`) — targets a specific service; others continue.
- Integration tests spin up a real Consul instance via Testcontainers 2.0.4.

## Extension Guide

- New lock type → extend lock API in `blue-green-state-monitor-java`; add Spring bean in `blue-green-state-monitor-spring`.
- Keep framework-specific code out of `blue-green-state-monitor-java`.
