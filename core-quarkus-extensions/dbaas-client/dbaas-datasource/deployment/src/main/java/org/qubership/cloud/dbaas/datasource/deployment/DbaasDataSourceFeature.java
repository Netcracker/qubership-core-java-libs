package org.qubership.cloud.dbaas.datasource.deployment;

import org.qubership.cloud.core.quarkus.dbaas.datasource.DataSourceAggregator;
import org.qubership.cloud.core.quarkus.dbaas.datasource.config.CoreQuarkusDataSourceProducer;
import org.qubership.cloud.core.quarkus.dbaas.datasource.config.DataSourceConfiguration;
import org.qubership.cloud.core.quarkus.dbaas.datasource.metrics.PostgresMicrometerMetricsProvider;
import org.qubership.cloud.core.quarkus.dbaas.datasource.service.impl.MigrationServiceImpl;
import io.quarkus.agroal.spi.JdbcDataSourceBuildItem;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.micrometer.deployment.MicrometerProcessor;

import java.util.List;

public class DbaasDataSourceFeature {

    static final String FEATURE = "dbaas-datasource-postgresql";

    @BuildStep
    public FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }


    @BuildStep
    AdditionalBeanBuildItem registerAdditionalBeans() {
        return new AdditionalBeanBuildItem.Builder()
                .setUnremovable()
                .addBeanClass(DataSourceAggregator.class)
                .addBeanClass(MigrationServiceImpl.class)
                .addBeanClass(DataSourceConfiguration.class)
                .build();
    }

    @BuildStep
    void generateCustomQuarkusDataSourceProducerBean(List<JdbcDataSourceBuildItem> jdbcDataSourceBuildItems,
                                                     BuildProducer<AdditionalBeanBuildItem> additionalBeans) {

        if (jdbcDataSourceBuildItems.isEmpty()) {
            // No datasource has been configured so bail out
            return;
        }
        additionalBeans.produce(AdditionalBeanBuildItem.builder()
                .addBeanClasses(CoreQuarkusDataSourceProducer.class)
                .setDefaultScope(DotNames.SINGLETON)
                .setUnremovable()
                .build());
    }

    @BuildStep(onlyIf = MicrometerProcessor.MicrometerEnabled.class)
    AdditionalBeanBuildItem registerMetricsProvider() {
        return new AdditionalBeanBuildItem.Builder()
                .setUnremovable()
                .addBeanClass(PostgresMicrometerMetricsProvider.class)
                .build();
    }
}
