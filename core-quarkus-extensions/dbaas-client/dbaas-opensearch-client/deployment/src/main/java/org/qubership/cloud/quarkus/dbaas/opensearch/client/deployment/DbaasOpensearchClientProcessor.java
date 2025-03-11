package org.qubership.cloud.quarkus.dbaas.opensearch.client.deployment;

import org.qubership.cloud.quarkus.dbaas.opensearch.client.DbaasOpensearchConfiguration;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;

class DbaasOpensearchClientProcessor {

    private static final String FEATURE = "dbaas-opensearch-client";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    AdditionalBeanBuildItem registerAdditionalBeans() {
        return new AdditionalBeanBuildItem.Builder()
                .setUnremovable()
                .addBeanClass(DbaasOpensearchConfiguration.class)
                .build();
    }
}
