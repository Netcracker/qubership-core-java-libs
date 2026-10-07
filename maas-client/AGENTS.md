# AGENTS.md — maas-client

## Module

Framework-agnostic core client for the Messaging-as-a-Service API. Handles topic/queue provisioning, tenant namespace management, and wires context propagation into Kafka and RabbitMQ messages.

## Sub-modules

| Sub-module | Purpose |
|---|---|
| `client` | `MaaSAPIClient` / `MaaSAPIClientImpl` — HTTP calls to MaaS control plane |
| `kafka-context-propagation` | Injects/extracts `ContextSnapshot` from Kafka message headers |
| `kafka-blue-green-consumer` | Kafka consumer that pauses during blue/green flips |
| `kafka-streams-adapter` | Kafka Streams `Processor` wrapper with context propagation |
| `rabbit-context-propagation` | Injects/extracts context from `AMQP.BasicProperties` |
| `rabbit-blue-green` | RabbitMQ consumer pause for blue/green flips |
| `deployment-version-tracker` | Tracks current deployment version for blue/green routing |
| `bom` | BOM for consumers |
| `report-aggregate` | JaCoCo aggregate |

## Build

```bash
mvn verify
```

## Architecture

- No Spring or Quarkus dependency — pure Java. Framework integrations live in `maas-client-spring` and `maas-client-quarkus`.
- Context propagation delegates to `core-context-propagation` for snapshot capture/restore.
- Blue/green-aware consumers delegate to `core-blue-green-state-monitor` for state detection.

## Extension Guide

- New messaging protocol → add `*-context-propagation` + `*-blue-green` sub-modules; keep them framework-agnostic.
- New MaaS API endpoint → extend `MaaSAPIClient` interface in `client`, implement in `MaaSAPIClientImpl`.
