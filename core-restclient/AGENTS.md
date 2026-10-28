# AGENTS.md — core-restclient

## Module

API abstraction over HTTP client implementations so callers are decoupled from WebClient/RestTemplate internals.

## Sub-modules

- **`microservice-restclient-api`** — interfaces and request/response DTOs (no implementation).
- **`microservice-restclient-webclient`** — **preferred** WebClient-backed implementation.
- **`microservice-restclient-resttemplate`** — **deprecated** RestTemplate-backed implementation; no new development.
- **`microservice-restclient-test-utils`** — mock implementations and helpers for unit tests.
- **`parent`** — shared POM.
- **`microservice-restclient-report-aggregate`** — JaCoCo aggregate.

## Build

```bash
mvn verify
```

## Extension Guide

- New HTTP feature → add interface to `microservice-restclient-api`, implement in `microservice-restclient-webclient`.
- Add test helpers for new features to `microservice-restclient-test-utils`.

## Notes

- Test code should depend on `microservice-restclient-test-utils` rather than Mockito-mocking the API interfaces directly.
- `microservice-restclient-resttemplate` receives no new development.
