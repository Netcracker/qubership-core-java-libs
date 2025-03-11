package org.qubership.cloud.quarkus.dbaas.mongoclient.service.impl;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.client.MongoClient;
import com.mongodb.client.internal.MongoClientImpl;
import org.qubership.cloud.dbaas.client.DbaasClient;
import org.qubership.cloud.dbaas.client.management.DatabaseConfig;
import org.qubership.cloud.dbaas.client.management.DbaasDbClassifier;
import org.qubership.cloud.quarkus.dbaas.mongoclient.config.properties.DbaasMongoDbCreationConfig;
import org.qubership.cloud.quarkus.dbaas.mongoclient.entity.connection.MongoDBConnection;
import org.qubership.cloud.quarkus.dbaas.mongoclient.entity.database.MongoDatabase;
import org.qubership.cloud.quarkus.dbaas.mongoclient.entity.database.type.MongoDBType;
import org.qubership.cloud.quarkus.dbaas.mongoclient.service.MongoClientCreation;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.bson.UuidRepresentation;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.qubership.cloud.security.core.utils.tls.TlsUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

@Slf4j
@ApplicationScoped
public class MongoClientCreationImpl implements MongoClientCreation {
    @ConfigProperty(name = "cloud.microservice.namespace")
    String namespace;

    @Inject
    DbaasClient dbaaSClient;

    private DbaasMongoDbCreationConfig dbaasMongoDbCreationConfig;

    public MongoClientCreationImpl(DbaasMongoDbCreationConfig dbaasMongoDbCreationConfig){
        this.dbaasMongoDbCreationConfig = dbaasMongoDbCreationConfig;
    }


    private DatabaseConfig getDbCreateParameters() {
        DatabaseConfig.Builder builder = DatabaseConfig.builder();
        builder.dbNamePrefix(dbaasMongoDbCreationConfig.dbaasApiPropertiesConfig.getDbaaseApiProperties().getDbPrefix());
        builder.userRole(dbaasMongoDbCreationConfig.dbaasApiPropertiesConfig.getDbaaseApiProperties().getRuntimeUserRole());
        return builder.build();
    }


    private final Map<DbaasDbClassifier, MongoDatabase> mongoDbMap = new ConcurrentHashMap<>();

    @Override
    public MongoDatabase getOrCreateMongoDatabase(DbaasDbClassifier classifier) {
        log.trace("Create new mongo database for {}", classifier);
        return mongoDbMap.computeIfAbsent(classifier, this::createMongoDatabase);
    }

    private MongoDatabase createMongoDatabase(DbaasDbClassifier dbaasDbClassifier) {
        DatabaseConfig config = getDbCreateParameters();
        Map<String, Object> classifier = dbaasDbClassifier.asMap();
        log.debug("Create new MongoClient for {}", classifier);

        MongoDatabase db = dbaaSClient.getOrCreateDatabase(MongoDBType.INSTANCE, namespace, classifier, config);
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
        MongoClient mongoClient = new MongoClientImpl(mongoClientSettings, null);

        log.info("Created mongo client: {}", mongoClient);
        connectionProperties.setClient(mongoClient);
        return db;
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
