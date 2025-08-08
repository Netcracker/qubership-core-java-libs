package org.qubership.cloud.dbaas.client.opensearch.config;

import org.qubership.cloud.dbaas.client.entity.database.DatabaseSettings;
import org.qubership.cloud.dbaas.client.management.DatabaseConfig;
import org.qubership.cloud.dbaas.client.management.DatabasePool;
import org.qubership.cloud.dbaas.client.management.classifier.DbaasClassifierFactory;
import org.qubership.cloud.dbaas.client.opensearch.DbaasOpensearchClient;
import org.qubership.cloud.dbaas.client.opensearch.DbaasOpensearchClientImpl;
import org.qubership.cloud.dbaas.client.opensearch.entity.OpensearchProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DbaasTenantNativeOpensearchConfiguration extends DbaasOpensearchConfiguration {

    @Bean(name = {TENANT_NATIVE_OPENSEARCH_CLIENT})
    @ConditionalOnMissingBean(name = TENANT_NATIVE_OPENSEARCH_CLIENT)
    public DbaasOpensearchClient tenantOpensearchClient(DatabasePool databasePool,
                                                        DbaasClassifierFactory classifierFactory,
                                                        OpensearchProperties opensearchProperties) {
        DatabaseSettings dbSettings = getDatabaseSettings();
        DatabaseConfig.Builder databaseConfigBuilder = DatabaseConfig.builder()
                .userRole(opensearchProperties.getRuntimeUserRole())
                .databaseSettings(dbSettings);
        OpensearchConfig opensearchConfig = new OpensearchConfig(opensearchProperties, opensearchProperties.getTenant().getDelimiter());
        return new DbaasOpensearchClientImpl(databasePool,
                classifierFactory.newTenantClassifierBuilder(), databaseConfigBuilder, opensearchConfig);
    }
}
