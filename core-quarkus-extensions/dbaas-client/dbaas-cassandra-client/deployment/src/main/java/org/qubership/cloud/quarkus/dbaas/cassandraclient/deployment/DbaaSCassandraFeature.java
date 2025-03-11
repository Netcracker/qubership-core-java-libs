package org.qubership.cloud.quarkus.dbaas.cassandraclient.deployment;

import org.qubership.cloud.quarkus.dbaas.cassandraclient.config.CassandraClientConfiguration;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;

public class DbaaSCassandraFeature {
    private static final String FEATURE = "dbaas-cassandra-client";

    @BuildStep
    public FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    AdditionalBeanBuildItem registerAdditionalBeans() {
        return new AdditionalBeanBuildItem.Builder()
                .setUnremovable()
                .addBeanClass(CassandraClientConfiguration.class)
                .build();
    }
}
