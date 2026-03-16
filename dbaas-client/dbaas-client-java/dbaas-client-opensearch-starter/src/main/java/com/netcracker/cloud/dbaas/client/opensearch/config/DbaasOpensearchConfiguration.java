package com.netcracker.cloud.dbaas.client.opensearch.config;

import com.netcracker.cloud.dbaas.client.config.EnableDbaasDefault;
import com.netcracker.cloud.dbaas.client.entity.database.DatabaseSettings;
import com.netcracker.cloud.dbaas.client.management.PostConnectProcessor;
import com.netcracker.cloud.dbaas.client.metrics.DbaaSMetricsRegistrar;
import com.netcracker.cloud.dbaas.client.opensearch.entity.OpensearchDatabaseSettings;
import com.netcracker.cloud.dbaas.client.opensearch.entity.OpensearchIndex;
import com.netcracker.cloud.dbaas.client.opensearch.entity.OpensearchProperties;
import com.netcracker.cloud.dbaas.client.opensearch.management.OpensearchPostConnectProcessor;
import com.netcracker.cloud.dbaas.client.opensearch.DbaasOpensearchClientBuilderCustomizer;
import com.netcracker.cloud.dbaas.client.opensearch.DefaultDbaasOpensearchClientBuilderCustomizer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@EnableDbaasDefault
@EnableConfigurationProperties
@Slf4j
public class DbaasOpensearchConfiguration {
    public static final String TENANT_OPENSEARCH_CLIENT = "tenantOpensearchClient";
    public static final String SERVICE_OPENSEARCH_CLIENT = "serviceOpensearchClient";
    public static final String TENANT_NATIVE_OPENSEARCH_CLIENT = "tenantNativeOpensearchClient";
    public static final String SERVICE_NATIVE_OPENSEARCH_CLIENT = "serviceNativeOpensearchClient";

    @Bean
    public DbaasOpensearchClientBuilderCustomizer bulder(){
            return new DefaultDbaasOpensearchClientBuilderCustomizer();
    }

    @Bean
    @ConfigurationProperties(prefix = "dbaas.api.opensearch")
    public OpensearchProperties opensearchCustomPrefixProperties(){
        return new OpensearchProperties();
    }

    @Bean
    @ConfigurationProperties(prefix = "dbaas.opensearch")
    public DbaaSOpensearchConfigurationProperty dbaaSOpensearchConfigurationProperty(){
        return new DbaaSOpensearchConfigurationProperty();
    }

    @Bean
    public PostConnectProcessor<OpensearchIndex> opensearchPostConnectProcessor(
            DbaaSOpensearchConfigurationProperty configurationProperties,
            ObjectProvider<DbaasOpensearchClientBuilderCustomizer> builderCustomizers,
            @Autowired(required = false) DbaaSMetricsRegistrar metricsRegistrar
    ) {
        List<DbaasOpensearchClientBuilderCustomizer> sortedBuilderCustomizer =
                builderCustomizers.orderedStream().collect(Collectors.toList());
        return new OpensearchPostConnectProcessor(configurationProperties, sortedBuilderCustomizer, metricsRegistrar);
    }

    protected DatabaseSettings getDatabaseSettings() {
        OpensearchDatabaseSettings opensearchDatabaseSettings = new OpensearchDatabaseSettings();
        opensearchDatabaseSettings.setResourcePrefix(true);
        opensearchDatabaseSettings.setCreateOnly(Collections.singletonList("user"));
        return opensearchDatabaseSettings;
    }

}
