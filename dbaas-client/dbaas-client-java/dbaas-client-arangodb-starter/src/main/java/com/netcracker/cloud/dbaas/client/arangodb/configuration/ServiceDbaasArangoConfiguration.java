package org.qubership.cloud.dbaas.client.arangodb.configuration;

import com.arangodb.serde.ArangoSerde;
import org.qubership.cloud.dbaas.client.arangodb.entity.database.ArangoDatabase;
import org.qubership.cloud.dbaas.client.arangodb.management.ArangoPostProcessor;
import org.qubership.cloud.dbaas.client.entity.DbaasApiProperties;
import org.qubership.cloud.dbaas.client.management.ArangoDatabaseProvider;
import org.qubership.cloud.dbaas.client.management.DatabaseConfig;
import org.qubership.cloud.dbaas.client.management.DatabasePool;
import org.qubership.cloud.dbaas.client.management.PostConnectProcessor;
import org.qubership.cloud.dbaas.client.management.classifier.DbaasClassifierFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;

import java.util.Optional;

@Configuration
public class ServiceDbaasArangoConfiguration {

    public static final String SERVICE_ARANGODB_PROVIDER = "serviceArangoDbProvider";

    @Bean
    public PostConnectProcessor<ArangoDatabase> arangoPostProcessor(DbaasArangoDBConfigurationProperties properties,
                                                                    @Lazy ArangoSerde arangoSerde) {
        return new ArangoPostProcessor(new DbaasArangoDBClientProperties(properties, arangoSerde));
    }

    @Primary
    @Bean(SERVICE_ARANGODB_PROVIDER)
    @ConditionalOnMissingBean(name = SERVICE_ARANGODB_PROVIDER)
    public ArangoDatabaseProvider serviceArangoDatabaseProvider(DatabasePool databasePool,
                                                                DbaasClassifierFactory classifierFactory,
                                                                DbaasApiProperties arangodbDbaasApiProperties) {
        DatabaseConfig databaseConfig = DatabaseConfig.builder()
                .userRole(arangodbDbaasApiProperties.getRuntimeUserRole())
                .dbNamePrefix(arangodbDbaasApiProperties.getDbPrefix())
                .build();
        return new ArangoDatabaseProvider(databasePool, classifierFactory.newServiceClassifierBuilder(), databaseConfig, arangodbDbaasApiProperties.getRetryAttempts(), arangodbDbaasApiProperties.getRetryDelay());
    }
}
