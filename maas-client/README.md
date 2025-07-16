[![Maven build](https://github.com/Netcracker/qubership-maas-client/actions/workflows/maven-build.yaml/badge.svg)](https://github.com/Netcracker/qubership-maas-client/actions/workflows/maven-build.yaml)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?metric=coverage&project=Netcracker_qubership-maas-client)](https://sonarcloud.io/summary/overall?id=Netcracker_qubership-maas-client)
[![duplicated_lines_density](https://sonarcloud.io/api/project_badges/measure?metric=duplicated_lines_density&project=Netcracker_qubership-maas-client)](https://sonarcloud.io/summary/overall?id=Netcracker_qubership-maas-client)
[![vulnerabilities](https://sonarcloud.io/api/project_badges/measure?metric=vulnerabilities&project=Netcracker_qubership-maas-client)](https://sonarcloud.io/summary/overall?id=Netcracker_qubership-maas-client)
[![bugs](https://sonarcloud.io/api/project_badges/measure?metric=bugs&project=Netcracker_qubership-maas-client)](https://sonarcloud.io/summary/overall?id=Netcracker_qubership-maas-client)
[![code_smells](https://sonarcloud.io/api/project_badges/measure?metric=code_smells&project=Netcracker_qubership-maas-client)](https://sonarcloud.io/summary/overall?id=Netcracker_qubership-maas-client)

# MaaS Client

<!-- TOC -->
* [MaaS Client](#maas-client)
* [Tiny, pure java client](#tiny-pure-java-client)
  * [Prerequisite](#prerequisite)
  * [Start](#start)
  * [Kafka client usage example](#kafka-client-usage-example)
    * [Context propagation](#context-propagation)
  * [RabbitMQ client usage example](#rabbitmq-client-usage-example)
    * [Context propagation](#context-propagation-)
  * [Migration](#migration)
<!-- TOC -->

Maas client consist of two parts:
* maas-client-core - Tiny, pure java client to MaaS
* rabbit-context-propagation - utility classes for propagation and restoration of B/G version context
* maas-client-quarkus - Core client with beans for Quarkus now located at: https://github.com/Netcracker/qubership-core-quarkus-extensions

# Tiny, pure java client
Thin layer to MaaS with following requirements in mind:
* as simple as it possible
* minimal external dependencies
* stay framework, cloud and platforms agnostic

Important notice to developer: import classes from `api` packages and avoid explicit importing of `impl` packages in your code as much as you can.
Developing MaaS client is rules of thumb in mind were:
* `api` packages will be backward compatible across minor releases and patches 
* `impl` packages may contain changes that can breaks backward compatibility

All changes in API across library releases will be conformed to semantic versioning rules (See https://semver.org/)  

## Prerequisite
Please ensure, that your pod runtime environment variables contains:
* `NAMESPACE` - namespace name in which microservice is deployed  
* `CLOUD_SERVICE_NAME` - name of microservice 

For more information about variables see: [Parameters Provided by Deployer to Application](https://qubership.org/display/CLOUDDEVOPS/Parameters+Provided+by+Deployer+to+Application)

To add missed variables to your pod runtime environment, you need to edit your deployment chart files.  

## Start
First of all we need to create instance of [MaaSAPIClient](https://github.com/Netcracker/qubership-maas-client/blob/main/client/src/main/java/org/qubership/cloud/maas/client/api/MaaSAPIClient.java). 
Default implementation for this interface is [MaaSAPIClientImpl](https://github.com/Netcracker/qubership-maas-client/blob/main/client/src/main/java/org/qubership/cloud/maas/client/impl/MaaSAPIClientImpl.java).
MaaSAPIClientImpl requires single parameter to instantiate - M2M auth token supplier. This token will be used to:
* interact with maas-agent microservice
* subscribe to tenant-manager tenant activation/deactivation events (tenant-topics feature)
* subscribe to control-plane service to watch on B/G version deploy/promote/rollback events

It's simple constructor call with token from cloud core libraries m2m-manager:
```java
MaaSClient client = new MaaSAPIClientImpl(() -> M2MManager.getInstance().getToken().getToken());
```
   

## Kafka client usage example
All MaaS operations for Kafka is collected in [KafkaMaaSClient](https://github.com/Netcracker/qubership-maas-client/blob/main/client/src/main/java/org/qubership/cloud/maas/client/api/kafka/KafkaMaaSClient.java). To obtain *new* instance of MaaS Kafka client just call: 
```java
KafkaMaaSClient kafkaClient = client.getKafkaClient();
```

To avoid explicit dependency to Kafka clients, maas client only provide various info about Kafka topic:
* brokers addresses
* auth mathod
* name of topic
* topic options and configs

MaaS Client doesn't provide methods to create KafkaProducer or KafkaConsumer. So developer can freely choose more suitable version of kafka client library for his needs.
Get or create topic and create KafkaProducer to it: 
```java
// search existing or request for new topic by MaaS, address structure contains all 
// required info to create connection to Kafka broker instances   
var address = kafkaClient.getOrCreateTopic(new Classifier("invoices"), TopicCreateOptions.DEFAULTS);

// transform address to connection properties needed to KafkaProducer/KafkaConsumer instantiation 
var props = address.formatConnectionProperties()
    	.orElseThrow(() -> new IllegalArgumentException("Unable to construct connection properties to Kafka"));

// create KafkaProducer instance 
try(var producer = new KafkaProducer<Integer, String>(props, new IntegerSerializer(), new StringSerializer())) {
	...
}
```

Produce record for tenant topics:  
```java
TopicAddress topicAddress = kafkaClient.getTopic(new Classifier("orders").tenantId(TenantContext.get()))
        .orElseThrow(() -> new RuntimeException("Topic `orders' not found. Configuration or deployment processing error?"));

ProducerRecord<Integer, String> record = new ProducerRecord<>(
        topicAddress.getTopicName(),
        order.getOrderId(),
        mapper.writeValueAsString(wrapped));

// example how to create KafkaProducer look at the previous code example
kafkaProducer.send(record);
```
Full producer example can be found [here](https://git.qubership.org/PROD.Platform.Cloud_Core/maas-demo-order-prep/-/blob/main/service/src/main/java/com/qubership/core/demo/maas/service/OrderService.java)

Consumer for tenant-topics is much complex, because of runtime nature of tenants. Tenant may be created and activated of deactivated in runtime. 
And consumer in microservice have to dynamically subscribe/unsubscribe to topics created to new tenants.

MaaSKafkaClient provide convenient method to manage topic subscriptions on tenants list change. Callback is called at least once on application 
startup to simplify initial microservice subscriptions code.
```java
kafkaClient.watchTenantTopics("orders", topics -> {
            // perform subscribe/unsubscribe to given topics 
        });
```
Full example for consumer can be found [here](https://git.qubership.org/PROD.Platform.Cloud_Core/maas-demo-order-proc/-/blob/main/service/src/main/java/com/qubership/core/demo/maas/service/OrderProcService.java)

### Context propagation
It is crucial to save and restore context during message processing. Moreover, its is manadatory requirements to correctly filtering messages in 
Blue/Green deployment. To serialize current execution context into message headers just call utility method:
```java
import org.qubership.cloud.maas.client.context.kafka.KafkaContextPropagation;

var record = new ProducerRecord<...>(
        topicName,
        partition,
        messageKey,
        messageValue,
        KafkaContextPropagation.propagateContext() // dump context to message headers
   );
```
Context propagation methods is in class [KafkaContextPropagation](kafka-context-propagation/src/main/java/org/qubership/cloud/maas/client/context/kafka/KafkaContextPropagation.java) located in module:
```xml
<dependency>
    <groupId>org.qubership.cloud.maas.client</groupId>
    <artifactId>kafka-context-propagation</artifactId>
</dependency>

```

To restore context from received message into current execution thread use:
```java
ConsumerRecord message = ... 
KafkaContextPropagation.restoreContext(message.headers());
```

## RabbitMQ client usage example
All MaaS operations for RabbitMQ is collected in [RabbitMaaSClient](https://github.com/Netcracker/qubership-maas-client/blob/main/client/src/main/java/org/qubership/cloud/maas/client/api/rabbit/RabbitMaaSClient.java). 
To obtain *new* instance of MaaS RabbitMQ client just call:
```java
RabbitMaaSClient rabbitClient = client.getRabbitClient();
```
Despite of Kafka approach where classifier is pointed to topic, classifier used for rabbit is pointed to VHost entity in RabbitMQ.
So to locate or create VHost you need: 
```java
VHost vhost = rabbitClient.getOrCreateVirtualHost(new Classifier("commands"));
```
`VHost` entity contains exhaustive information about vhost location and credentials needed for connection to RabbitMQ instance.

If you want to just get vhost (without its creation in case it doesn't exist):
```java
VHost vhost = client.getVirtualHost(new Classifier("commands"));
```

Example of usage: [connection factory](https://git.qubership.org/PROD.Platform.Cloud_Core/maas-group/maas-demo-bill-processor/-/blob/main/service/src/main/java/com/qubership/core/demo/maas/Producers.java#L23)

### Context propagation 
In Blue/Green deployments you need to save and restore context information about original request version. Because version exchange 
created for `versionedEntities` rely on `version` message header, you need to save http `X-Version` header value to message headers. Also, you need to 
restore version value to microservice execution context on message receiver side. 

MaaS Client offers utility class to simplify these tasks. Include dependency to:
```xml
<dependency>
    <groupId>org.qubership.cloud.maas.client</groupId>
    <artifactId>rabbit-context-propagation</artifactId>
    <version>{version}</version>
</dependency>
```

To save version context to message you can use:
```java
AMQP.BasicProperties props = new AMQP.BasicProperties();

// save version value to message headers
props = RabbitContextPropagation.propagateContext(props);

channel.basicPublish("my-exchange", routingKey,  props, data);
```
As a good usage example bill-processor microservice from Demo MaaS application: 
[ProcessorResource](https://git.qubership.org/PROD.Platform.Cloud_Core/maas-group/maas-demo-bill-processor/-/blob/main/service/src/main/java/com/qubership/core/demo/maas/ProcessorResource.java#L44)

To restore context from message to consumer thread: 
```java
DeliverCallback deliverCallback = (consumerTag, delivery) -> {
        ....
        // restore b/g version context from message headers 
		RabbitContextPropagation.restoreContext(delivery);
        ...
}
```
Example of consumer from demo microservices: 
[PaymentProcessor](https://git.qubership.org/PROD.Platform.Cloud_Core/maas-group/maas-demo-payment-gateway/-/blob/main/service/src/main/java/com/qubership/core/demo/maas/PaymentProcessor.java#L48)

## Migration
Details [here](docs/migration)

