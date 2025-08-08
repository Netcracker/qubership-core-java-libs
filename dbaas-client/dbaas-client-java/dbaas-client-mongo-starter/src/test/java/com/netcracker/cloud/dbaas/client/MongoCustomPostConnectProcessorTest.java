package com.netcracker.cloud.dbaas.client;

import org.qubership.cloud.dbaas.client.entity.database.MongoDatabase;
import org.qubership.cloud.dbaas.client.entity.database.type.MongoDBType;
import org.qubership.cloud.dbaas.client.management.DatabaseConfig;
import org.qubership.cloud.dbaas.client.management.DatabasePool;
import org.qubership.cloud.dbaas.client.management.DbaasDbClassifier;
import org.qubership.cloud.dbaas.client.management.PostConnectProcessor;
import org.qubership.cloud.dbaas.client.test.configuration.TestMongoConfiguration;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.annotation.Order;
import org.springframework.test.context.ContextConfiguration;

import java.lang.reflect.Field;
import java.util.List;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;

@SpringBootTest
@ContextConfiguration(classes = {TestMongoConfiguration.class, MongoCustomPostConnectProcessorTest.Configuaration.class})
public class MongoCustomPostConnectProcessorTest {
    @Configuration
    static class Configuaration {

        @Bean
        CustomMongoPostConnectProcessor1 customPostConnectProcessor1() {
            return new CustomMongoPostConnectProcessor1();
        }

        @Bean
        CustomMongoPostConnectProcessor2 customPostConnectProcessor2() {
            return new CustomMongoPostConnectProcessor2();
        }

    }

    private static class CustomMongoPostConnectProcessor implements PostConnectProcessor<MongoDatabase> {
        long invocationTime;

        @Override
        public void process(MongoDatabase database) {
            invocationTime = System.nanoTime();
        }

        @Override
        public Class<MongoDatabase> getSupportedDatabaseType() {
            return MongoDatabase.class;
        }

        public long getInvocationTime() {
            return invocationTime;
        }
    }

    @Order(1)
    private static class CustomMongoPostConnectProcessor1 extends CustomMongoPostConnectProcessor {
    }

    @Order(2)
    private static class CustomMongoPostConnectProcessor2 extends CustomMongoPostConnectProcessor {
    }

    @Autowired
    private DatabasePool databasePool;

    @Autowired
    private List<PostConnectProcessor<?>> postConnectProcessors;
    private List<PostConnectProcessor<?>> spiedPostConnectProcessors;

    @BeforeEach
    public void init() throws Exception {
        spiedPostConnectProcessors = postConnectProcessors.stream()
                .map(postConnectProcessor -> Mockito.spy(postConnectProcessor))
                .collect(Collectors.toList());
        Field postProcessorsField = databasePool.getClass().getDeclaredField("postProcessors");
        postProcessorsField.setAccessible(true);
        postProcessorsField.set(databasePool, spiedPostConnectProcessors);
    }

    @Test
    public void testAllPostprocessors() {
        this.databasePool.getOrCreateDatabase(MongoDBType.INSTANCE, new DbaasDbClassifier.Builder().build(), DatabaseConfig.builder().build());
        List<CustomMongoPostConnectProcessor> customPostProcessors = spiedPostConnectProcessors.stream()
                .filter(postConnectProcessor -> postConnectProcessor instanceof CustomMongoPostConnectProcessor)
                .map(postConnectProcessor -> (CustomMongoPostConnectProcessor) postConnectProcessor)
                .sorted(AnnotationAwareOrderComparator.INSTANCE)
                .collect(Collectors.toList());
        long prevPostProcessorInvocationTime = -1;
        for (CustomMongoPostConnectProcessor postConnectProcessor : customPostProcessors) {
            Mockito.verify(postConnectProcessor, Mockito.times(1)).process(any(MongoDatabase.class));
            long invocationTime = postConnectProcessor.getInvocationTime();
            MatcherAssert.assertThat(invocationTime, Matchers.greaterThan(prevPostProcessorInvocationTime));
            prevPostProcessorInvocationTime = invocationTime;
        }
    }
}
