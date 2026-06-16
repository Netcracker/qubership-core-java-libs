package com.netcracker.cloud.podsecrets.quarkus.deployment;

import com.netcracker.cloud.podsecrets.quarkus.runtime.PodSecretsConfigSourceFactoryBuilder;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigBuilderBuildItem;

class PodSecretsConfigSourceProcessor {

    private static final String FEATURE = "pod-secrets-config-source";

    @BuildStep
    void feature(BuildProducer<FeatureBuildItem> feature) {
        feature.produce(new FeatureBuildItem(FEATURE));
    }

    @BuildStep
    void configFactory(BuildProducer<RunTimeConfigBuilderBuildItem> runTimeConfigBuilder) {
        runTimeConfigBuilder.produce(new RunTimeConfigBuilderBuildItem(PodSecretsConfigSourceFactoryBuilder.class));
    }
}
