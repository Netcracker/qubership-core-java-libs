package org.qubership.cloud.core.quarkus.dbaas.datasource.config;

import org.qubership.cloud.core.quarkus.dbaas.datasource.DbaaSDataSource;
import io.agroal.api.AgroalDataSource;
import io.quarkus.agroal.runtime.*;
import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.datasource.common.runtime.DatabaseKind;
import io.quarkus.datasource.runtime.DataSourceSupport;
import io.quarkus.datasource.runtime.DataSourcesBuildTimeConfig;
import io.quarkus.datasource.runtime.DataSourcesRuntimeConfig;
import io.quarkus.narayana.jta.runtime.TransactionManagerConfiguration;
import jakarta.annotation.Priority;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Named;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.TransactionSynchronizationRegistry;

import org.jboss.tm.XAResourceRecoveryRegistry;

import java.lang.annotation.Annotation;

@Alternative
@Priority(1)
public class CoreQuarkusDataSourceProducer extends DataSources {

    private static final String DEFAULT_DATASOURCE_NAME = "<default>";

    private final AgroalDataSourceSupport agroalDataSourceSupport;
    private final AgroalDataSource dbaasDataSourceAggregator;

    public CoreQuarkusDataSourceProducer(DataSourcesBuildTimeConfig dataSourcesBuildTimeConfig,
                                         DataSourcesRuntimeConfig dataSourcesRuntimeConfig,
                                         DataSourcesJdbcBuildTimeConfig dataSourcesJdbcBuildTimeConfig,
                                         DataSourcesJdbcRuntimeConfig dataSourcesJdbcRuntimeConfig,
                                         TransactionManagerConfiguration transactionRuntimeConfig,
                                         TransactionManager transactionManager,
                                         XAResourceRecoveryRegistry xaResourceRecoveryRegistry,
                                         TransactionSynchronizationRegistry transactionSynchronizationRegistry,
                                         DataSourceSupport dataSourceSupport,
                                         AgroalDataSourceSupport agroalDataSourceSupport,
                                         Instance<io.agroal.api.AgroalPoolInterceptor> agroalPoolInterceptors,
                                         Instance<AgroalOpenTelemetryWrapper> agroalOpenTelemetryWrapper,
                                         @Named("dbaasDataSourceAggregator") AgroalDataSource dbaasDataSourceAggregator) {

        super(dataSourcesBuildTimeConfig, dataSourcesRuntimeConfig, dataSourcesJdbcBuildTimeConfig, dataSourcesJdbcRuntimeConfig, transactionRuntimeConfig, transactionManager, xaResourceRecoveryRegistry, transactionSynchronizationRegistry, dataSourceSupport, agroalDataSourceSupport, agroalPoolInterceptors, agroalOpenTelemetryWrapper);
        this.agroalDataSourceSupport = agroalDataSourceSupport;
        this.dbaasDataSourceAggregator = dbaasDataSourceAggregator;
    }

    @Override
    public AgroalDataSource getDataSource(String dataSourceName) {
        AgroalDataSourceSupport.Entry entry = agroalDataSourceSupport.entries.get(dataSourceName);
        if (DatabaseKind.POSTGRESQL.equals(entry.resolvedDbKind)) {
            if (DEFAULT_DATASOURCE_NAME.equals(dataSourceName)) {
                return dbaasDataSourceAggregator;
            } else {
                InstanceHandle<AgroalDataSource> dataSourceHandle = Arc.container().instance(AgroalDataSource.class, new Annotation[]{new io.quarkus.agroal.DataSource.DataSourceLiteral(dataSourceName)});
                if (dataSourceHandle.isAvailable()) {
                    AgroalDataSource agroalDataSource = dataSourceHandle.get();
                    if (agroalDataSource instanceof DbaaSDataSource) {
                        return agroalDataSource;
                    }
                }
            }
        }
        return super.getDataSource(dataSourceName);
    }
}
