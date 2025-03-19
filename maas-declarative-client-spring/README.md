[![Coverage](https://sonarcloud.io/api/project_badges/measure?metric=coverage&project=Netcracker_qubership-maas-declarative-client-spring)](https://sonarcloud.io/summary/overall?id=Netcracker_qubership-maas-declarative-client-spring)
[![duplicated_lines_density](https://sonarcloud.io/api/project_badges/measure?metric=duplicated_lines_density&project=Netcracker_qubership-maas-declarative-client-spring)](https://sonarcloud.io/summary/overall?id=Netcracker_qubership-maas-declarative-client-spring)
[![vulnerabilities](https://sonarcloud.io/api/project_badges/measure?metric=vulnerabilities&project=Netcracker_qubership-maas-declarative-client-spring)](https://sonarcloud.io/summary/overall?id=Netcracker_qubership-maas-declarative-client-spring)
[![bugs](https://sonarcloud.io/api/project_badges/measure?metric=bugs&project=Netcracker_qubership-maas-declarative-client-spring)](https://sonarcloud.io/summary/overall?id=Netcracker_qubership-maas-declarative-client-spring)
[![code_smells](https://sonarcloud.io/api/project_badges/measure?metric=code_smells&project=Netcracker_qubership-maas-declarative-client-spring)](https://sonarcloud.io/summary/overall?id=Netcracker_qubership-maas-declarative-client-spring)

# Maas Spring Kafka

This module adds ability to work with Kafka via declarative approach or directly using MaaS API.

### How start work with Maas Spring Kafka Client:

#### 1. Add dependency on maas-kafka-spring-client

```xml

<dependency>
    <groupId>org.qubership.cloud.maas.declarative</groupId>
    <artifactId>maas-declarative-kafka-client-spring</artifactId>
</dependency>
```

#### 2. Configure kafka clients

1. Add config properties to application.yml (All the config props described [below](#common-client-config-properties))

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
            managedby: maas or self
            namespace: namespace_name
            on-topic-exists: merge or fail # Optional
          is-tenant: true or false
          pool-duration: 1000 # Overrides common pool duration
          instance-count: 2 # used for concurrent handling
          dedicated-thread-pool-size: 5
          group:
            id: your_group_id
          # default kafka client properties
          kafka-consumer:
            property:
              key:
                deserializer: org.apache.kafka.common.serialization.StringDeserializer
              value:
                deserializer: org.apache.kafka.common.serialization.StringDeserializer
              ...
      producer:
        your_producer_name:
          topic:
            actual-name: "actual_kafka_topic_name" # Optional
            name: your_topic_name
            template: "maas_template" # Optional
            managedby: maas or self
            namespace: namespace_name
            on-topic-exists: merge or fail # Optional
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

@Configuration
public class SomeConfigClass {

    @Autowired
    MaasKafkaClientFactory clientFactory;

    @Bean
    MaasKafkaProducer maasKafkaProducer() {
        // Get producer according to config described above
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

    @Bean
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

> If you fill serializers/deserializers in application.yml and still would like to use beans,
> in this case beans will be used instead (You can fill only value serializer/deserializer as bean and key
> serializer/deserializer
> set in application.yml or vice versa)

```java


@Configuration
public class SomeConfigClass {

    @Autowired
    MaasKafkaClientFactory clientFactory;

    @Autowired
    @Qualifier(value = "keyDeserializer")
    Deserializer<String> keyDeserializer;

    @Autowired
    @Qualifier(value = "valueDeserializer")
    Deserializer<String> valueDeserializer;

    @Autowired
    @Qualifier(value = "keySerializer")
    Serializer<String> keySerializer;

    @Autowired
    @Qualifier(value = "valueSerializer")
    Serializer<String> valueSerializer;

    @Bean
    MaasKafkaProducer maasKafkaProducer() {
        // Get producer according to config described above
        MaasKafkaProducerCreationRequest producerCreationRequest =
                MaasKafkaProducerCreationRequest.builder()
                        .setProducerDefinition(clientFactory.getProducerDefinition("your_producer_name"))
                        .setValueSerializer(valueSerializer)
                        .setKeySerializer(keySerializer)
                        .build();

        KafkaProducer producer = clientFactory.createProducer(producerCreationRequest);

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

    @Bean
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
   ACTIVE,
   if this method will be called before client has been initialized, it will be waited until client became INITIALIZED (
   more info described in javadocs)

```java
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

@Configuration
public class SomeConfig {

    @Autowired
    ClientStateManagerService clientStateManagerService;

    @EventListener(ApplicationReadyEvent.class)
    public void activateKafkaClients() {
        clientStateManagerService.activateInitializedClients(null);
    }

}
```

### Note

> Approach above fits only if client is produced as MaasKafkaClient instance bean.
> Not supported collecting from Map or Collection of MaasKafkaClient instances.

### Note

> Also, you can initialize and activate client out of configuration

### Note

> Also, you can create consumer with setting 'CustomProcessed' = true. If you create the client this way, both the event
> management process
> and multitenancy will be disabled

#### 3.2 Runtime configuration

Instead, step 1 from 3.1 there is a way to configure clients in runtime only

### Note

All contracts(Mandatory/Optional property) described for [application.yaml](#common-client-config-properties) config
implemented in builders

1. Create client definition using builders

```java

@Configuration
public class SomeConfigClass {

    @Autowired
    MaasKafkaClientFactory clientFactory;

    // Create kafka producer
    @Bean
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
    @Bean
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
import org.springframework.beans.factory.annotation.Autowired;

public class SomeClass {

    @Autowired
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
| maas.opentracing.kafka.enabled       | Boolean | false     | true                   | Enables opentracing in kafka                                 |
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
| maas.kafka.client.producer.[your-producer-name].topic.template                                        | String  | false     |               | The template of maas topic                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     |
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
| maas.kafka.client.consumer.[your-consumer-name].topic.template                                        | String  | false     |               | The template of maas topic                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     |
| maas.kafka.client.consumer.[your-consumer-name].topic.namespace                                       | String  | true      |               | Service namespace                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              |
| maas.kafka.client.consumer.[your-consumer-name].topic.managedby                                       | String  | true      |               | Used to indicate that topic should be crated by microservice in case of self. There are situations when we need to dynamically create topic and maas can't deal with it. For example ${quarkus.application.namespace}-${cpq.agm-core-integration.kafka.agreement-entity-type}_dbCleaningNotification-{tenant-id}. In this case we will create topic from microservice using MaaS API.Available values: maas/self Additional params like replication, partitions should be added in case of self topic creation |
| maas.kafka.client.consumer.[your-consumer-name].topic.configs.[nay-kafka-topic-property]              | String  | true      |               | Any kafka topic [property](https://kafka.apache.org/documentation.html#topicconfigs) (Usen only if managedby is "self")                                                                                                                                                                                                                                                                                                                                                                                        |
| maas.kafka.client.consumer.[your-consumer-name].is-tenant                                             | String  | true      |               | Tells consumer that all topic used bgy them are tenants                                                                                                                                                                                                                                                                                                                                                                                                                                                        |
| maas.kafka.client.consumer.[your-consumer-name].key.deserializer                                      | String  | false     |               | Deserializer class for key that implements the org.apache.kafka.common.serialization.Deserializer interface                                                                                                                                                                                                                                                                                                                                                                                                    |
| maas.kafka.client.consumer.[your-consumer-name].value.deserializer                                    | String  | false     |               | Deserializer class for value that implements the org.apache.kafka.common.serialization.Deserializer interface                                                                                                                                                                                                                                                                                                                                                                                                  |
| maas.kafka.client.consumer.[your-consumer-name].group.id                                              | String  | true      |               | A unique string that identifies the consumer group this consumer belongs to. Can contain **{{tenantId}}** placeholder                                                                                                                                                                                                                                                                                                                                                                                          |
| maas.kafka.client.consumer.[your-consumer-name].kafka-consumer.property.[any-kafka-consumer-property] | String  | false     |               | Any kafka consumer [property](http://kafka.apache.org/documentation.html#consumerconfigs)                                                                                                                                                                                                                                                                                                                                                                                                                      |
| maas.kafka.client.consumer.[your-consumer-name].dedicated-thread-pool-size                            | Integer | false     |               | If defined - a separate thread pool will be created with defined number of threads                                                                                                                                                                                                                                                                                                                                                                                                                             |

### Local development:

In local development all cloud dependent implementations are replaced.
However, if you want to use your custom or cloud implementations you can do what described below:

#### MaaS dependent implementations

You can enable MaaS implementations in development/test profile via setting maas.kafka.maas.enabled = true
and maas.agent.url = <url to your local MaaS instance>

#### Tenant manager dependent implementations

You can enable local working with Tenant manager by providing bean of type `TenantServiceProvider`
with order = 0 (max priority)

#### Credential extractor

You can provide your own credentials for topics by providing bean of class
`CredExtractorProvider` with order = 0 (max priority)

### Multitenancy

Multitenancy is supported by setting is-tenant=true in client definition. If at least one client is multitenant
you should not set `maas.tenant.default-id` to any value in your application. If after service start you see that
lib can't find topic, check that log about default tenant id looks like

```
[class=c.n.cloud.maas.kafka.tenant.impl.ActiveTenantInfoServiceImpl] [method=getActiveTenantIds] Default tenant id: 
```

### Blue Green 2 adaptation

Blue Green functionality requires to monitor BG state from Consul. This monitoring is done via BlueGreenStatePublisher
bean.
This bean provided automatically by dependency:

```xml

<dependency>
    <groupId>org.qubership.cloud</groupId>
    <artifactId>blue-green-state-monitor-spring</artifactId>
</dependency>
```

See documentation
of blue-green-state-monitor-spring
for detail how configure BlueGreenStatePublisher.
