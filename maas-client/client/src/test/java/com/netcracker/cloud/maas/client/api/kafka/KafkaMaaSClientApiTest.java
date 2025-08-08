package com.netcracker.cloud.maas.client.api.kafka;

import com.netcracker.cloud.maas.client.api.Classifier;
import com.netcracker.cloud.maas.client.impl.dto.kafka.v1.TopicTemplate;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

// test 6.0.0 API stability
public class KafkaMaaSClientApiTest {

    // consider somewhere in client libraries we have class implementing our interface
    //
    // test performs by compiler itself, because api breaking changes will failure compilation of this subclass
    class CustomKafkaMaaSClientApi implements KafkaMaaSClient {
        @Override
        public TopicAddress getOrCreateTopic(Classifier classifier, TopicCreateOptions options) {
            return null;
        }

        @Override
        public Optional<TopicAddress> getTopic(Classifier classifier) {
            return Optional.empty();
        }

        @Override
        public void watchTenantTopics(String name, Consumer<List<TopicAddress>> callback) {
        }

        @Override
        public void watchTopicCreate(String name, Consumer<TopicAddress> callback) {
        }

        @Override
        public TopicAddress getOrCreateLazyTopic(String name) {
            return null;
        }

        @Override
        public TopicAddress getOrCreateLazyTopic(String name, String tenantId) {
            return null;
        }

        @Override
        public TopicTemplate deleteTopicTemplate(String name) {
            return null;
        }
    }
}
