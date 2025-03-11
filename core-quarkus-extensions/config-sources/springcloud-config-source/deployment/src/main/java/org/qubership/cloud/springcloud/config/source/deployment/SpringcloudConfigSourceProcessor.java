package org.qubership.cloud.springcloud.config.source.deployment;

import org.qubership.cloud.springcloud.config.source.PropertyManager;
import org.qubership.cloud.springcloud.config.source.SpringCloudConfigSourceBuilder;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigBuilderBuildItem;

class SpringcloudConfigSourceProcessor {

    private static final String FEATURE = "springcloud-config-source";

    @BuildStep
    FeatureBuildItem build() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    void setUpConfigFile(BuildProducer<RunTimeConfigBuilderBuildItem> configSourceConsumer,
                         BuildProducer<SpringCloudConfigSourceEnabledBuildItem> springCloudEnabled) {
        configSourceConsumer.produce(new RunTimeConfigBuilderBuildItem(
                SpringCloudConfigSourceBuilder.class.getName()));

        springCloudEnabled.produce(new SpringCloudConfigSourceEnabledBuildItem());
    }

    @BuildStep
    AdditionalBeanBuildItem registerAdditionalBeans() {
        return new AdditionalBeanBuildItem.Builder()
                .setUnremovable()
                .addBeanClass(PropertyManager.class)
                .build();
    }
}
