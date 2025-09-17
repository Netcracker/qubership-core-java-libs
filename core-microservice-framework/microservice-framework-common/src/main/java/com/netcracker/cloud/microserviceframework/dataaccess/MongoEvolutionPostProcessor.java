package com.netcracker.cloud.microserviceframework.dataaccess;


import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.netcracker.cloud.dbaas.client.entity.connection.MongoDBConnection;
import com.netcracker.cloud.dbaas.client.entity.database.MongoDatabase;
import com.netcracker.cloud.dbaas.client.management.PostConnectProcessor;
import com.netcracker.cloud.microserviceframework.config.MongoPackagesConfigHolder;
import com.netcracker.cloud.mongoevolution.SpringMongoEvolution;
import com.netcracker.cloud.mongoevolution.java.dataaccess.ConnectionSearchKey;
import lombok.extern.slf4j.Slf4j;
import org.bson.UuidRepresentation;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.netcracker.cloud.microserviceframework.config.MongoEvolutionConfiguration.MICROSERVICE_MONGO_EVOLUTION_DATA_BEANS;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

@Slf4j
public class MongoEvolutionPostProcessor implements PostConnectProcessor<MongoDatabase> {

    @Autowired(required = false)
    @Qualifier(MICROSERVICE_MONGO_EVOLUTION_DATA_BEANS)
    private Map<String, Object> classNamesAndBeans;

    @Autowired
    private MongoPackagesConfigHolder configHolderMongo;

    private List<String> microservicePackages;
    private List<String> tenantAwarePackages;
    private List<String> defaultPackages;

    private Environment environment;

    @Value("${mongo-evolution.auth.enabled:true}")
    private boolean authEnabled; // it's needed only for test, will be removed in 4.x

    public MongoEvolutionPostProcessor(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    private void init() {
        classNamesAndBeans = (null == classNamesAndBeans) ? null
                : (Map<String, Object>) classNamesAndBeans.get(MICROSERVICE_MONGO_EVOLUTION_DATA_BEANS);

        if (configHolderMongo.isUsingCustomPackages()) {
            microservicePackages = new ArrayList<>(2);
            tenantAwarePackages = new ArrayList<>(2);
            if (!StringUtils.isEmpty(configHolderMongo.getMicroserviceCustomPackage())) {
                microservicePackages.add(configHolderMongo.getMicroserviceCustomPackage());
            }
            if (!StringUtils.isEmpty(configHolderMongo.getTenantAwareCustomPackage())) {
                tenantAwarePackages.add(configHolderMongo.getTenantAwareCustomPackage());
            }
            if (!StringUtils.isEmpty(configHolderMongo.getCommonPackage())) {
                if (!configHolderMongo.getCommonPackage().equals(configHolderMongo.getMicroserviceCustomPackage())) {
                    microservicePackages.add(configHolderMongo.getCommonPackage());
                }
                if (!configHolderMongo.getCommonPackage().equals(configHolderMongo.getTenantAwareCustomPackage())) {
                    tenantAwarePackages.add(configHolderMongo.getCommonPackage());
                }
            }
        } else {
            defaultPackages = new ArrayList<>(2);
            defaultPackages.add(configHolderMongo.getDefaultPackage());
            if (!StringUtils.isEmpty(configHolderMongo.getCommonPackage()) && !configHolderMongo.getCommonPackage().equals(configHolderMongo.getDefaultPackage())) {
                defaultPackages.add(configHolderMongo.getCommonPackage());
            }
        }
    }

    @Override
    public void process(MongoDatabase database) {
        try {
            log.info("Starting mongo evolution for db: {} and classifier: {}", database.getName(), database.getClassifier());
            Map<String, Object> classifierMap = database.getClassifier();

            Object tenantIdValue = classifierMap.get("tenantId");
            String tenantId = tenantIdValue == null ? null : String.valueOf(tenantIdValue);

            Object dbClassifierValue = classifierMap.get("dbClassifier");
            String dbClassifier = dbClassifierValue == null ? null : String.valueOf(dbClassifierValue);

            ConnectionSearchKey searchKey = new ConnectionSearchKey(tenantId, dbClassifier);
            MongoDBConnection connectionProperties = database.getConnectionProperties();

            List<String> mongoEvolutionPackages = configHolderMongo.isUsingCustomPackages()
                    ? (tenantId == null ? microservicePackages : tenantAwarePackages)
                    : defaultPackages;
            if (mongoEvolutionPackages == null || mongoEvolutionPackages.isEmpty()) {
                log.info("No packages are specified, nothing to do with database {}", database);
                return;
            }
            try (MongoClient mongoClientV4 = buildV4MongoClient(connectionProperties)) {
                SpringMongoEvolution evolution = new SpringMongoEvolution(mongoClientV4, database.getName(), searchKey);
                evolution.evolve(mongoEvolutionPackages, environment, classNamesAndBeans);
            }
            log.info("Finished mongo evolution execution");
        } catch (Exception e) {
            throw new RuntimeException("Error occurred during mongo evolution execution", e);
        }
    }

    private com.mongodb.client.MongoClient buildV4MongoClient(MongoDBConnection connectionProperties) {
        CodecRegistry pojoCodecRegistry = fromRegistries(MongoClientSettings.getDefaultCodecRegistry(),
                fromProviders(PojoCodecProvider.builder().automatic(true).build()));
        MongoClientSettings.Builder mongoClientSettingsBuilder = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(connectionProperties.getUrl()))
                .uuidRepresentation(UuidRepresentation.JAVA_LEGACY)
                .codecRegistry(pojoCodecRegistry);
        if (authEnabled) {
            mongoClientSettingsBuilder.credential(getMongoCredential(connectionProperties));
        }
        return MongoClients.create(mongoClientSettingsBuilder.build());
    }

    private MongoCredential getMongoCredential(MongoDBConnection connectionProperties) {
        return MongoCredential.createScramSha1Credential(connectionProperties.getUsername(), connectionProperties.getAuthDbName(), connectionProperties.getPassword().toCharArray());
    }

    @Override
    public Class<MongoDatabase> getSupportedDatabaseType() {
        return MongoDatabase.class;
    }
}
