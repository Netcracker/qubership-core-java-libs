package com.netcracker.cloud.dbaas.client.management;

import com.mongodb.ClientSessionOptions;
import com.mongodb.WriteConcern;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import org.qubership.cloud.dbaas.client.entity.database.MongoDatabase;
import org.qubership.cloud.dbaas.client.entity.database.type.MongoDBType;
import org.qubership.cloud.dbaas.client.management.classifier.DbaaSClassifierBuilder;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.support.PersistenceExceptionTranslator;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoExceptionTranslator;
import org.springframework.lang.Nullable;

/**
 * The default mongo factory which created mongo db via dbaas client
 */
public class DbaasMongoDbFactory implements MongoDatabaseFactory {

    protected DbaaSClassifierBuilder dbClassifierBuilder;
    protected DatabasePool databasePool;
    protected PersistenceExceptionTranslator exceptionTranslator;

    protected DatabaseConfig databaseConfig;
    @Nullable
    protected WriteConcern writeConcern;

    public DbaasMongoDbFactory(DbaaSClassifierBuilder dbClassifierBuilder, DatabasePool databasePool, DatabaseConfig databaseConfig) {
        this.dbClassifierBuilder = dbClassifierBuilder;
        this.databasePool = databasePool;
        this.exceptionTranslator = new MongoExceptionTranslator();
        this.databaseConfig = databaseConfig;
    }

    public DbaasMongoDbFactory(DbaaSClassifierBuilder dbClassifierBuilder, DatabasePool databasePool,
                               PersistenceExceptionTranslator exceptionTranslator, DatabaseConfig databaseConfig) {
        this.dbClassifierBuilder = dbClassifierBuilder;
        this.databasePool = databasePool;
        this.exceptionTranslator = exceptionTranslator;
        this.databaseConfig = databaseConfig;
    }

    public void setWriteConcern(WriteConcern writeConcern) {
        this.writeConcern = writeConcern;
    }

    @Override
    public com.mongodb.client.MongoDatabase getMongoDatabase() throws DataAccessException {
        return getMongoDatabase("default");
    }

    @Override
    public com.mongodb.client.MongoDatabase getMongoDatabase(String ignored) throws DataAccessException {
        MongoDatabase database = databasePool.getOrCreateDatabase(MongoDBType.INSTANCE, dbClassifierBuilder.build(), databaseConfig);
        MongoClient mongoClient = database.getConnectionProperties().getClient();
        com.mongodb.client.MongoDatabase db = mongoClient.getDatabase(database.getName());
        return this.writeConcern == null ? db : db.withWriteConcern(this.writeConcern);
    }

    @Override
    public PersistenceExceptionTranslator getExceptionTranslator() {
        return exceptionTranslator;
    }

    @Override
    public ClientSession getSession(ClientSessionOptions options) {
        MongoDatabase database = databasePool.getOrCreateDatabase(MongoDBType.INSTANCE, dbClassifierBuilder.build());
        return database.getConnectionProperties().getClient().startSession(options);
    }

    @Override
    public MongoDatabaseFactory withSession(ClientSession session) {
        return new ClientSessionBoundMongoDbFactory(session, this);
    }
}
