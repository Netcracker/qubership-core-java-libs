# pod-secrets-provider-spring

Spring Boot integration for pod-secrets. Registers a `MapPropertySource` before `systemEnvironment` so that file-based secrets override environment variables. Consul (ConfigData) still wins over pod-secrets.

## Activation

Automatic — no `@EnableX` or extra dependency needed. The `EnvironmentPostProcessor` is registered via `META-INF/spring/org.springframework.boot.env.EnvironmentPostProcessor.imports`.

## Disabling

```properties
pod.secrets.enabled=false
```

## Property source order

Pod-secrets is positioned between Consul and environment variables, so:

```
ConfigData (Consul)         — overrides pod-secrets
pod-secrets-property-source — overrides systemEnvironment
systemEnvironment
```

## DI access

A `PodSecretsLoader` bean is published by `PodSecretsAutoConfiguration` for cases where direct programmatic access is needed:

```java
@Autowired PodSecretsLoader loader;
String val = loader.getValue("db_password");
```
