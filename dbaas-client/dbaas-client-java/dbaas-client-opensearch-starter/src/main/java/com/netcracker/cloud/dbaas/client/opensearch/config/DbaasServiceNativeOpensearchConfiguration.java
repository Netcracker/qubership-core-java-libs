package com.netcracker.cloud.dbaas.client.opensearch.config;

import com.netcracker.cloud.dbaas.client.entity.database.DatabaseSettings;
import com.netcracker.cloud.dbaas.client.management.DatabaseConfig;
import com.netcracker.cloud.dbaas.client.management.DatabasePool;
import com.netcracker.cloud.dbaas.client.management.classifier.DbaasClassifierFactory;
import com.netcracker.cloud.dbaas.client.opensearch.DbaasOpensearchClient;
import com.netcracker.cloud.dbaas.client.opensearch.DbaasOpensearchClientImpl;
import com.netcracker.cloud.dbaas.client.opensearch.entity.OpensearchProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DbaasServiceNativeOpensearchConfiguration extends DbaasOpensearchConfiguration {

    @Bean(name = {SERVICE_NATIVE_OPENSEARCH_CLIENT})
    @ConditionalOnMissingBean(name = SERVICE_NATIVE_OPENSEARCH_CLIENT)
    public DbaasOpensearchClient microserviceOpensearchRestClient(DatabasePool databasePool,
                                                                  DbaasClassifierFactory classifierFactory,
                                                                  OpensearchProperties opensearchProperties) {
        DatabaseSettings dbSettings = getDatabaseSettings();
        DatabaseConfig.Builder databaseConfig = DatabaseConfig.builder()
                .userRole(opensearchProperties.getRuntimeUserRole())
                .databaseSettings(dbSettings);
        OpensearchConfig opensearchConfig = new OpensearchConfig(opensearchProperties, opensearchProperties.getService().getDelimiter());
        return new DbaasOpensearchClientImpl(databasePool,
                classifierFactory.newServiceClassifierBuilder(), databaseConfig, opensearchConfig);
    }
}
