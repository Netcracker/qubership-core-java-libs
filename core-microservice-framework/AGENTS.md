# AGENTS.md — core-microservice-framework

## Module

Spring Boot scaffolding providing out-of-the-box microservice capabilities: route registration, config-server integration, security policy, M2M / user-secured REST clients, header interceptors, and DBaaS integration.

## Sub-modules

- **`microservice-framework-common`** — shared Spring utilities, `MicroserviceApplicationContext`, property loaders.
- **`microservice-framework-webclient`** — **preferred** RestClient implementation based on Spring WebClient.
- **`microservice-framework-resttemplate`** — **deprecated** RestTemplate-based RestClient. No new development.
- **`microservice-framework-parent`** — shared POM (no code).
- **`microservice-framework-report-aggregate`** — JaCoCo aggregate.

## Build

```bash
mvn verify          # PMD analysis runs automatically during verify; violations fail the build
```

## Entry Points

- `MicroserviceApplicationBuilder` — fluent builder wiring all framework features into a Spring `ApplicationContext`.
- `MicroserviceApplicationContext` — extended `ApplicationContext` exposing framework-managed beans.

## Extension Guide

- New feature → add to `microservice-framework-webclient` (never to `resttemplate`).
- New shared Spring utility → add to `microservice-framework-common`.

## Notes

- Always depend on `microservice-framework-webclient`, never on `microservice-framework-resttemplate` in new development.
- PMD rules are configured in `microservice-framework-parent`; violations fail the build.
