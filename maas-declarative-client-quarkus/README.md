# Maas Quarkus Kafka

This module adds ability to work with Kafka via declarative approach or directly using MaaS API.

### How start work with Maas Quarkus Kafka Client:

#### 1. Add dependency on maas-kafka-quarkus-client

```xml

<dependency>
   <groupId>org.qbership.cloud.maas.declarative</groupId>
   <artifactId>maas-declarative-kafka-client-quarkus</artifactId>
</dependency>
```

#### 2. Configure kafka clients

There are two ways to configure MaasKafkaClient: Using application.yaml and Using runtime builders

##### 2.1 Application.yaml configuration

1. Add config properties to application.yml (All the config props described [bellow](#common-client-config-properties))

```yaml
maas:
  kafka:
    client:
      consumer:
        your_consumer_name:
          topic:
            actual-name: "actual_kafka_topic_name" # Optional
            name: your_topic_name
            template: "maas_template" # Optional
            namespace: namespace_name
            managedby: maas or self
            on-topic-exists: merge or fail # Optional 
            configs:
              kafka.topic.any.property: any_value # Optional
          is-tenant: true or false
          pool-duration: 1000 # Overrides common pool duration
          instance-count: 2 # used for concurrency handling
          dedicated-thread-pool-size: 5
          # default kafka client properties
          kafka-consumer:
            property:
              key:
                deserializer: org.apache.kafka.common.serialization.StringDeserializer
              value:
                deserializer: org.apache.kafka.common.serialization.StringDeserializer
              group:
                id: your_group_id
              ...
      producer:
        your_producer_name:
          topic:
            actualname: "actual_kafka_topic_name" # Optional
            name: your_topic_name
            template: "maas_template" # Optional
            namespace: namespace_name
            managedby: maas or self
            on-topic-exists: merge or fail # Optional 
            configs:
              kafka.topic.any.property: any_value # Optional
          is-tenant: true or false
          # default kafka client properties
          kafka-producer:
            property:
              key:
                serializer: org.apache.kafka.common.serialization.StringSerializer
              value:
                serializer: org.apache.kafka.common.serialization.StringSerializer
              ...              
```

2. Configure, initialize and activate clients (consumer/producer)

2.1 Configure clients with serializers/deserializers described in application.yml

```java

@Startup
@ApplicationScoped
public class SomeConfigClass {

    @Inject
    MaasKafkaClientFactory clientFactory;

    // Create kafka producer
    @Startup
    @Singleton
    MaasKafkaProducer maasKafkaProducer() {
        // Get producer according config described above
        MaasKafkaProducerCreationRequest producerCreationRequest =
                MaasKafkaProducerCreationRequest.builder()
                        .setProducerDefinition(clientFactory.getProducerDefinition("your_producer_name"))
                        .build();

        MaasKafkaProducer producer = clientFactory.createProducer(producerCreationRequest);
        producer.initAsync()
                .handle((v, e) -> {
                    if (e != null) {
                        // Handle error
                    } else {
                        // Post initialization logic
                        // for example let's activate producer
                        producer.activateAsync()
                                .handle((vd, ex) -> {
                                    if (ex != null) {
                                        // Handle error
                                    } else {
                                        // Post activation logic
                                    }
                                    return null;
                                });
                    }
                    return null;
                });
        return producer;
    }

    // Create kafka consumer
    @Startup
    @Singleton
    MaasKafkaConsumer maasKafkaConsumer(final KafkaProducer producer) {
        Consumer<ConsumerRecord<String, String>> recordConsumer = record -> {
            // Some logic
            MaasProducerRecord<String, String> someRecord = new MaasProducerRecord(
                    null,
                    // any key,
                    // any data,
                    null,
                    null
            );
            producer.sendSync(someRecord); // Sending as example
            // Some logic
        };
        // Optionally you can create custom errorhandler
        ConsumerErrorHandler errorHandler = (exception, errorRecord, handledRecords, consumer) -> {
            // Custom error handling logic
        };

        MaasKafkaConsumerCreationRequest consumerCreationRequest =
                MaasKafkaConsumerCreationRequest.builder()
                        .setConsumerDefinition(clientFactory.getConsumerDefinition("your_consumer_name"))
                        .setErrorHandler(errorHandler) // If no set used default error handler
                        .setHandler(recordConsumer)
                        .build();

        MaasKafkaConsumer consumer = clientFactory.createConsumer(consumerCreationRequest);

        consumer.initAsync()
                .handle((v, e) -> {
                    if (e != null) {
                        // Handle error
                    } else {
                        // Post initialization logic
                        // for example let's activate consumer
                        consumer.activateAsync()
                                .handle((vd, ex) -> {
                                    if (ex != null) {
                                        // Handle error
                                    } else {
                                        // Post activation logic
                                    }
                                    return null;
                                });
                    }
                    return null;
                });
        return consumer;
    }

}

```

2.2 Configure clients with serializers/deserializers as beans

### Note

> If you fill serializers/deserializers in application.yml and still would like use beans,
> in this case beans will be used instead (You can fill only value serializer/deserializer as bean and key serializer/deserializer
> set in application.yml or vice versa)

```java

@Startup
@ApplicationScoped
public class SomeConfigClass {

    @Inject
    MaasKafkaClientFactory clientFactory;

    @Inject
    @Named(value = "keyDeserializer")
    Deserializer<String> keyDeserializer;
    @Inject
    @Named(value = "valueDeserializer")
    Deserializer<String> valueDeserializer;

    @Inject
    @Named(value = "keySerializer")
    Serializer<String> keySerializer;
    @Inject
    @Named(value = "valueSerializer")
    Serializer<String> valueSerializer;

    // Create kafka producer
    @Startup
    @Singleton
    MaasKafkaProducer maasKafkaProducer() {
        // Get producer according config described above
        MaasKafkaProducerCreationRequest producerCreationRequest =
                MaasKafkaProducerCreationRequest.builder()
                        .setProducerDefinition(clientFactory.getProducerDefinition("your_producer_name"))
                        .setValueSerializer(valueSerializer)
                        .setKeySerializer(keySerializer)
                        .build();

        MaasKafkaProducer producer = clientFactory.createProducer(producerCreationRequest);

        producer.initAsync()
                .handle((v, e) -> {
                    if (e != null) {
                        // Handle error
                    } else {
                        // Post initialization logic
                        // for example let's activate producer
                        producer.activateAsync()
                                .handle((vd, ex) -> {
                                    if (ex != null) {
                                        // Handle error
                                    } else {
                                        // Post activation logic
                                    }
                                    return null;
                                });
                    }
                    return null;
                });
        return producer;
    }

    // Create kafka consumer
    @Startup
    @Singleton
    MaasKafkaConsumer maasKafkaConsumer(final KafkaProducer producer) {
        Consumer<ConsumerRecord<String, String>> recordConsumer = record -> {
            // Some logic
            ProducerRecord<String, String> someRecord = new ProducerRecord(
                    null,
                    // any key,
                    // any data,
                    null,
                    null
            );
            producer.sendSync(someRecord); // Sending as example
            // Some logic
        };

        MaasKafkaConsumerCreationRequest consumerCreationRequest =
                MaasKafkaConsumerCreationRequest.builder()
                        .setConsumerDefinition(clientFactory.getConsumerDefinition("your_consumer_name"))
                        .setHandler(recordConsumer)
                        .setValueDeserializer(valueDeserializer)
                        .setKeyDeserializer(keyDeserializer)
                        .build();

        MaasKafkaConsumer consumer = clientFactory.createConsumer(consumerCreationRequest);

        consumer.initAsync()
                .handle((v, e) -> {
                    if (e != null) {
                        // Handle error
                    } else {
                        // Post initialization logic
                        // for example let's activate consumer
                        consumer.activateAsync()
                                .handle((vd, ex) -> {
                                    if (ex != null) {
                                        // Handle error
                                    } else {
                                        // Post activation logic
                                    }
                                    return null;
                                });
                    }
                    return null;
                });
        return consumer;
    }

}

```

3. (Optional) External activation after initialization, activation this way move client from INITIALIZED state to
   ACTIVE, if this method will be called before client has been initialized, it will be waited until client became
   INITIALIZED (more info described in javadocs)

```java

@Startup
@ApplicationScoped
public class SomeConfig {

    @Inject
    ClientStateManagerService clientStateManagerService;

    //activate your kafka clients on ServiceReadyEvent
    void activateKafkaClients(@ObservesAsync ServiceReadyEvent event) {
        clientStateManagerService.activateInitializedClients(null);
    }
//...
}
```

### Note

> Also, you can initialize and activate client out of configuration

### Note

> Also, you can create consumer setting 'CustomProcessed' to true, when you create the client this way, both the event management process
> and multitenancy disabled

#### 3.2 Runtime configuration

Instead, step 1 from 3.1 there is a way to configure clients in runtime only

### Note

All contracts(Mandatory/Optional property) described for [application.yaml](#common-client-config-properties) config
implemented in builders

1. Create client definition using builders

```java

@Startup
@ApplicationScoped
public class SomeConfigClass {

    @Inject
    MaasKafkaClientFactory clientFactory;

    // Create kafka producer
    @Startup
    @Singleton
    MaasKafkaProducer maasKafkaProducer() {
        // Create topic definition
        TopicDefinition testTopicDefinition = TopicDefinition.builder()
                .setName("test_topic_name")
                .setNamespace("test_namespace")
                .setManagedBy(ManagedBy.SELF)
                .build();
        // Create producer definition
        MaasKafkaProducerDefinition producerDefinition = MaasKafkaProducerDefinition.builder()
                .setTopic(testTopicDefinition)
                .setTenant(true)
                .build();
        // Create producer creation request
        MaasKafkaProducerCreationRequest producerCreationRequest =
                MaasKafkaProducerCreationRequest.builder()
                        .setProducerDefinition(producerDefinition)
                        .build();

        MaasKafkaProducer producer = clientFactory.createProducer(producerCreationRequest);
        producer.initAsync()
                .handle((v, e) -> {
                    if (e != null) {
                        // Handle error
                    } else {
                        // Post initialization logic
                        // for example let's activate producer
                        producer.activateAsync()
                                .handle((vd, ex) -> {
                                    if (ex != null) {
                                        // Handle error
                                    } else {
                                        // Post activation logic
                                    }
                                    return null;
                                });
                    }
                    return null;
                });
        return producer;
    }

    // Create kafka consumer
    @Startup
    @Singleton
    MaasKafkaConsumer maasKafkaConsumer(final MaasKafkaProducer producer) {
        Consumer<ConsumerRecord<String, String>> recordConsumer = record -> {
            // Some logic
            ProducerRecord<String, String> someRecord = new ProducerRecord(
                    null,
                    // any key,
                    // any data,
                    null,
                    null
            );
            producer.sendSync(someRecord); // Sending as example
            // Some logic
        };

        // Create topic definition
        TopicDefinition testTopicDefinition = TopicDefinition.builder()
                .setName("test_topic_name")
                .setNamespace("test_namespace")
                .setManagedBy(ManagedBy.SELF)
                .build();
        // Create consumer definition
        MaasKafkaConsumerDefinition consumerDefinition = MaasKafkaConsumerDefinition.builder()
                .setTopic(testTopicDefinition)
                .setTenant(true)
                .setInstanceCount(1)
                .setPollDuration(1)
                .setGroupId("test-group")
                .build();
        // Create consumer creation request
        MaasKafkaConsumerCreationRequest consumerCreationRequest =
                MaasKafkaConsumerCreationRequest.builder()
                        .setConsumerDefinition(consumerDefinition)
                        .setHandler(recordConsumer)
                        .build();

        MaasKafkaConsumer consumer = clientFactory.createConsumer(consumerCreationRequest);

        consumer.initAsync()
                .handle((v, e) -> {
                    if (e != null) {
                        // Handle error
                    } else {
                        // Post initialization logic
                        // for example let's activate consumer
                        consumer.activateAsync()
                                .handle((vd, ex) -> {
                                    if (ex != null) {
                                        // Handle error
                                    } else {
                                        // Post activation logic
                                    }
                                    return null;
                                });
                    }
                    return null;
                });
        return consumer;
    }

}

```

Other steps related with activation/deactivation and Serializer/Deserializer logic are the same as in 3.1

#### 4. Manage client state during execution

```java
public class SomeClass {

    @Inject
    ClientStateManagerService clientStateManagerService;

    void onDeactivation() {
        clientStateManagerService.emitClientDeactivationEvent();
    }

    void onActivation() {
        clientStateManagerService.emitClientActivationEvent();
    }

}

```

### Common client config properties:

| Name                                 | Type    | Mandatory | Default value          | Description                                                  |
|--------------------------------------|---------|-----------|------------------------|--------------------------------------------------------------|
| maas.tracing.kafka.enabled           | Boolean | false     | true                   | Enables tracing in kafka                                     |
| maas.kafka.acceptable-tenants        | String  | false     |                        | List tenants for using in multitenant environment (dev only) |
| maas.kafka.consumer-thread-pool-size | Integer | false     | 2                      | The number of threads in pool for kafka consumers            |
| maas.kafka.consumer-pool-duration    | Integer | false     | 4000                   | Common consumer pool timeout in milliseconds                 |
| maas.agent.url                       | String  | false     | http://maas-agent:8080 | Contains maas agent url                                      |
| quarkus.jaeger.propagation           | String  | false     | b3                     | Context propagation type                                     |
| maas.tenant-manager.url              | String  | false     | tenant-manager:8080    | Contains Tenant Manager url                                  |

### Producer config properties:

| Name                                                                                                  | Type    | Mandatory | Default value | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    |
|-------------------------------------------------------------------------------------------------------|---------|-----------|---------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| maas.kafka.client.producer.[your-producer-name].topic.name                                            | String  | true      |               | The name of kafka topic                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        |
| maas.kafka.client.producer.[your-producer-name].topic.actual-name                                     | String  | false     |               | Actual topic name in kafka brocker (This property support placeholders {tanant-id} - resolve tenants from Tenant Manager  and {namespace} - resolved from property maas.kafka.client.producer.[your-producer-name].topic.namespace )                                                                                                                                                                                                                                                                           |
| maas.kafka.client.producer.[your-producer-name].topic.on-topic-exist                                  | String  | false     | fail          | merge or fail. By default fail and applying configuration is failed on topic collision. merge option ignore collision and insert registration in MaaS database linking it to existing topic                                                                                                                                                                                                                                                                                                                    |
| maas.kafka.client.producer.[your-producer-name].topic.template                                        | String  | false     |               | The [template](https://git.quvership.org/PROD.Platform.Cloud_Core/maas/-/blob/main/README.md#kafka-templates-lazy-topics-tenant-topics) of maas topic                                                                                                                                                                                                                                                                                                                                                          |
| maas.kafka.client.producer.[your-producer-name].topic.namespace                                       | String  | true      |               | Service namespace                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              |
| maas.kafka.client.producer.[your-producer-name].topic.managedby                                       | String  | true      |               | Used to indicate that topic should be crated by microservice in case of self. There are situations when we need to dynamically create topic and maas can't deal with it. For example ${quarkus.application.namespace}-${cpq.agm-core-integration.kafka.agreement-entity-type}_dbCleaningNotification-{tenant-id}. In this case we will create topic from microservice using MaaS API.Available values: maas/self Additional params like replication, partitions should be added in case of self topic creation |
| maas.kafka.client.producer.[your-producer-name].topic.configs.[nay-kafka-topic-property]              | String  | false     |               | Any kafka topic [property](https://kafka.apache.org/documentation.html#topicconfigs) (Usen only if managedby is "self")                                                                                                                                                                                                                                                                                                                                                                                        |
| maas.kafka.client.producer.[your-producer-name].is-tenant                                             | Boolean | true      |               | Tells producer that all topic used bgy them are tenants                                                                                                                                                                                                                                                                                                                                                                                                                                                        |
| maas.kafka.client.producer.[your-producer-name].key.serializer                                        | String  | false     |               | Serializer class for key that implements the org.apache.kafka.common.serialization.Serializer interface                                                                                                                                                                                                                                                                                                                                                                                                        |
| maas.kafka.client.producer.[your-producer-name].value.serializer                                      | String  | false     |               | Serializer class for value that implements the org.apache.kafka.common.serialization.Serializer interface                                                                                                                                                                                                                                                                                                                                                                                                      |
| maas.kafka.client.producer.[your-producer-name].kafka-producer.property.[any-kafka-producer-property] | String  | false     |               | Any kafka producer [property](http://kafka.apache.org/documentation.html#producerconfigs)                                                                                                                                                                                                                                                                                                                                                                                                                      |

### Consumer config properties:

| Name                                                                                                  | Type    | Mandatory | Default value | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    |
|-------------------------------------------------------------------------------------------------------|---------|-----------|---------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| maas.kafka.client.consumer.[your-consumer-name].topic.name                                            | String  | true      |               | The name of kafka topic                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        |
| maas.kafka.client.consumer.[your-consumer-name].topic.actual-name                                     | String  | false     |               | Actual topic name in kafka brocker (This property support placeholders {tanant-id} - resolve tenants from Tenant Manager  and {namespace} - resolved from property maas.kafka.client.producer.[your-producer-name].topic.namespace )                                                                                                                                                                                                                                                                           |
| maas.kafka.client.consumer.[your-consumer-name].topic.on-topic-exist                                  | String  | false     | fail          | merge or fail. By default fail and applying configuration is failed on topic collision. merge option ignore collision and insert registration in MaaS database linking it to existing topic                                                                                                                                                                                                                                                                                                                    |
| maas.kafka.client.consumer.[your-consumer-name].topic.template                                        | String  | false     |               | The [template](https://git.qubership.org/PROD.Platform.Cloud_Core/maas/-/blob/main/README.md#kafka-templates-lazy-topics-tenant-topics) of maas topic                                                                                                                                                                                                                                                                                                                                                          |
| maas.kafka.client.consumer.[your-consumer-name].topic.namespace                                       | String  | true      |               | Service namespace                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              |
| maas.kafka.client.consumer.[your-consumer-name].topic.managedby                                       | String  | true      |               | Used to indicate that topic should be crated by microservice in case of self. There are situations when we need to dynamically create topic and maas can't deal with it. For example ${quarkus.application.namespace}-${cpq.agm-core-integration.kafka.agreement-entity-type}_dbCleaningNotification-{tenant-id}. In this case we will create topic from microservice using MaaS API.Available values: maas/self Additional params like replication, partitions should be added in case of self topic creation |
| maas.kafka.client.consumer.[your-consumer-name].topic.configs.[nay-kafka-topic-property]              | String  | true      |               | Any kafka topic [property](https://kafka.apache.org/documentation.html#topicconfigs) (Usen only if managedby is "self")                                                                                                                                                                                                                                                                                                                                                                                        |
| maas.kafka.client.consumer.[your-consumer-name].is-tenant                                             | String  | true      |               | Tells consumer that all topic used bgy them are tenants                                                                                                                                                                                                                                                                                                                                                                                                                                                        |
| maas.kafka.client.consumer.[your-consumer-name].key.deserializer                                      | String  | false     |               | Deserializer class for key that implements the org.apache.kafka.common.serialization.Deserializer interface                                                                                                                                                                                                                                                                                                                                                                                                    |
| maas.kafka.client.consumer.[your-consumer-name].value.deserializer                                    | String  | false     |               | Deserializer class for value that implements the org.apache.kafka.common.serialization.Deserializer interface                                                                                                                                                                                                                                                                                                                                                                                                  |
| maas.kafka.client.consumer.[your-consumer-name].group.id                                              | String  | true      |               | A unique string that identifies the consumer group this consumer belongs to                                                                                                                                                                                                                                                                                                                                                                                                                                    |
| maas.kafka.client.consumer.[your-consumer-name].kafka-consumer.property.[any-kafka-consumer-property] | String  | false     |               | Any kafka consumer [property](http://kafka.apache.org/documentation.html#consumerconfigs)                                                                                                                                                                                                                                                                                                                                                                                                                      |
| maas.kafka.client.consumer.[your-consumer-name].dedicated-thread-pool-size                            | Integer | false     |               | If defined - a separate thread pool will be created with defined number of threads                                                                                                                                                                                                                                                                                                                                                                                                                             |

### Local development:

In local development you should up kafka broker and set next properties

| Name                                            | Type    | Mandatory | Default value                        | Description                                                                                    |
|-------------------------------------------------|---------|-----------|--------------------------------------|------------------------------------------------------------------------------------------------|
| maas.kafka.local-dev.enabled                    | Boolean | false     | false                                | Enables development mode                                                                       |
| maas.kafka.local-dev.config.bootstrap.servers   | String  | true      |                                      | A list of host/port pairs to use for establishing the initial connection to the Kafka cluster. |
| maas.kafka.local-dev.config.[other-kafka-props] | String  | false     |                                      | Default kafka [connect config](https://kafka.apache.org/documentation.html#connectconfigs)     |
| maas.tenant.default-id                          | String  | false     | 00000000-0000-0000-0000-000000000000 | Default tenant id (Used for tenant topics)                                                     |


### Blue Green 2 adaptation
Blue Green functionality requires to monitor BG state from Consul. This monitoring is done via BlueGreenStatePublisher bean.
This bean provided automatically by dependency:
```xml
<dependency>
    <groupId>org.qubership.cloud</groupId>
    <artifactId>blue-green-state-monitor-spring</artifactId>
</dependency>
```
See documentation of [blue-green-state-monitor-quarkus](https://git.qubership.org/PROD.Platform.Cloud_Core/libs/blue-green-state-monitor-quarkus/-/blob/main/README.md)
for detail how configure BlueGreenStatePublisher.
