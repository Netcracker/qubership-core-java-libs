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
the projected service account token of the pod, `m2m` sends an M2M token. The three properties below are read at runtime
when the `TokenStorage` bean is built, so the way can be switched without rebuilding the application.

A pod keeps one way for its whole life: in `auto` mode the first login decides, and a failure of a later login does not
switch the way back. In `auto` mode the `kubernetes` way is tried first, and on failure the pod falls back to the `m2m`
way and logs one `INFO` record with the reason, the Consul response code, and a truncated response body. In `kubernetes`
mode there is no probe and no fallback. In `m2m` mode the auth method name and the audience are not read at all.

An unknown value of `quarkus.consul-source-config.login.mode` fails the startup. With
`quarkus.consul-source-config.m2m.enabled=false` none of the three properties are read.

The default auth method name is a placeholder. Set `quarkus.consul-source-config.login.auth-method` to the name of the
auth method registered in your Consul.

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
| quarkus.consul-source-config.login.mode      | Way to obtain the ACL token: auto, kubernetes or m2m          | auto                                                      |
| quarkus.consul-source-config.login.auth-method | Consul auth method name, used by the kubernetes way         | kubernetes-auth-method-placeholder                        |
| quarkus.consul-source-config.login.audience  | Projected token audience, used by the kubernetes way          | netcracker                                                |
