# AGENTS.md — core-error-handling

## Module

Exception hierarchy based on numeric error codes, plus REST layer mapping to RFC-compliant problem JSON.

## Sub-modules

- **`core-error-handling-runtime`** — base `ErrorCodeException`, `ErrorCode` contract, `MultiCauseException`.
- **`core-error-handling-rest`** — `@ControllerAdvice` / `@ExceptionHandler` that maps `ErrorCodeException` to problem JSON responses.
- **`core-error-handling-report-aggregate`** — JaCoCo aggregate; not a shipping artifact.

## Build

```bash
mvn verify
```

## Key Pattern

All application exceptions must extend `ErrorCodeException` and supply an `ErrorCode`. The REST handler in `core-error-handling-rest` serialises them automatically — do not add ad-hoc `@ExceptionHandler` methods for error-coded exceptions in service code.

## Extension Guide

- New error code → implement `ErrorCode` interface in `core-error-handling-runtime`.
- New HTTP mapping rule → add to the `@ControllerAdvice` in `core-error-handling-rest`.
