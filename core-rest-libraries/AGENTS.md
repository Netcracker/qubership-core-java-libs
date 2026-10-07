# AGENTS.md — core-rest-libraries

## Module

Collection of REST-layer utilities consumed by `core-microservice-framework` and services directly.

## Sub-modules

| Sub-module | Contents |
|---|---|
| `webclient` | Spring WebClient builder helpers, timeout/retry config (`SmartWebClient`, `@EnableFrameworkWebClient`) |
| `restlegacy` | Legacy RestTemplate helpers — no new code |
| `route-registration` | Dynamic route registration (`@Routes`, `@Route` annotations) |
| `config-server-loader` | Loads properties from Spring Cloud Config Server at startup (`@EnableConfigServerLoaderOnWebClient`) |
| `consul-config-provider` | Consul KV-based `PropertySource` |
| `rest-api-deprecation-switcher` | Request-based toggle for deprecated API endpoints |
| `log-manager` | Structured JSON logging configuration |
| `security` | JWT parsing, RBAC helpers, security filter beans |
| `rest-third-party` | Thin wrappers over external REST clients |
| `rest-libraries-bom` | BOM for consumers |
| `rest-libraries-parent` | Shared POM |
| `report-aggregate` | JaCoCo aggregate |

## Build

```bash
mvn verify
```

## Extension Guide

- New WebClient feature → add to `webclient`; use `TlsUtils` from `core-utils` for any TLS setup.
- New route annotation → add to `route-registration`.
- New security utility → add to `security`.

## Notes

- `restlegacy` receives no new development — use `webclient` sub-module.
- Depends on `core-utils` for TLS; do not re-implement TLS logic here.
