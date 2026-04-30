# AGENTS.md — maas-declarative-client-quarkus

## Module

Quarkus CDI integration of the declarative Kafka client. Provides Arc-based bean registration for MaaS-managed Kafka producers and consumers.

## Sub-modules

- **`maas-kafka-quarkus-client`** — Arc producers and Quarkus build-time extensions for declarative Kafka.
- **`maas-kafka-quarkus-client-report-aggregate`** — JaCoCo aggregate.

## Build

```bash
mvn verify
```

## Extension Guide

- Shared Kafka logic → add to `maas-declarative-client-commons/maas-kafka-client`.
- Quarkus-specific wiring (Arc producers, build-time extensions) → add here in `maas-kafka-quarkus-client`.

## Notes

- Quarkus version: 3.33.1.
