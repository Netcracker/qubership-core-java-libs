# Warning: Module is obsolete
This module is obsolete. Please migrate to https://github.com/Netcracker/qubership-maas-client-quarkus

# MaaS Client

MaaS provide quarkus extension based on plain java core maas library: [maas-client-java](https://github.com/Netcracker/qubership-maas-client).

To include extension to your project add:
```xml
<dependency>
    <groupId>com.netcracker.cloud.maas.client</groupId>
    <artifactId>maas-client-quarkus</artifactId>
    <version>4.4.6</version>
</dependency>
```

It proivides default MaaSClient and KafkaMaaSClient bean instances to inject:
```java
@Inject
MaaSAPIClient maasClient;

@Inject
KafkaMaaSClient kafkaClient;

@Inject
RabbitMaaSClient rabbitClient;
```
It is preconfigured to use microservice M2M token for all outgoing REST API calls.

