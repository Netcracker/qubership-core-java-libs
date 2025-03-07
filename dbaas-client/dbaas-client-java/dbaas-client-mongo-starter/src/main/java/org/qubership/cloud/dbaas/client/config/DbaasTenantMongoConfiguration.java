package org.qubership.cloud.dbaas.client.config;

import org.qubership.cloud.dbaas.client.entity.DbaasApiProperties;
import org.qubership.cloud.dbaas.client.management.DatabaseConfig;
import org.qubership.cloud.dbaas.client.management.DatabasePool;
import org.qubership.cloud.dbaas.client.management.DbaasMongoDbFactory;
import org.qubership.cloud.dbaas.client.management.classifier.DbaaSClassifierBuilder;
import org.qubership.cloud.dbaas.client.management.classifier.DbaasClassifierFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MongoConverter;

import java.util.HashMap;
import java.util.Map;

import static org.qubership.cloud.dbaas.client.config.DbaasMongoConfiguration.*;

@Configuration
@Import({DbaasMongoConfiguration.class})
public class DbaasTenantMongoConfiguration {

    @Bean(name = {TENANT_MONGO_DB_FACTORY})
    @ConditionalOnMissingBean(name = TENANT_MONGO_DB_FACTORY)
    public MongoDatabaseFactory tenantMongoDbFactory(DatabasePool databasePool,
                                                     DbaasClassifierFactory dbaasClassifierFactory,
                                                     @Value("${dbaas.mongo.dbClassifier:default}") String dbClassifierField,
                                                     DbaasApiProperties mongoDbaasApiProperties) {
        Map<String, Object> additionalClassifierFields = new HashMap<>(1);
        additionalClassifierFields.put("dbClassifier", dbClassifierField);
        DbaaSClassifierBuilder classifierBuilder = dbaasClassifierFactory.newTenantClassifierBuilder(additionalClassifierFields);
        DatabaseConfig databaseConfig = DatabaseConfig.builder()
                .dbNamePrefix(mongoDbaasApiProperties.getDbPrefix())
                .userRole(mongoDbaasApiProperties.getRuntimeUserRole())
                .build();
        return new DbaasMongoDbFactory(classifierBuilder, databasePool, databaseConfig);
    }

    @Bean(name = {TENANT_MONGO_TEMPLATE})
    @ConditionalOnMissingBean(name = TENANT_MONGO_TEMPLATE)
    public MongoTemplate tenantMongoTemplate(@Qualifier(TENANT_MONGO_DB_FACTORY) MongoDatabaseFactory tenantAwareMongoDbFactory,
                                             @Autowired(required = false) @Qualifier(TENANT_MONGO_CONVERTER) MongoConverter mongoConverter) {
        return new MongoTemplate(tenantAwareMongoDbFactory, mongoConverter);
    }
}
