# pod-secrets-config-source

Quarkus extension that registers pod-secrets as a SmallRye `ConfigSource` with ordinal 450 — above env vars (300) and system properties (400), below Consul (500).

## Activation

Automatic via `RunTimeConfigBuilderBuildItem`. No annotation, no `META-INF/services` entry needed in the consuming application.

## Disabling

```properties
pod.secrets.enabled=false
```

## Usage

```java
// Any of the three forms resolves to the same file value:
@ConfigProperty(name = "db.password")   String pass;
@ConfigProperty(name = "DB_PASSWORD")   String pass;
@ConfigProperty(name = "db_password")   String pass;
```

## Modules

| Artifact | Purpose |
|---|---|
| `pod-secrets-config-source` | Runtime — `ConfigSource` + factory |
| `pod-secrets-config-source-deployment` | Build-time processor, registers the `ConfigBuilder` |
