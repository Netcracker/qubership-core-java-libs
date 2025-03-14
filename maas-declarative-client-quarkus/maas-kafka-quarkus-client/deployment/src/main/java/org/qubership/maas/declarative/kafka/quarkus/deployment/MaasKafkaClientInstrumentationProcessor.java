package org.qubership.maas.declarative.kafka.quarkus.deployment;

import org.qubership.maas.declarative.kafka.client.impl.common.constant.MaasKafkaCommonConstants;
import org.qubership.maas.declarative.kafka.quarkus.client.config.*;
import org.qubership.maas.declarative.kafka.quarkus.client.config.local.dev.MaasKafkaLocalDevConfig;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import org.apache.commons.lang3.BooleanUtils;
import org.eclipse.microprofile.config.ConfigProvider;


public class MaasKafkaClientInstrumentationProcessor {
    private static final String FEATURE = "maas-declarative-kafka";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    AdditionalBeanBuildItem registerKafkaBeans() {
        AdditionalBeanBuildItem.Builder builder = AdditionalBeanBuildItem.builder();

        builder
                .addBeanClass(MaasKafkaCommonConfig.class)
                .addBeanClass(MaasKafkaClientFactoryConfig.class)
                .addBeanClass(MaasKafkaClientStateManagerConfig.class)
                .addBeanClass(MaasKafkaTopicServiceConfig.class)
                .addBeanClass(MaasKafkaProps.class);

        boolean isLocalDevEnabled = ConfigProvider.getConfig().getOptionalValue(MaasKafkaCommonConstants.KAFKA_LOCAL_DEV_ENABLED, Boolean.class)
                .map(BooleanUtils::isTrue)
                .orElse(false);

        if (isLocalDevEnabled) {
            builder.addBeanClass(MaasKafkaLocalDevConfig.class);
        } else {
            // to prevent useless log in localdev
            builder.addBeanClass(MaasKafkaProdClientConfig.class);
        }

        return builder
                .setUnremovable()
                .build();
    }
}
