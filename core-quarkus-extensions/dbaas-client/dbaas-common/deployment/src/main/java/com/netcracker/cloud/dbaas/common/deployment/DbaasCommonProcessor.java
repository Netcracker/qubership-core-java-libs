package com.netcracker.cloud.dbaas.common.deployment;

import com.netcracker.cloud.dbaas.common.config.DbaaSClassifierProducer;
import com.netcracker.cloud.dbaas.common.config.DbaaSMetricsRegistrarProducer;
import com.netcracker.cloud.dbaas.common.config.DbaasClientProducer;
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
