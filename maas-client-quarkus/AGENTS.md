# AGENTS.md — maas-client-quarkus

## Module

Quarkus CDI integration of `maas-client` core. Provides Arc producers for `MaaSAPIClient` and SmallRye Reactive Messaging interceptors for context propagation and blue/green pause on Kafka and RabbitMQ channels.

## Sub-modules

- **`maas-client-quarkus-common`** — shared Arc producers and configuration.
- **`maas-client-quarkus-kafka`** — Kafka channel interceptors.
- **`maas-client-quarkus-rabbit`** — RabbitMQ channel interceptors.
- **`maas-client-quarkus-bom`** — BOM for consumers.
- **`maas-client-quarkus-report-aggregate`** — JaCoCo aggregate.

## Build

```bash
mvn verify
```

## Extension Guide

- Core/framework-agnostic logic lives in `maas-client` — do not duplicate it here.
- New CDI producer → add to `maas-client-quarkus-common` using Arc `@Produces`.

## Notes

- CDI (Arc) producers only — no Spring annotations.
