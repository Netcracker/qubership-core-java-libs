package org.qubership.cloud.quarkus.dbaas.mongoclient;

import com.mongodb.ClientSessionOptions;
import com.mongodb.client.ChangeStreamIterable;
import com.mongodb.client.ClientSession;
import com.mongodb.client.ListDatabasesIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoIterable;
import com.mongodb.connection.ClusterDescription;
import org.qubership.cloud.dbaas.client.management.classifier.DbaaSClassifierBuilder;
import org.qubership.cloud.quarkus.dbaas.mongoclient.entity.connection.MongoDBConnection;
import org.qubership.cloud.quarkus.dbaas.mongoclient.entity.database.MongoDatabase;
import org.qubership.cloud.quarkus.dbaas.mongoclient.service.MongoClientCreation;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.List;

public class DbaaSMongoClient implements MongoClient {

    private MongoClientCreation mongoClientCreation;
    private DbaaSClassifierBuilder classifierBuilder;

    public DbaaSMongoClient(DbaaSClassifierBuilder classifierBuilder, MongoClientCreation mongoClientCreation) {
        this.classifierBuilder = classifierBuilder;
        this.mongoClientCreation = mongoClientCreation;
    }

    MongoDatabase getOrCreateMongoDb() {
        return mongoClientCreation.getOrCreateMongoDatabase(classifierBuilder.build());
    }

    public MongoDBConnection getConnectionProperties() {
        return getOrCreateMongoDb().getConnectionProperties();
    }

    private MongoClient getMongoClient() {
        return getOrCreateMongoDb().getConnectionProperties().getClient();
    }

    public com.mongodb.client.MongoDatabase getDatabase() {
        MongoDatabase mongoDb = getOrCreateMongoDb();
        return mongoDb.getConnectionProperties().getClient().getDatabase(mongoDb.getConnectionProperties().getDbName());
    }

    @Override
    public com.mongodb.client.MongoDatabase getDatabase(String ignored) {
        return getDatabase();
    }

    @Override
    public ClientSession startSession() {
        return getMongoClient().startSession();
    }

    @Override
    public ClientSession startSession(ClientSessionOptions clientSessionOptions) {
        return getMongoClient().startSession(clientSessionOptions);
    }

    @Override
    public void close() {
        getMongoClient().close();
    }

    @Override
    public MongoIterable<String> listDatabaseNames() {
        return getMongoClient().listDatabaseNames();
    }

    @Override
    public MongoIterable<String> listDatabaseNames(ClientSession clientSession) {
        return getMongoClient().listDatabaseNames(clientSession);
    }

    @Override
    public ListDatabasesIterable<Document> listDatabases() {
        return getMongoClient().listDatabases();
    }

    @Override
    public ListDatabasesIterable<Document> listDatabases(ClientSession clientSession) {
        return getMongoClient().listDatabases(clientSession);
    }

    @Override
    public <TResult> ListDatabasesIterable<TResult> listDatabases(Class<TResult> aClass) {
        return getMongoClient().listDatabases(aClass);
    }

    @Override
    public <TResult> ListDatabasesIterable<TResult> listDatabases(ClientSession clientSession, Class<TResult> aClass) {
        return getMongoClient().listDatabases(clientSession, aClass);
    }

    @Override
    public ChangeStreamIterable<Document> watch() {
        return getMongoClient().watch();
    }

    @Override
    public <TResult> ChangeStreamIterable<TResult> watch(Class<TResult> aClass) {
        return getMongoClient().watch(aClass);
    }

    @Override
    public ChangeStreamIterable<Document> watch(List<? extends Bson> list) {
        return getMongoClient().watch(list);
    }

    @Override
    public <TResult> ChangeStreamIterable<TResult> watch(List<? extends Bson> list, Class<TResult> aClass) {
        return getMongoClient().watch(list, aClass);
    }

    @Override
    public ChangeStreamIterable<Document> watch(ClientSession clientSession) {
        return getMongoClient().watch(clientSession);
    }

    @Override
    public <TResult> ChangeStreamIterable<TResult> watch(ClientSession clientSession, Class<TResult> aClass) {
        return getMongoClient().watch(clientSession, aClass);
    }

    @Override
    public ChangeStreamIterable<Document> watch(ClientSession clientSession, List<? extends Bson> list) {
        return getMongoClient().watch(clientSession, list);
    }

    @Override
    public <TResult> ChangeStreamIterable<TResult> watch(ClientSession clientSession, List<? extends Bson> list, Class<TResult> aClass) {
        return getMongoClient().watch(clientSession, list, aClass);
    }

    @Override
    public ClusterDescription getClusterDescription() {
        return getMongoClient().getClusterDescription();
    }
}
