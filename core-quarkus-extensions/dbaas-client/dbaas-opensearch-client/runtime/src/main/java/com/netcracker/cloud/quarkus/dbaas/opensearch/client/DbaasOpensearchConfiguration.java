package com.netcracker.cloud.quarkus.dbaas.opensearch.client;

import com.netcracker.cloud.dbaas.client.DbaasClient;
import com.netcracker.cloud.dbaas.client.metrics.DbaaSMetricsRegistrar;
import com.netcracker.cloud.dbaas.client.metrics.MetricsProvider;
import com.netcracker.cloud.dbaas.client.opensearch.DbaasOpensearchClient;
import com.netcracker.cloud.dbaas.client.opensearch.entity.OpensearchIndex;
import com.netcracker.cloud.dbaas.client.opensearch.metrics.OpensearchMetricsProvider;
import com.netcracker.cloud.dbaas.client.opensearch.service.OpensearchLogicalDbProvider;
import com.netcracker.cloud.dbaas.common.classifier.DbaaSClassifierFactory;
import com.netcracker.cloud.quarkus.dbaas.opensearch.client.config.DbaaSOpensearchConfigurationProperty;
import com.netcracker.cloud.quarkus.dbaas.opensearch.client.config.DbaaSOpensearchCreationConfig;
import com.netcracker.cloud.quarkus.dbaas.opensearch.client.service.OpensearchDbaaSApiClient;
import com.netcracker.cloud.quarkus.dbaas.opensearch.client.service.impl.OpensearchDbaaSApiClientImpl;
import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.arc.DefaultBean;
import io.quarkus.arc.properties.UnlessBuildProperty;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.HashMap;
import java.util.Map;

@Singleton
public class DbaasOpensearchConfiguration {
    public static final String SERVICE_NATIVE_OPENSEARCH_CLIENT = "serviceNativeOpensearchClient";
    public static final String TENANT_NATIVE_OPENSEARCH_CLIENT = "tenantNativeOpensearchClient";

    @ConfigProperty(name = "cloud.microservice.name")
    String microserviceName;

    @ConfigProperty(name = "cloud.microservice.namespace")
    String namespace;

    @Produces
    @Named(SERVICE_NATIVE_OPENSEARCH_CLIENT)
    @DefaultBean
    public DbaasOpensearchClient getDbaaSServiceNativeOpensearchClient(OpensearchDbaaSApiClient opensearchDbaaSApiClient,
                                                                       DbaaSClassifierFactory dbaaSClassifierFactory,
                                                                       DbaaSOpensearchCreationConfig opensearchCreationConfig) {
        return new DbaasOpensearchClientImpl(
                dbaaSClassifierFactory.newServiceClassifierBuilder(getInitialClassifierMap()),
                opensearchDbaaSApiClient,
                opensearchCreationConfig.servicePrefixConfig().delimiter());
    }

    @Produces
    @Named(TENANT_NATIVE_OPENSEARCH_CLIENT)
    @DefaultBean
    public DbaasOpensearchClient getDbaaSTenantNativeOpensearchClient(OpensearchDbaaSApiClient opensearchDbaaSApiClient,
                                                                      DbaaSClassifierFactory dbaaSClassifierFactory,
                                                                      DbaaSOpensearchCreationConfig opensearchCreationConfig) {
        return new DbaasOpensearchClientImpl(
                dbaaSClassifierFactory.newTenantClassifierBuilder(getInitialClassifierMap()),
                opensearchDbaaSApiClient,
                opensearchCreationConfig.singleTenantPrefixConfig().delimiter());
    }

    @Produces
    @ApplicationScoped
    @UnlessBuildProperty(name = "quarkus.micrometer.enabled", stringValue = "false", enableIfMissing = true)
    @UnlessBuildProperty(name = "quarkus.dbaas.opensearch.metrics.enabled", stringValue = "false", enableIfMissing = true)
    public MetricsProvider<OpensearchIndex> opensearchMetricsProvider(MeterRegistry meterRegistry,
                                                                      DbaaSOpensearchConfigurationProperty configurationProperty) {
        return new OpensearchMetricsProvider(meterRegistry,
            configurationProperty.metrics().toDbaasOpensearchMetricsProperties()
        );
    }

    @Produces
    @DefaultBean
    public OpensearchDbaaSApiClient getOpensearchDbaasApiClient(Instance<OpensearchLogicalDbProvider> dbProviders,
                                                                DbaaSOpensearchCreationConfig opensearchCreationConfig,
                                                                DbaasClient dbaaSClient,
                                                                DbaaSOpensearchConfigurationProperty configurationProperty,
                                                                DbaaSMetricsRegistrar metricsRegistrar) {
        return new OpensearchDbaaSApiClientImpl(namespace, dbProviders, opensearchCreationConfig, dbaaSClient, configurationProperty, metricsRegistrar);
    }

    private Map<String, Object> getInitialClassifierMap() {
        Map<String, Object> params = new HashMap<>();
        params.put("microserviceName", microserviceName);
        params.put("namespace", namespace);
        return params;
    }
}
