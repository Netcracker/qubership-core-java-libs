# AGENTS.md — core-microservice-framework-extensions

## Module

Optional add-ons for `core-microservice-framework`: SpringDoc/OpenAPI, health indicators, and Micrometer metrics.

## Sub-modules

- **`framework-extension-springdoc-swagger`** — SpringDoc/OpenAPI 3 Swagger UI integration.
- **`framework-extension-health-indicators`** — custom Spring Boot health indicator beans.
- **`framework-extension-metrics`** — Micrometer metrics extensions.
- **`framework-extensions-parent`** — shared build POM.
- **`framework-extension-bom`** — BOM for consumers.
- **`framework-extensions-report-aggregate`** — JaCoCo aggregate.

## Build

```bash
mvn verify
```

## Extension Guide

- New OpenAPI customisation → add to `framework-extension-springdoc-swagger`.
- New health indicator → add to `framework-extension-health-indicators`; implement Spring Boot's `HealthIndicator`.
- New metric → add to `framework-extension-metrics`; register via Micrometer `MeterRegistry`.
- New extension artifact → create sub-module, add to `framework-extensions-parent` modules list and `framework-extension-bom`.
