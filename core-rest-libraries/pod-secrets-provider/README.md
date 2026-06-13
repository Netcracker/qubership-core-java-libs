# pod-secrets-provider

Reads Kubernetes pod-secrets mounted as files and exposes them as a property source for Spring Boot and Quarkus — no consumer configuration required.

## Modules

| Artifact | Purpose |
|---|---|
| `pod-secrets-provider-common` | Pure-Java loader, shared by both frameworks |
| `pod-secrets-provider-spring` | Spring `EnvironmentPostProcessor` + `@AutoConfiguration` |

The Quarkus counterpart lives in `core-quarkus-extensions/config-sources/pod-secrets-config-source`.

## How it works

```
/etc/secrets/pod-secrets/
  db_password      → "s3cr3t"
  api_token        → "tok"
```

Each file becomes a property. The key is published in three forms so consumers don't need to care about notation:

| File | Published as |
|---|---|
| `db_password` | `db_password` · `DB_PASSWORD` · `db.password` |

Values are cached with a 60 s TTL (configurable). After a Kubernetes Secret rotation the new value is visible within one TTL cycle. The default matches the default kubelet sync period, so there is no benefit in polling faster.

## Priority

| Source | Quarkus ordinal | Spring position |
|---|---|---|
| Consul | 500 | ConfigData |
| **pod-secrets** | **450** | **before `systemEnvironment`** |
| System properties | 400 | `systemProperties` |
| Environment variables | 300 | `systemEnvironment` |

## Configuration

| Property | Env var | Default | Description |
|---|---|---|---|
| `pod.secrets.dir` | `POD_SECRETS_DIR` | `/etc/secrets/pod-secrets/` | Secrets directory |
| `pod.secrets.ttl` | — | `PT5S` | Cache TTL (ISO-8601) |
| `pod.secrets.enabled` | — | `true` | Set `false` to disable entirely |

## Inclusion

**Spring** — added as a dependency to `microservice-framework-common`, so any app using `netcracker-spring-boot-starter-parent` gets it automatically.

**Quarkus** — registered in `cloud-core-quarkus-bom`. The extension activates via `RunTimeConfigBuilderBuildItem` with no annotation or property needed from the application.
