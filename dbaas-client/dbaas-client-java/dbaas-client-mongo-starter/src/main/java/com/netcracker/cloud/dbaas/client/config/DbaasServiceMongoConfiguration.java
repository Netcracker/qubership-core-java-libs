package com.netcracker.cloud.dbaas.client.config;

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
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MongoConverter;

import java.util.HashMap;
import java.util.Map;

import static org.qubership.cloud.dbaas.client.config.DbaasMongoConfiguration.*;

@Configuration
@Import({DbaasMongoConfiguration.class})
public class DbaasServiceMongoConfiguration {

    @Bean(name = {SERVICE_MONGO_DB_FACTORY})
    @ConditionalOnMissingBean(name = SERVICE_MONGO_DB_FACTORY)
    @Primary
    public MongoDatabaseFactory serviceMongoDbFactory(DatabasePool databasePool,
                                                      DbaasClassifierFactory dbaasClassifierFactory,
                                                      @Value("${dbaas.mongo.dbClassifier:default}") String dbClassifierField,
                                                      DbaasApiProperties mongoDbaasApiProperties) {
        Map<String, Object> additionalClassifierFields = new HashMap<>(1);
        additionalClassifierFields.put("dbClassifier", dbClassifierField);
        DbaaSClassifierBuilder classifierBuilder = dbaasClassifierFactory.newServiceClassifierBuilder(additionalClassifierFields);
        DatabaseConfig databaseConfig = DatabaseConfig.builder()
                .dbNamePrefix(mongoDbaasApiProperties.getDbPrefix())
                .userRole(mongoDbaasApiProperties.getRuntimeUserRole())
                .build();
        return new DbaasMongoDbFactory(classifierBuilder, databasePool, databaseConfig);
    }

    @Bean(name = {SERVICE_MONGO_TEMPLATE})
    @ConditionalOnMissingBean(name = SERVICE_MONGO_TEMPLATE)
    @Primary
    public MongoTemplate serviceMongoTemplate(@Qualifier(SERVICE_MONGO_DB_FACTORY) MongoDatabaseFactory serviceAwareMongoDbFactory,
                                              @Autowired(required = false) @Qualifier(SERVICE_MONGO_CONVERTER) MongoConverter mongoConverter) {
        return new MongoTemplate(serviceAwareMongoDbFactory, mongoConverter);
    }
}
