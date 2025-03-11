package org.qubership.cloud.consul.config.source.deployment;

import org.qubership.cloud.consul.config.source.runtime.ConsulConfigSourceFactoryBuilder;
import org.qubership.cloud.springcloud.config.source.deployment.SpringCloudConfigSourceEnabledBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigBuilderBuildItem;

import java.util.Optional;

class ConsulConfigSourceProcessor {
    private static final String FEATURE = "consul-config-source";

    @BuildStep
    public void feature(BuildProducer<FeatureBuildItem> feature,
                        Optional<SpringCloudConfigSourceEnabledBuildItem> springCloudEnabled) {
        if (springCloudEnabled.isPresent()) {
            throw new SpringCloudWithConsulException();
        }
        feature.produce(new FeatureBuildItem(FEATURE));
    }

    @BuildStep
    void configFactory(BuildProducer<RunTimeConfigBuilderBuildItem> runTimeConfigBuilder) {
        runTimeConfigBuilder.produce(new RunTimeConfigBuilderBuildItem(ConsulConfigSourceFactoryBuilder.class));
    }
}
