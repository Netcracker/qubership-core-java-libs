# AGENTS.md — maas-client-spring

## Module

Spring Boot auto-configuration wrapping `maas-client` core for Kafka and RabbitMQ.

## Sub-modules

- **`maas-client-spring`** — Spring Boot starter: auto-configures `MaaSAPIClient` bean.
- **`maas-client-spring-kafka`** — Spring Kafka listener container factory with context propagation and blue/green pause.
- **`maas-client-spring-rabbit`** — Spring AMQP listener container factory with equivalent features.
- **`maas-client-spring-report-aggregate`** — JaCoCo aggregate.

## Build

```bash
mvn verify
```

## Key Dependencies

- Spring Boot 4.0.5, Spring Cloud 2025.1.1
- Lombok (delombok sources generated at build time by `lombok-maven-plugin`)

## Extension Guide

- New Spring-specific MaaS feature → add to `maas-client-spring`.
- New Kafka integration → add to `maas-client-spring-kafka`.
- New RabbitMQ integration → add to `maas-client-spring-rabbit`.
- Core (framework-agnostic) logic belongs in `maas-client`, not here.
