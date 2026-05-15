# AGENTS.md — maas-declarative-client-commons

## Module

Shared utilities for annotation-driven (declarative) Kafka client configuration. Primary sub-module: `maas-kafka-client`.

## Sub-modules

- **`maas-kafka-client`** — annotation processor, declarative Kafka producer/consumer wiring, MaaS topic resolution.

## Build

```bash
mvn verify                  # integration tests require Docker (Testcontainers + real Kafka)
mvn verify -DskipTests      # compile and unit tests only
```

## Key Dependencies

- `kafka-clients` 4.2.0
- Tests: Mockito 5.21.0, AssertJ 3.27.7, Testcontainers

## Extension Guide

- New declarative annotation → add to `maas-kafka-client/src/main/java/`; add processing logic in the annotation processor.
- Consumers should depend on `maas-declarative-client-spring` or `maas-declarative-client-quarkus`, not this module directly.

## Notes

- Uses `flatten-maven-plugin` — the published POM has all version references resolved.
