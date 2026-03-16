package com.netcracker.cloud.maas.client.quarkus.deployment;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class MaasClientQuarkusKafkaProcessorTest {

    private static final String EXPECTED_FEATURE_NAME = "maas-client-quarkus-kafka";

    private MaasClientQuarkusKafkaProcessor processor;

    @Mock
    private BuildProducer<AdditionalBeanBuildItem> additionalBeanProducer;

    @BeforeEach
    void setUp() {
        processor = new MaasClientQuarkusKafkaProcessor();
    }

    @Test
    void feature_ShouldReturnCorrectFeatureName() {
        // When
        FeatureBuildItem result = processor.feature();

        // Then
        assertNotNull(result);
        assertEquals(EXPECTED_FEATURE_NAME, result.getName());
    }

    @Test
    void build_ShouldProduceAdditionalBeanBuildItem() {
        // When
        processor.build(additionalBeanProducer);

        // Then
        verify(additionalBeanProducer).produce(any(AdditionalBeanBuildItem.class));
        verifyNoMoreInteractions(additionalBeanProducer);
    }
}
