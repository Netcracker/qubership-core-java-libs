# AGENTS.md — core-context-propagation

## Module

Propagates named execution contexts (headers, trace IDs, locale, etc.) transparently across HTTP and messaging boundaries in Spring microservices.

## Sub-modules

- **`context-propagation-core`** — provider SPI, context registry, `ContextSnapshot`, storage strategies.
- **`framework-contexts`** — built-in contexts: `Accept-Language`, `X-Request-Id` (auto-generated), `Api-Version`, `X-Version`, `X-Version-Name`, `X-Nc-Client-Ip`, `Business-Process-Id`, `Originating-Bi-Id`, configurable custom headers.
- **`spring-context-aggregator`** — Spring `HandlerInterceptor` (inbound) + `ClientHttpRequestInterceptor`/Kafka/Rabbit interceptors (outbound).
- **`context-propagation-test-extensions`** — JUnit 5 test utilities (Jandex index rebuilder).
- **`context-propagation-bom`** — BOM for consumers.
- **`api-tests`**, **`sample-context-tests`** — integration/API compatibility test suites.
- **`context-propagation-report-aggregate`** — JaCoCo aggregate.

## Build

```bash
mvn verify                  # full build including api-tests and sample-context-tests
mvn generate-sources        # rebuild Jandex index after adding a new provider
```

## Architecture

1. **Provider discovery via Jandex** — not classpath reflection. Jandex index must be regenerated (`mvn generate-sources`) after adding a new provider class, otherwise it won't be picked up at runtime.
2. **Storage strategies**: default `ThreadLocal`; use `ThreadLocalWithInheritance` when submitting tasks to child threads.
3. **Async propagation**: wrap `ExecutorService` with the provided wrapper, or capture a `ContextSnapshot` and restore it inside the `Callable`/`Supplier`.
4. **Serialisation**: snapshots are JSON-serialised for async messaging hand-off.

## Extension Guide

- New built-in context → add a `ContextProvider` implementation in `framework-contexts/src/main/java/`, annotate it for Jandex, then run `mvn generate-sources`.
- New Spring interceptor → add to `spring-context-aggregator`.
