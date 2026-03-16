package com.netcracker.cloud.quarkus.dbaas.mongoclient;

import com.mongodb.*;
import com.mongodb.client.*;
import com.mongodb.client.model.bulk.ClientBulkWriteOptions;
import com.mongodb.client.model.bulk.ClientBulkWriteResult;
import com.mongodb.client.model.bulk.ClientNamespacedWriteModel;
import com.mongodb.connection.ClusterDescription;
import com.netcracker.cloud.dbaas.client.management.classifier.DbaaSClassifierBuilder;
import com.netcracker.cloud.quarkus.dbaas.mongoclient.entity.connection.MongoDBConnection;
import com.netcracker.cloud.quarkus.dbaas.mongoclient.entity.database.MongoDatabase;
import com.netcracker.cloud.quarkus.dbaas.mongoclient.service.MongoClientCreation;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;

import java.util.List;
import java.util.concurrent.TimeUnit;

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
    public ClientBulkWriteResult bulkWrite(List<? extends ClientNamespacedWriteModel> list) throws ClientBulkWriteException {
        return getMongoClient().bulkWrite(list);
    }

    @Override
    public ClientBulkWriteResult bulkWrite(List<? extends ClientNamespacedWriteModel> list, ClientBulkWriteOptions clientBulkWriteOptions) throws ClientBulkWriteException {
        return getMongoClient().bulkWrite(list, clientBulkWriteOptions);
    }

    @Override
    public ClientBulkWriteResult bulkWrite(ClientSession clientSession, List<? extends ClientNamespacedWriteModel> list) throws ClientBulkWriteException {
        return getMongoClient().bulkWrite(clientSession, list);
    }

    @Override
    public ClientBulkWriteResult bulkWrite(ClientSession clientSession, List<? extends ClientNamespacedWriteModel> list, ClientBulkWriteOptions clientBulkWriteOptions) throws ClientBulkWriteException {
        return getMongoClient().bulkWrite(clientSession, list, clientBulkWriteOptions);
    }

    @Override
    public ClusterDescription getClusterDescription() {
        return getMongoClient().getClusterDescription();
    }

    @Override
    public CodecRegistry getCodecRegistry() {
        return getMongoClient().getCodecRegistry();
    }

    @Override
    public ReadPreference getReadPreference() {
        return getMongoClient().getReadPreference();
    }

    @Override
    public WriteConcern getWriteConcern() {
        return getMongoClient().getWriteConcern();
    }
    @Override
    public ReadConcern getReadConcern() {
        return getMongoClient().getReadConcern();
    }

    @Override
    public Long getTimeout(TimeUnit timeUnit) {
        return getMongoClient().getTimeout(timeUnit);
    }

    @Override
    public MongoCluster withCodecRegistry(CodecRegistry codecRegistry) {
        return getMongoClient().withCodecRegistry(codecRegistry);
    }

    @Override
    public MongoCluster withReadPreference(ReadPreference readPreference) {
        return getMongoClient().withReadPreference(readPreference);
    }

    @Override
    public MongoCluster withWriteConcern(WriteConcern writeConcern) {
        return getMongoClient().withWriteConcern(writeConcern);
    }

    @Override
    public MongoCluster withReadConcern(ReadConcern readConcern) {
        return getMongoClient().withReadConcern(readConcern);
    }

    @Override
    public MongoCluster withTimeout(long timeout, TimeUnit timeUnit) {
        return getMongoClient().withTimeout(timeout, timeUnit);
    }
}
