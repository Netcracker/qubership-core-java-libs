package com.netcracker.cloud.dbaas.client.management;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.client.MongoClient;
import com.mongodb.client.internal.MongoClientImpl;
import com.netcracker.cloud.dbaas.client.entity.connection.MongoDBConnection;
import com.netcracker.cloud.dbaas.client.entity.database.MongoDatabase;
import com.netcracker.cloud.security.core.utils.tls.TlsUtils;
import lombok.extern.slf4j.Slf4j;
import org.bson.UuidRepresentation;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.util.concurrent.TimeUnit;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

/**
 * Creates MongoClient for the database passed as parameter to this postConnectProcessor.
 * This allows to keep 1 Mongo client per each created database
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MongoPostConnectProcessor implements PostConnectProcessor<MongoDatabase> {

    private MongoClientSettings defaultMongoClientSettings;

    public MongoPostConnectProcessor(MongoClientSettings defaultMongoClientSettings) {
        this.defaultMongoClientSettings = defaultMongoClientSettings;
    }

    @Override
    public void process(MongoDatabase database) {
        log.debug("Starting the initialization of MongoClient for database with classifier {}", database.getClassifier());
        MongoDBConnection connectionProperties = database.getConnectionProperties();
        ConnectionString connectionString = new ConnectionString(connectionProperties.getUrl());
        String username = connectionString.getUsername() != null ? connectionString.getUsername() : connectionProperties.getUsername();
        String password = connectionString.getPassword() != null ? String.valueOf(connectionString.getPassword()) : connectionProperties.getPassword();
        String authDb = getAuthDb(connectionString.getCredential(), connectionProperties.getAuthDbName());


        MongoClientSettings.Builder mongoBuilder = defaultMongoClientSettings != null ?
                MongoClientSettings.builder(defaultMongoClientSettings) : MongoClientSettings.builder();
        CodecRegistry pojoCodecRegistry = fromRegistries(MongoClientSettings.getDefaultCodecRegistry(),
                fromProviders(PojoCodecProvider.builder().automatic(true).build()));

        if (connectionProperties.isTls()) {
            log.info("Connection to mongodb will be secured");
            mongoBuilder.applyToSslSettings(builder ->
                    builder
                            .enabled(true)
                            .context(TlsUtils.getSslContext())
            );
        }
        MongoClientSettings mongoClientSettings = mongoBuilder
                .applyConnectionString(connectionString)
                .uuidRepresentation(UuidRepresentation.JAVA_LEGACY)
                .codecRegistry(pojoCodecRegistry)
                .credential(getMongoCredential(username, password, authDb))
                .applyToConnectionPoolSettings(builder ->
                        builder
                                .maxConnectionIdleTime(5, TimeUnit.MINUTES)
                                .maxConnectionLifeTime(5, TimeUnit.MINUTES)
                )
                .build();

        MongoClient mongoClient = new MongoClientImpl(mongoClientSettings, null);

        log.debug("Created mongo client: {}", mongoClient);
        connectionProperties.setClient(mongoClient);
    }

    private String getAuthDb(MongoCredential credential, String authDbName) {
        if (credential != null && !credential.getSource().isEmpty()) {
            return credential.getSource();
        }
        return authDbName;
    }

    private MongoCredential getMongoCredential(String username, String password, String authDbName) {
        return MongoCredential.createScramSha1Credential(username, authDbName, password.toCharArray());
    }

    @Override
    public Class<MongoDatabase> getSupportedDatabaseType() {
        return MongoDatabase.class;
    }
}
