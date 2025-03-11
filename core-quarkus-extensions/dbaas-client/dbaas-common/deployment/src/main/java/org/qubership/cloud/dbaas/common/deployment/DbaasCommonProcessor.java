package org.qubership.cloud.dbaas.common.deployment;

import org.qubership.cloud.dbaas.common.config.DbaaSClassifierProducer;
import org.qubership.cloud.dbaas.common.config.DbaaSMetricsRegistrarProducer;
import org.qubership.cloud.dbaas.common.config.DbaasClientProducer;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;

public class DbaasCommonProcessor {
    private static final String FEATURE = "dbaas-common";

    @BuildStep
    public FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }


    @BuildStep
    AdditionalBeanBuildItem registerAdditionalBeans() {
        return new AdditionalBeanBuildItem.Builder()
                .setUnremovable()
                .addBeanClass(DbaasClientProducer.class)
                .addBeanClass(DbaaSClassifierProducer.class)
                .addBeanClass(DbaaSMetricsRegistrarProducer.class)
                .build();
    }

}
