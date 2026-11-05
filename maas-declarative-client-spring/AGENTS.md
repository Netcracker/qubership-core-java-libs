# AGENTS.md — maas-declarative-client-spring

## Module

Spring Boot integration for the declarative Kafka client. Wires MaaS topic names from the MaaS control plane at startup via annotation-based consumer/producer configuration.

## Sub-modules

- **`maas-kafka-spring-client`** — Spring auto-configuration and bean post-processing for declarative Kafka.

## Build

```bash
mvn verify
```

## Extension Guide

- Shared annotation logic → add to `maas-declarative-client-commons/maas-kafka-client`.
- Spring-specific glue (auto-configuration, `BeanPostProcessor`) → add here in `maas-kafka-spring-client`.
