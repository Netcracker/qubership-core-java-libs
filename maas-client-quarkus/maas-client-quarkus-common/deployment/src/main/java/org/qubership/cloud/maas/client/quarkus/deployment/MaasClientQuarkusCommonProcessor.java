package org.qubership.cloud.maas.client.quarkus.deployment;

import org.qubership.cloud.maas.client.quarkus.MaaSClientCommonConfiguration;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;

class MaasClientQuarkusCommonProcessor {

    private static final String FEATURE = "maas-client-quarkus-common";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    void build(BuildProducer<AdditionalBeanBuildItem> additionalBean) {
        additionalBean.produce(new AdditionalBeanBuildItem(MaaSClientCommonConfiguration.class));
    }
}
