package com.netcracker.cloud.quarkus.dbaas.mongoclient.service.impl;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.netcracker.cloud.dbaas.client.management.DatabaseConfig;
import com.netcracker.cloud.dbaas.client.management.DbaasDbClassifier;
import com.netcracker.cloud.quarkus.dbaas.mongoclient.config.properties.DbaasMongoDbCreationConfig;
import com.netcracker.cloud.quarkus.dbaas.mongoclient.entity.connection.MongoDBConnection;
import com.netcracker.cloud.quarkus.dbaas.mongoclient.entity.database.MongoDatabase;
import com.netcracker.cloud.quarkus.dbaas.mongoclient.service.MongoClientCreation;
import com.netcracker.cloud.quarkus.dbaas.mongoclient.service.MongoLogicalDbProvider;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.bson.UuidRepresentation;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import com.netcracker.cloud.security.core.utils.tls.TlsUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

@Slf4j
@ApplicationScoped
public class MongoClientCreationImpl implements MongoClientCreation {
    @ConfigProperty(name = "cloud.microservice.namespace")
    String namespace;

    @Inject
    Instance<MongoLogicalDbProvider> dbProviders;

    private DbaasMongoDbCreationConfig dbaasMongoDbCreationConfig;

    public MongoClientCreationImpl(DbaasMongoDbCreationConfig dbaasMongoDbCreationConfig){
        this.dbaasMongoDbCreationConfig = dbaasMongoDbCreationConfig;
    }


    private DatabaseConfig getDbCreateParameters(String tenantId) {
        DatabaseConfig.Builder builder = DatabaseConfig.builder();
        builder.dbNamePrefix(dbaasMongoDbCreationConfig.dbaasApiPropertiesConfig().getDbaaseApiProperties().getDbPrefix());
        builder.userRole(dbaasMongoDbCreationConfig.dbaasApiPropertiesConfig().getDbaaseApiProperties().getRuntimeUserRole());
        dbaasMongoDbCreationConfig.getMongoDbConfiguration(tenantId).getDatabaseSettings().ifPresent(builder::databaseSettings);
        return builder.build();
    }


    private final Map<DbaasDbClassifier, MongoDatabase> mongoDbMap = new ConcurrentHashMap<>();

    @Override
    public MongoDatabase getOrCreateMongoDatabase(DbaasDbClassifier classifier) {
        log.trace("Create new mongo database for {}", classifier);
        return mongoDbMap.computeIfAbsent(classifier, this::createMongoDatabase);
    }

    private MongoDatabase createMongoDatabase(DbaasDbClassifier dbaasDbClassifier) {
        Map<String, Object> classifier = dbaasDbClassifier.asMap();
        String tenantId = (String) classifier.get("tenantId");
        DatabaseConfig config = getDbCreateParameters(tenantId);
        log.debug("Create new MongoClient for {}", classifier);

        MongoDatabase db = resolveMongoDatabase(classifier, config);
        log.debug("Connection: " + db.getConnectionProperties());

        log.debug("Starting the initialization of MongoClient for database with classifier: {}", db.getClassifier());
        MongoDBConnection connectionProperties = db.getConnectionProperties();
        setDbName(connectionProperties);

        CodecRegistry pojoCodecRegistry = fromRegistries(MongoClientSettings.getDefaultCodecRegistry(),
                fromProviders(PojoCodecProvider.builder().automatic(true).build()));
        MongoClientSettings.Builder mongoBuilder = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(connectionProperties.getUrl()))
                .uuidRepresentation(UuidRepresentation.JAVA_LEGACY)
                .codecRegistry(pojoCodecRegistry)
                .credential(getMongoCredential(connectionProperties));
        if (connectionProperties.isTls()) {
            log.info("Connection to mongodb will be secured");
            mongoBuilder.applyToSslSettings(builder ->
                    builder
                            .enabled(true)
                            .context(TlsUtils.getSslContext())
            );
        }
        MongoClientSettings mongoClientSettings = mongoBuilder.build();

        MongoClient mongoClient = MongoClients.create(mongoClientSettings);

        log.info("Created mongo client: {}", mongoClient);
        connectionProperties.setClient(mongoClient);
        return db;
    }

    /**
     * Resolves the mongo database through the {@link MongoLogicalDbProvider} chain: providers are
     * tried in ascending {@link MongoLogicalDbProvider#order()} order and the first non-null result
     * wins. The file-backed mounted-secret provider (order {@code MAX_VALUE - 1}) is consulted just
     * before the dbaas-aggregator provider (order {@code MAX_VALUE}), so when no Secret is mounted
     * resolution falls through to REST exactly as before.
     */
    private MongoDatabase resolveMongoDatabase(Map<String, Object> classifier, DatabaseConfig config) {
        SortedMap<String, Object> sortedClassifier = new TreeMap<>(classifier);
        for (MongoLogicalDbProvider provider : sortProviders(dbProviders)) {
            MongoDatabase db = provider.provide(sortedClassifier, config, namespace);
            if (db != null) {
                if (db.getConnectionProperties() == null) {
                    throw new IllegalStateException("Provider: " + provider
                            + " provided a mongo database but connection properties is null");
                }
                return db;
            }
        }
        throw new IllegalStateException("No MongoLogicalDbProvider resolved a database for classifier " + classifier);
    }

    private List<MongoLogicalDbProvider> sortProviders(Instance<MongoLogicalDbProvider> dbProviders) {
        return dbProviders.stream()
                .sorted(Comparator.comparingInt(MongoLogicalDbProvider::order))
                .toList();
    }

    private void setDbName(MongoDBConnection connectionProperties) {
        if (connectionProperties.getDbName() == null) {
            connectionProperties.setDbName(connectionProperties.getAuthDbName());
        }
    }

    private MongoCredential getMongoCredential(MongoDBConnection connectionProperties) {
        return MongoCredential.createScramSha1Credential(connectionProperties.getUsername(), connectionProperties.getAuthDbName(), connectionProperties.getPassword().toCharArray());
    }
}
