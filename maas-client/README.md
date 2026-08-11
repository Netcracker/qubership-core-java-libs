[![Maven build](https://github.com/Netcracker/qubership-maas-client/actions/workflows/maven-deploy.yml/badge.svg)](https://github.com/Netcracker/qubership-maas-client/actions/workflows/maven-deploy.yml)
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

To add missed variables to your pod runtime environment, you need to edit your deployment chart files.  

## Start
First of all we need to create instance of [MaaSAPIClient](https://github.com/Netcracker/qubership-maas-client/blob/main/client/src/main/java/com/netcracker/cloud/maas/client/api/MaaSAPIClient.java). 
Default implementation for this interface is [MaaSAPIClientImpl](https://github.com/Netcracker/qubership-maas-client/blob/main/client/src/main/java/com/netcracker/cloud/maas/client/impl/MaaSAPIClientImpl.java).
MaaSAPIClientImpl requires single parameter to instantiate - M2M auth token supplier. This token will be used to:
* interact with maas-agent microservice
* subscribe to tenant-manager tenant activation/deactivation events (tenant-topics feature)
* subscribe to control-plane service to watch on B/G version deploy/promote/rollback events

It's simple constructor call with token from cloud core libraries m2m-manager:
```java
MaaSClient client = new MaaSAPIClientImpl(() -> M2MManager.getInstance().getToken().getToken(), false);
```
   

## Retry behaviour and configuration

Every call to maas-agent is retried before giving up, bounded by a single
setting: the maximum total duration of the call.

| Property | Default | Meaning |
|---|---|---|
| `maas.http.timeout` | `30` (seconds) | connect/read/write timeout of a **single** attempt |
| `maas.http.retry.max-total-duration-ms` | `60000` | how long one call may take in **total**, retries included. `0` disables retries |

`max-total-duration-ms` is the only retry knob: the attempt count and the pauses
between attempts are derived from it. The first pause is 1s, each next one
doubles, and the cap is a quarter of the total — with the default 60s that gives
1s, 2s, 4s, 8s, 15s, 15s, roughly six attempts when each attempt fails fast. If
attempts hang instead, fewer of them fit into the same duration. Backoff carries
+/-20% jitter so concurrent callers do not retry in lockstep.

Each attempt is additionally bounded by what is left of the total duration, so
the worst case a caller sees is that total duration itself rather than the total
duration plus one `maas.http.timeout`.

The 60s default is meant to outlast a database leader switchover while still
failing fast enough to react to a real outage.

The watch endpoint (`watch-create`) is excluded: it is a long poll with its own
loop and its own backoff. Its window is derived from `maas.http.timeout` and stays
below it — maas-service holds the request open for the whole window and then answers
with an empty list, which the client has to be able to receive.

Which responses are retried:

| Response | Retried | Why |
|---|---|---|
| `IOException` | yes | connection refused/reset while the agent is being rescheduled |
| 5xx | yes | includes the `500` maas-agent returns when it cannot reach maas-service at all |
| 429 | yes | throttling |
| **405** | **only with a maas-service error body** | maas-service maps PostgreSQL error `25006` (READ ONLY SQL TRANSACTION) to `405`, so a write against a demoted Patroni node during a switchover arrives as `405`, not as `5xx`. A plain `405` — a route removed on the server, an ingress rejecting the method — is permanent and fails fast |
| **401** | **once** | covers a token that expired in flight. Further attempts re-send the same token, since the supplier cannot be told it was rejected |
| other 4xx | no | permanent client errors, failed on the first attempt |

The two 4xx entries are deliberate: the usual "retry 5xx, fail fast on 4xx" rule
does not survive a database leader switchover here.

## Kafka client usage example
All MaaS operations for Kafka is collected in [KafkaMaaSClient](https://github.com/Netcracker/qubership-maas-client/blob/main/client/src/main/java/com/netcracker/cloud/maas/client/api/kafka/KafkaMaaSClient.java). To obtain *new* instance of MaaS Kafka client just call: 
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

Consumer for tenant-topics is much complex, because of runtime nature of tenants. Tenant may be created and activated of deactivated in runtime. 
And consumer in microservice have to dynamically subscribe/unsubscribe to topics created to new tenants.

MaaSKafkaClient provide convenient method to manage topic subscriptions on tenants list change. Callback is called at least once on application 
startup to simplify initial microservice subscriptions code.
```java
kafkaClient.watchTenantTopics("orders", topics -> {
            // perform subscribe/unsubscribe to given topics 
        });
```

### Context propagation
It is crucial to save and restore context during message processing. Moreover, its is manadatory requirements to correctly filtering messages in 
Blue/Green deployment. To serialize current execution context into message headers just call utility method:
```java
import com.netcracker.cloud.maas.client.context.kafka.KafkaContextPropagation;

var record = new ProducerRecord<...>(
        topicName,
        partition,
        messageKey,
        messageValue,
        KafkaContextPropagation.propagateContext() // dump context to message headers
   );
```
Context propagation methods is in class [KafkaContextPropagation](kafka-context-propagation/src/main/java/com/netcracker/cloud/maas/client/context/kafka/KafkaContextPropagation.java) located in module:
```xml
<dependency>
    <groupId>com.netcracker.cloud.maas.client</groupId>
    <artifactId>kafka-context-propagation</artifactId>
</dependency>

```

To restore context from received message into current execution thread use:
```java
ConsumerRecord message = ... 
KafkaContextPropagation.restoreContext(message.headers());
```

## RabbitMQ client usage example
All MaaS operations for RabbitMQ is collected in [RabbitMaaSClient](https://github.com/Netcracker/qubership-maas-client/blob/main/client/src/main/java/com/netcracker/cloud/maas/client/api/rabbit/RabbitMaaSClient.java). 
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

### Context propagation 
In Blue/Green deployments you need to save and restore context information about original request version. Because version exchange 
created for `versionedEntities` rely on `version` message header, you need to save http `X-Version` header value to message headers. Also, you need to 
restore version value to microservice execution context on message receiver side. 

MaaS Client offers utility class to simplify these tasks. Include dependency to:
```xml
<dependency>
    <groupId>com.netcracker.cloud.maas.client</groupId>
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

To restore context from message to consumer thread: 
```java
DeliverCallback deliverCallback = (consumerTag, delivery) -> {
        ....
        // restore b/g version context from message headers 
		RabbitContextPropagation.restoreContext(delivery);
        ...
}
```

## Migration
Details [here](/docs/migration)

