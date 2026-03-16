## Kafka streams adapter for MaaS
This library provide ability to configure KafkaStreams client to create internal topics via MaaS client

<!-- TOC -->
  * [Kafka streams adapter for MaaS](#kafka-streams-adapter-for-maas)
    * [Important! 'application.id' property](#important-applicationid-property)
    * [Default configuration](#default-configuration)
    * [Custom classifier configuration](#custom-classifier-configuration)
    * [Make MaaS to merge existing topics](#make-maas-to-merge-existing-topics)
<!-- TOC -->

### Important! 'application.id' property
When KafkaStreams is used with MaaS it's important to specify valid 'application.id' property.
Recommended to extend 'application.id' according to the following template 
1. For tenant related topics use 
~~~ properties
application.id=maas.{{application}}.{{namespace}}.{{tenantId}}
~~~
2. For other cases use
~~~ properties
application.id=maas.{{application}}.{{namespace}}
~~~

For example:
~~~ properties
application.id=maas.my-app-streams.my-namespace.123e4567-e89b-12d3-a456-426614174000
~~~
or simply
~~~ properties
application.id=maas.my-app-streams.my-namespace
~~~

### Default configuration

~~~ java 
import com.netcracker.cloud.maas.client.api.kafka.KafkaMaaSClient;
import com.netcracker.cloud.maas.client.context.kafka.MaaSKafkaAdminWrapper;
import com.netcracker.cloud.maas.client.context.kafka.MaaSKafkaClientSupplier;
import org.apache.kafka.clients.admin.ForwardingAdmin;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;

import java.util.Map;
import java.util.Properties;
import java.util.function.Function;

public class KafkaStreamsExample {

    KafkaStreams kafkaStreams(StreamsBuilder builder, Properties kafkaStreamProps, KafkaMaaSClient kafkaMaaSClient) {
        Function<Map<String, Object>, ForwardingAdmin> adminSupplier = config -> MaaSKafkaAdminWrapper.builder(config, kafkaMaaSClient).build();
        MaaSKafkaClientSupplier clientSupplier = new MaaSKafkaClientSupplier(adminSupplier);
        return new KafkaStreams(builder.build(), kafkaStreamProps, clientSupplier);
    }
}
~~~

### Custom classifier configuration

~~~ java 
import com.netcracker.cloud.maas.client.api.Classifier;
import com.netcracker.cloud.maas.client.api.kafka.KafkaMaaSClient;
import com.netcracker.cloud.maas.client.context.kafka.MaaSKafkaAdminWrapper;
import com.netcracker.cloud.maas.client.context.kafka.MaaSKafkaClientSupplier;
import org.apache.kafka.clients.admin.ForwardingAdmin;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;

import java.util.Map;
import java.util.Properties;
import java.util.function.Function;

public class KafkaStreamsExample {

    KafkaStreams tenantKafkaStreams(StreamsBuilder builder, Properties kafkaStreamProps, KafkaMaaSClient kafkaMaaSClient, String tenantId) {
        Function<Map<String, Object>, ForwardingAdmin> adminSupplier = config -> MaaSKafkaAdminWrapper.builder(config, kafkaMaaSClient)
                .classifierSupplier(topicName -> new Classifier(topicName, Classifier.TENANT_ID, tenantId))
                .build();
        MaaSKafkaClientSupplier clientSupplier = new MaaSKafkaClientSupplier(adminSupplier);
        return new KafkaStreams(builder.build(), kafkaStreamProps, clientSupplier);
    }
~~~

### Make MaaS to merge existing topics
In case if topics already exists in Kafka but not registered in MaaS yet, override onTopicExistsAction as in the example below.
This will make MaaS to register existing Kafka topic. So the topic will be become manageable by MaaS.

~~~ java 
import com.netcracker.cloud.maas.client.api.kafka.KafkaMaaSClient;
import com.netcracker.cloud.maas.client.api.kafka.protocolextractors.OnTopicExists;
import com.netcracker.cloud.maas.client.context.kafka.MaaSKafkaAdminWrapper;
import com.netcracker.cloud.maas.client.context.kafka.MaaSKafkaClientSupplier;
import org.apache.kafka.clients.admin.ForwardingAdmin;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;

import java.util.Map;
import java.util.Properties;
import java.util.function.Function;

public class KafkaStreamsExample {

    KafkaStreams tenantKafkaStreams(StreamsBuilder builder, Properties kafkaStreamProps, KafkaMaaSClient kafkaMaaSClient, String tenantId) {
        Function<Map<String, Object>, ForwardingAdmin> adminSupplier = config -> MaaSKafkaAdminWrapper.builder(config, kafkaMaaSClient)
                .onTopicExistsAction(OnTopicExists.MERGE)
                .build();
        MaaSKafkaClientSupplier clientSupplier = new MaaSKafkaClientSupplier(adminSupplier);
        return new KafkaStreams(builder.build(), kafkaStreamProps, clientSupplier);
    }
}
~~~
