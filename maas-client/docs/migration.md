# Migration from 8.x.x to 9.x.x versions

The main reason of changing major version of library was migration from Blue/Green version1 to new 
concept of Blue/Green version2. More about Blue/Green version2: `https://qubership.org/display/CLOUDCORE/BlueGreen+2.0+design+space`

<!-- TOC -->
* [Migration from 8.x.x to 9.x.x versions](#migration-from-8xx-to-9xx-versions)
  * [Tracking Version Filter](#tracking-version-filter-)
* [Kafka](#kafka)
    * [Kafka consumer group name requirements:](#kafka-consumer-group-name-requirements)
    * [Get/create/watch Kafka topics by classifier:](#getcreatewatch-kafka-topics-by-classifier)
* [RabbitMQ](#rabbitmq)
    * [Blue/Green version 1](#bluegreen-version-1)
    * [Blue/Green version 2](#bluegreen-version-2)
<!-- TOC -->

## Tracking Version Filter 
Tracking Version Filter feature is now depends only on Consul instead of Control-Plane and Paas-Mediation. Interface of 
`TrackingVersionFilter` got rid of mandatory `AutoCloseable` interface. Reference implementation now 
requires instance of `BlueGreenStatePublisher`:
```java
public TrackingVersionFilterImpl(BlueGreenStatePublisher publisher) {
    ...
}
```
You can take `BlueGreenStatePublisher` suitable implementation from: 
```xml
<dependency>
    <groupId>com.netcracker.cloud</groupId>
    <artifactId>blue-green-state-monitor-spring</artifactId>
</dependency>
```
or for Quarkus:
```xml
<dependency>
    <groupId>com.netcracker.cloud</groupId>
    <artifactId>blue-green-state-monitor-quarkus</artifactId>
</dependency>
```

# Kafka
Classes related to Blue/Green have undergone changes due to the alteration of the Blue/Green deployment model concept. 
From regular library consumer perspective there is no big changes to the API. The most important change is dependency 
on `BlueGreeStatePublisher` as a source of Blue/Green state events. `BGKafkaConsumerConfig` builder now requires the 
instance of `Blue/GreenStatePublisher`:
```java
public class BGKafkaConsumerConfig {
    public static Builder builder(Map<String, Object> properties, Set<String> topics, BlueGreenStatePublisher statePublisher) {
    ...
```
### Kafka consumer group name requirements:
Kafka consumer group of non-versioned topic(s) must consist of some logical name, ORIGIN_NAMESPACE and microservice name
Example of consumer group for non-tenant topic(s):
```java
String originNamespace = System.getEnv("ORIGIN_NAMESPACE");
String serviceName = System.getEnv("SERVICE_NAME");
String consumerGroup = String.format("orders.%s.%s", originNamespace, serviceName);
var consumer = new BGKafkaConsumerImpl<K, V>(BGKafkaConsumerConfig.builder(Map.of(ConsumerConfig.GROUP_ID_CONFIG, consumerGroup), topicName, blueGreenStatePublisher)
        .deserializers(keyDeserializer, valueDeserializer)
        .consistencyMode(consistencyMode).build());
```
Kafka consumer group of non-versioned tenant topic(s) must consist of some logical name, ORIGIN_NAMESPACE, microservice name and tenant-id
Example of consumer group for tenant topic(s):
```java
String originNamespace = System.getEnv("ORIGIN_NAMESPACE");
String serviceName = System.getEnv("SERVICE_NAME");
String tenant = getTenant();
String consumerGroup = String.format("orders.%s.%s.%s", originNamespace, serviceName, tenant);
var consumer = new BGKafkaConsumerImpl<K, V>(BGKafkaConsumerConfig.builder(Map.of(ConsumerConfig.GROUP_ID_CONFIG, consumerGroup), topicName, blueGreenStatePublisher)
        .deserializers(keyDeserializer, valueDeserializer)
        .consistencyMode(consistencyMode).build());
```
The naming of final consumer groups (which are created by BGKafkaConsumerImpl internally) has also been changed. New consumer group naming template is:
```
<consumer-group-name>-<bg-version>-<bg-state-of-current-namespace>_<bg-state-of-sibling-namespace>-<date-of-bg-state>
```
where
`consumer-group-name` - consumer group name
`bg-state-of-current-namespace/bg-state-of-sibling-namespace` - is first char from current Blue/Green state names.
`date-of-bg-state` - update time when BG state changed with UTC offset

Example of group names for active and candidate namespaces:
```
orders.origin-namespace.my-microservice-v5-a_c-2023-08-25_13-30-15
orders.origin-namespace.my-microservice-v6-c_a-2023-08-25_13-30-15
```
The new Blue/Green kafka consumer perform migrations from non-bg group id and BG1 implementations to new group id naming 
automatically.

### Get/create/watch Kafka topics by classifier:
Use NAMESPACE env value to retrieve/create/watch Kafka topics via MaaS by classifier.
```java
var classifier = new Classifier("orders").namespace(System.getEnv("NAMESPACE"));
```

# RabbitMQ
Rabbit Blue/Green concept changed a lot from BG1 implementation. In BG1 version, all Blue/Green related changes has been 
performed in the same VHost instance. We've introduced Versioned Exchange that prepends Business Exchanges, versioned 
Queues suffixed by BG version. All this has been gone in new Blue/Green v2 implementation. The main change is that 
no more Versioned Entities in declarative configs. VHost fully cloned during Warmup phase in new VHost.   

Consider following RabbitMQ declarative configuration:

```yaml
apiVersion: nc.maas.rabbit/v2
kind: vhost
spec:
  classifier:
    name: demo-vhost
    namespace: ${ENV_NAMESPACE}
  entities:
    exchanges:
      - name: payments
    queues:
      - name: payments-queue
  versionedEntities:
    exchanges:
      - name: quote-changes
    queues:
      - name: quote-changes-queue
```
Here we see two sections with RabbitMQ structure items: entities  and versionedEntities. Bindings were not 
declared solely for the sake of simplicity of the provided configuration example.
Let's drill it down how this configuration will behave in different Blue/Green version implementations.

### Blue/Green version 1
In this implementation of Blue/Green:

* all mutations performed to implement Blue/Green requirements will be done in the same vhost.
* all items declared in entities section stay intact on any Blue/Green operations
* items declared in versionedEntities mutates according to Blue/Green state.

### Blue/Green version 2
This version of Blue/Green approach have changes:

* during warmup procedure vhost from active side will be cloned to candidate without any modifications, except data in queues.  
* MaaS will fail warmup procedure if it finds declaration of `versionedEntities` in application configuration

So, to enable your configuration for Blue/Green v2 you need to move all your entites from `versionedEntites` section 
to `entities` section. If you need to save versioned queue names, just add `-v1` suffix to migrated to non-versioned
section queue names. For provided example above, migrated configuration will become to:

```yaml
apiVersion: nc.maas.rabbit/v2
kind: vhost
spec:
  classifier:
    name: demo-vhost
    namespace: ${ENV_NAMESPACE}
  entities:
    exchanges:
      - name: payments
      - name: quote-changes
    queues:
      - name: payments-queue
      - name: quote-changes-queue-v1
```

To resolve versioned queue name in Blue/Green v1 context there was `rabbit-blue-green` module. It is no need any more 
in Blue/Green v2, because queue names stay intact during warmup clone procedure.

