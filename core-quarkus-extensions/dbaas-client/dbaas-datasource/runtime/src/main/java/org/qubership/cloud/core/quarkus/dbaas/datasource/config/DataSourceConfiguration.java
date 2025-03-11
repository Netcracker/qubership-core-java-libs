package org.qubership.cloud.core.quarkus.dbaas.datasource.config;

import org.qubership.cloud.core.quarkus.dbaas.datasource.DbaaSDataSource;
import org.qubership.cloud.core.quarkus.dbaas.datasource.DbaasQuarkusPostgresqlDatasourceBuilder;
import org.qubership.cloud.core.quarkus.dbaas.datasource.classifier.MicroserviceClassifierBuilder;
import org.qubership.cloud.core.quarkus.dbaas.datasource.classifier.TenantClassifierBuilder;
import org.qubership.cloud.core.quarkus.dbaas.datasource.service.DbaaSPostgresDbCreationService;
import io.agroal.api.AgroalDataSource;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.validation.constraints.NotNull;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.HashMap;
import java.util.Map;

@Singleton
public class DataSourceConfiguration {
    public static final String TENANT_DATASOURCE = "tenantDataSource";
    public static final String SERVICE_DATASOURCE = "serviceDataSource";
    @ConfigProperty(name = "cloud.microservice.name")
    String microserviceName;

    @ConfigProperty(name = "cloud.microservice.namespace")
    String namespace;

    @Produces
    @Named(SERVICE_DATASOURCE)
    @DefaultBean
    public AgroalDataSource getDbaasServiceDataSource(@NotNull DbaaSPostgresDbCreationService dataSourceCreationService) {
        return new DbaaSDataSource(new MicroserviceClassifierBuilder(getInitialClassifierMap()), dataSourceCreationService);
    }

    @Produces
    @Named(TENANT_DATASOURCE)
    @DefaultBean
    public AgroalDataSource getDbaasTenantDataSource(@NotNull DbaaSPostgresDbCreationService dataSourceCreationService) {
        return new DbaaSDataSource(new TenantClassifierBuilder(getInitialClassifierMap()), dataSourceCreationService);
    }

    @Produces
    @DefaultBean
    public DbaasQuarkusPostgresqlDatasourceBuilder getDbaasQuarkusPostgresqlDatasourceBuilder(@NotNull DbaaSPostgresDbCreationService dataSourceCreationService) {
        return new DbaasQuarkusPostgresqlDatasourceBuilder(dataSourceCreationService);
    }

    private Map<String, Object> getInitialClassifierMap() {
        Map<String, Object> params = new HashMap<>();
        params.put("microserviceName", microserviceName);
        params.put("namespace", namespace);
        return params;
    }
}
