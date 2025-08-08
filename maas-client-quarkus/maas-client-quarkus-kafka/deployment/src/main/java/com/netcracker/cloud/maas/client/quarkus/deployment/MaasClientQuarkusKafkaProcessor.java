package org.qubership.cloud.maas.client.quarkus.deployment;

import org.qubership.cloud.maas.client.quarkus.MaaSClientKafkaConfiguration;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;

class MaasClientQuarkusKafkaProcessor {

    private static final String FEATURE = "maas-client-quarkus-kafka";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    void build(BuildProducer<AdditionalBeanBuildItem> additionalBean) {
        additionalBean.produce(new AdditionalBeanBuildItem(MaaSClientKafkaConfiguration.class));
    }
}
