# MaaS Client

MaaS provide quarkus extension based on plain java core maas library: maas-client-java.

To include extension to your project add:
```xml
<project>
    <dependencyManagement>
        <dependency>
            <groupId>org.qubership.cloud.quarkus</groupId>
            <artifactId>maas-client-quarkus-bom</artifactId>
            <version>x.x.x</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencyManagement>

    <dependencies>
        <dependency>
            <groupId>org.qubership.cloud.quarkus</groupId>
            <artifactId>maas-client-quarkus-kafka</artifactId>
        </dependency>
        <dependency>
            <groupId>org.qubership.cloud.quarkus</groupId>
            <artifactId>maas-client-quarkus-rabbit</artifactId>
        </dependency>
    </dependencies>
</project>
```

It proivides default MaaSClient bean instance to inject.
There is also a DeploymentVersionTracker bean for quarkus:
```java
@Inject
MaaSAPIClient maasClient;

@Inject
DeploymentVersionTracker tracker;
```


For Rabbit there is a default maas client rabbit bean and also DynamicQueueBindingsManager if you want to use dynamic bindings:

```java
@Inject
RabbitMaaSClient rabbitClient;

@Inject
DynamicQueueBindingsManager manager;
```



For Kafka:

```java
@Inject
KafkaMaaSClient kafkaClient;
```




