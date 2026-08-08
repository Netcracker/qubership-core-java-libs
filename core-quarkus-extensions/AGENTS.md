# AGENTS.md — core-quarkus-extensions

## Module

Quarkus extensions mirroring the Spring functionality in `core-microservice-framework` and related modules.

## Sub-modules

| Sub-module | Spring counterpart |
|---|---|
| `context` | `core-context-propagation` Spring modules |
| `security` | `core-rest-libraries/security` |
| `log-manager` | `core-rest-libraries/log-manager` |
| `config-sources` | `core-rest-libraries/config-server-loader` + `consul-config-provider` |
| `dbaas-client` | `dbaas-client` Spring starters |
| `routes-registrator` | `core-rest-libraries/route-registration` |
| `maas-client` | `maas-client-spring` |
| `stomp-ws-server` | (no Spring equivalent) |
| `rest-api-deprecation-switcher` | `core-rest-libraries/rest-api-deprecation-switcher` |
| `cloud-core-quarkus-bom` | BOM for consumers |
| `build-parent` | Shared build POM |
| `report-aggregate` | JaCoCo aggregate |

## Build

```bash
mvn verify
```

## Extension Guide

- New extension → create sub-module, register it in `cloud-core-quarkus-bom/cloud-core-quarkus-bom-publish/pom.xml` so consumers can import without specifying a version.
- CDI (Arc) producers only — no Spring annotations.

## Notes

- Quarkus version: see `quarkus.platform.version` property in `pom.xml`.
