# Consul Client

Provides Consul client with M2M authorization.

To include extension to your project add:
```xml
<dependency>
    <groupId>org.qubership.cloud.quarkus</groupId>
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
`org.qubership.cloud.consul.config.source.runtime.ConsulClientConfiguration#innerConsulClient` to interact with Consul.
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

#### Configuration properties
| Property name                                | Description                                                   | Default value                                             |                                        
|----------------------------------------------|---------------------------------------------------------------|-----------------------------------------------------------|
| quarkus.consul-source-config.enabled         | Enable configuration approach with Consul (bool)              | true                                                      |
| quarkus.consul-source-config.agent.url       | Consul agent URL                                              |                                                           |
| quarkus.consul-source-config.properties-root | List of properties roots                                      | config/$namespace/application, config/$namespace/$appName |
| quarkus.consul-source-config.wait-time       | Maximum Value for Consul blocking queries wait time (seconds) | 570                                                       |