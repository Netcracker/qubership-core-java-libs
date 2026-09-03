# Consul Client

Provides Consul client with M2M authorization.

To include extension to your project add:
```xml
<dependency>
    <groupId>com.netcracker.cloud.quarkus</groupId>
    <artifactId>consul-client</artifactId>
    <version>your-version</version>
</dependency>
```

It provides default Consul Client and Consul token storage bean instances to inject:
```java
@Inject
TokenStorage tokenStorage;

@Inject
ConsulClient innerConsulClient;
```

You can use consul client that is available as bean from
`com.netcracker.cloud.consul.config.source.runtime.ConsulClientConfiguration#innerConsulClient` to interact with Consul.
Consul token is available from `TokenStorage` bean.

## Configure

Fill the config parameters. Typical default configuration:
```properties
quarkus.consul-source-config.enabled=true
quarkus.consul-source-config.agent.url=${CONSUL_URL}
quarkus.consul-source-config.properties-root=config/${cloud.microservice.namespace}/application,config/${cloud.microservice.namespace}/${cloud.microservice.name}
```

If no M2M auth needed(for localdev, tests, etc.) it can be disabled by setting property:
```properties
quarkus.consul-source-config.m2m.enabled=false
```

#### Login properties

The Consul ACL token is exchanged through `POST /v1/acl/login`. Two ways to obtain it are supported: `kubernetes` sends
the projected service account token of the pod, `m2m` sends an M2M token. The four properties below are read at runtime
when the `TokenStorage` bean is built, so the way can be switched without rebuilding the application.

In `kubernetes-with-m2m-fallback` mode the `kubernetes` way is tried first. If it fails, the pod falls back to the
`m2m` way and logs the reason, the Consul response code, and a truncated response body in a single `INFO` record. That
record marks the decision, so it appears once rather than on every retry; each login attempt logs an `INFO` record of
its own, naming the auth method it went to. In `kubernetes` mode there is no probe and no fallback. In `m2m` mode the
auth method name and the audience are not used at all.

`cloud.microservice.namespace` stays required in every mode, including `kubernetes`, where the namespace never reaches
Consul: the producer reads it whatever the mode says.

The fallback is temporary. Once `fallback-recheck-interval` has passed, the next scheduled relogin tries the
`kubernetes` way again, and the first success switches the pod over for good. Going back to `m2m` never happens. The
recheck rides on the scheduled relogin, so it needs `MaxTokenTTL` on the auth method: without it the token never
expires, nothing is scheduled, and nothing is rechecked.

`fallback-recheck-interval` therefore sets the lower bound on how often the pod retries, not the actual period. The
relogin runs at 80% of `MaxTokenTTL`, and the recheck waits for the first relogin past the interval, so with a
`MaxTokenTTL` of 24 hours a pod retries about every 19 hours whatever the interval says. Plan the migration of a fleet
against `MaxTokenTTL`, and lower it on the auth method if the pods have to move over sooner.

An unknown value of `quarkus.consul-source-config.login.mode` fails the startup, and so does a login failure the
retries do not fix: the `TokenStorage` bean cannot be produced without a token. With
`quarkus.consul-source-config.m2m.enabled=false` none of the four properties are read.

The default is the auth method the platform registers. Set `quarkus.consul-source-config.login.auth-method` only if
your Consul names it differently.

The `kubernetes` way reads the token from `/var/run/secrets/tokens/<audience>/token`. To run it outside a cluster, point
the token directory elsewhere with `com.netcracker.cloud.security.kubernetes.tokens.dir`.

#### Configuration properties
| Property name                                | Description                                                   | Default value                                             |                                        
|----------------------------------------------|---------------------------------------------------------------|-----------------------------------------------------------|
| quarkus.consul-source-config.enabled         | Enable configuration approach with Consul (bool)              | true                                                      |
| quarkus.consul-source-config.agent.url       | Consul agent URL                                              |                                                           |
| quarkus.consul-source-config.properties-root | List of properties roots                                      | config/$namespace/application, config/$namespace/$appName |
| quarkus.consul-source-config.wait-time       | Maximum Value for Consul blocking queries wait time (seconds) | 570                                                       |
| quarkus.consul-source-config.m2m.enabled     | Enable the Consul ACL token exchange (bool, build time)       | true                                                      |
| quarkus.consul-source-config.login.mode      | Way to obtain the ACL token: kubernetes-with-m2m-fallback, kubernetes or m2m          | kubernetes-with-m2m-fallback                              |
| quarkus.consul-source-config.login.auth-method | Consul auth method name, used by the kubernetes way         | applications-k8s-m2m                                      |
| quarkus.consul-source-config.login.audience  | Projected token audience, used by the kubernetes way          | netcracker                                                |
| quarkus.consul-source-config.login.fallback-recheck-interval | How often the fallback retries the kubernetes way | PT5H                                  |
