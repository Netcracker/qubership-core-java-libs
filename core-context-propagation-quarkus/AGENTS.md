# AGENTS.md — core-context-propagation-quarkus

## Module

Quarkus CDI-native port of `core-context-propagation`. Provides the same context extraction/injection pipeline integrated with Quarkus Arc and Vert.x request pipeline instead of Spring MVC.

## Sub-modules

- **`context`** — Arc CDI producers and Vert.x interceptors for inbound/outbound context propagation.
- **`framework-contexts`** — Quarkus-specific wiring for built-in contexts (shared definitions from `core-context-propagation/framework-contexts`).
- **`integration-tests`** — integration test suite.
- **`bom`** — BOM for consumers.
- **`build-parent`** — shared build POM.
- **`report-aggregate`** — JaCoCo aggregate.

## Build

```bash
mvn verify
mvn generate-sources        # rebuild Jandex index after adding a new provider
```

## Extension Guide

- New context → add the `ContextProvider` implementation in `core-context-propagation/framework-contexts` first; add a Quarkus-specific shim here only if the integration requires it.
- Jandex index rebuilt by `io.smallrye:jandex-maven-plugin` during `generate-sources`.

## Notes

- Quarkus version: 3.33.1. CDI (Arc) producers only — no Spring annotations.
