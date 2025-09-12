package com.netcracker.cloud.quarkus.dbaas.mongoclient;

import com.mongodb.*;
import com.mongodb.client.*;
import com.mongodb.client.model.bulk.ClientBulkWriteOptions;
import com.mongodb.client.model.bulk.ClientBulkWriteResult;
import com.mongodb.client.model.bulk.ClientNamespacedWriteModel;
import com.mongodb.connection.ClusterDescription;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Priority(1)
@Alternative
@ApplicationScoped
@Slf4j
public class MongoClientAggregator implements MongoClient {

    @Inject
    AnnotationParsingBean helper;

    @Inject
    @Named("serviceMongoClient")
    public MongoClient serviceMongoClient;

    @Inject
    @Named("tenantMongoClient")
    public MongoClient tenantMongoClient;

    @ConfigProperty(name = "quarkus.dbaas.mongodb.main-type", defaultValue = "tenant")
    String dbaasDsMainType;

    public boolean isServiceDb() {
        return !dbaasDsMainType.equalsIgnoreCase("tenant");
    }

    @Override
    public CodecRegistry getCodecRegistry() {
        return isServiceDb() ? serviceMongoClient.getCodecRegistry() : tenantMongoClient.getCodecRegistry();
    }

    @Override
    public ReadPreference getReadPreference() {
        return isServiceDb() ? serviceMongoClient.getReadPreference() : tenantMongoClient.getReadPreference();
    }

    @Override
    public WriteConcern getWriteConcern() {
        return isServiceDb() ? serviceMongoClient.getWriteConcern() : tenantMongoClient.getWriteConcern();
    }

    @Override
    public ReadConcern getReadConcern() {
        return isServiceDb() ? serviceMongoClient.getReadConcern() : tenantMongoClient.getReadConcern();
    }

    @Override
    public Long getTimeout(TimeUnit timeUnit) {
        return isServiceDb() ? serviceMongoClient.getTimeout(timeUnit) : tenantMongoClient.getTimeout(timeUnit);
    }

    @Override
    public MongoCluster withCodecRegistry(CodecRegistry codecRegistry) {
        return isServiceDb() ? serviceMongoClient.withCodecRegistry(codecRegistry) : tenantMongoClient.withCodecRegistry(codecRegistry);
    }

    @Override
    public MongoCluster withReadPreference(ReadPreference readPreference) {
        return isServiceDb() ? serviceMongoClient.withReadPreference(readPreference) : tenantMongoClient.withReadPreference(readPreference);
    }

    @Override
    public MongoCluster withWriteConcern(WriteConcern writeConcern) {
        return isServiceDb() ? serviceMongoClient.withWriteConcern(writeConcern) : tenantMongoClient.withWriteConcern(writeConcern);
    }

    @Override
    public MongoCluster withReadConcern(ReadConcern readConcern) {
        return isServiceDb() ? serviceMongoClient.withReadConcern(readConcern) : tenantMongoClient.withReadConcern(readConcern);
    }

    @Override
    public MongoCluster withTimeout(long l, TimeUnit timeUnit) {
        return isServiceDb() ? serviceMongoClient.withTimeout(l, timeUnit) : tenantMongoClient.withTimeout(l, timeUnit);
    }

    @Override
    public MongoDatabase getDatabase(String s) {
        return isServiceDb() ? serviceMongoClient.getDatabase(s) : tenantMongoClient.getDatabase(s);
    }

    @Override
    public ClientSession startSession() {
        return isServiceDb() ? serviceMongoClient.startSession() : tenantMongoClient.startSession();
    }

    @Override
    public ClientSession startSession(ClientSessionOptions clientSessionOptions) {
        return isServiceDb() ? serviceMongoClient.startSession() : tenantMongoClient.startSession();
    }

    @Override
    public void close() {
        if (isServiceDb()) {
            serviceMongoClient.close();
        } else {
            tenantMongoClient.close();
        }

    }

    @Override
    public MongoIterable<String> listDatabaseNames() {
        return isServiceDb() ? serviceMongoClient.listDatabaseNames() : tenantMongoClient.listDatabaseNames();
    }

    @Override
    public MongoIterable<String> listDatabaseNames(ClientSession clientSession) {
        return isServiceDb() ? serviceMongoClient.listDatabaseNames(clientSession)
                : tenantMongoClient.listDatabaseNames(clientSession);
    }

    @Override
    public ListDatabasesIterable<Document> listDatabases() {
        return isServiceDb() ? serviceMongoClient.listDatabases() : tenantMongoClient.listDatabases();
    }

    @Override
    public ListDatabasesIterable<Document> listDatabases(ClientSession clientSession) {
        return isServiceDb() ? serviceMongoClient.listDatabases(clientSession)
                : tenantMongoClient.listDatabases(clientSession);
    }

    @Override
    public <TResult> ListDatabasesIterable<TResult> listDatabases(Class<TResult> aClass) {
        return isServiceDb() ? serviceMongoClient.listDatabases(aClass) : tenantMongoClient.listDatabases(aClass);
    }

    @Override
    public <TResult> ListDatabasesIterable<TResult> listDatabases(ClientSession clientSession, Class<TResult> aClass) {
        return isServiceDb() ? serviceMongoClient.listDatabases(clientSession, aClass)
                : tenantMongoClient.listDatabases(clientSession, aClass);

    }

    @Override
    public ChangeStreamIterable<Document> watch() {
        return isServiceDb() ? serviceMongoClient.watch() : tenantMongoClient.watch();

    }

    @Override
    public <TResult> ChangeStreamIterable<TResult> watch(Class<TResult> aClass) {
        return isServiceDb() ? serviceMongoClient.watch(aClass) : tenantMongoClient.watch(aClass);
    }

    @Override
    public ChangeStreamIterable<Document> watch(List<? extends Bson> list) {
        return isServiceDb() ? serviceMongoClient.watch(list) : tenantMongoClient.watch(list);
    }

    @Override
    public <TResult> ChangeStreamIterable<TResult> watch(List<? extends Bson> list, Class<TResult> aClass) {
        return isServiceDb() ? serviceMongoClient.watch(list, aClass) : tenantMongoClient.watch(list, aClass);
    }

    @Override
    public ChangeStreamIterable<Document> watch(ClientSession clientSession) {
        return isServiceDb() ? serviceMongoClient.watch(clientSession) : tenantMongoClient.watch(clientSession);
    }

    @Override
    public <TResult> ChangeStreamIterable<TResult> watch(ClientSession clientSession, Class<TResult> aClass) {
        return isServiceDb() ? serviceMongoClient.watch(clientSession, aClass)
                : tenantMongoClient.watch(clientSession, aClass);
    }

    @Override
    public ChangeStreamIterable<Document> watch(ClientSession clientSession, List<? extends Bson> list) {
        return isServiceDb() ? serviceMongoClient.watch(clientSession, list)
                : tenantMongoClient.watch(clientSession, list);
    }

    @Override
    public <TResult> ChangeStreamIterable<TResult> watch(ClientSession clientSession, List<? extends Bson> list, Class<TResult> aClass) {
        return isServiceDb() ? serviceMongoClient.watch(clientSession, list, aClass)
                : tenantMongoClient.watch(clientSession, list, aClass);
    }

    @Override
    public ClientBulkWriteResult bulkWrite(List<? extends ClientNamespacedWriteModel> list) throws ClientBulkWriteException {
        return isServiceDb() ? serviceMongoClient.bulkWrite(list) : tenantMongoClient.bulkWrite(list);
    }

    @Override
    public ClientBulkWriteResult bulkWrite(List<? extends ClientNamespacedWriteModel> list, ClientBulkWriteOptions clientBulkWriteOptions) throws ClientBulkWriteException {
        return isServiceDb() ? serviceMongoClient.bulkWrite(list, clientBulkWriteOptions) : tenantMongoClient.bulkWrite(list, clientBulkWriteOptions);
    }

    @Override
    public ClientBulkWriteResult bulkWrite(ClientSession clientSession, List<? extends ClientNamespacedWriteModel> list) throws ClientBulkWriteException {
        return isServiceDb() ? serviceMongoClient.bulkWrite(clientSession, list) : tenantMongoClient.bulkWrite(clientSession, list);
    }

    @Override
    public ClientBulkWriteResult bulkWrite(ClientSession clientSession, List<? extends ClientNamespacedWriteModel> list, ClientBulkWriteOptions clientBulkWriteOptions) throws ClientBulkWriteException {
        return isServiceDb() ? serviceMongoClient.bulkWrite(clientSession, list, clientBulkWriteOptions) : tenantMongoClient.bulkWrite(clientSession, list, clientBulkWriteOptions);
    }

    @Override
    public ClusterDescription getClusterDescription() {
        return isServiceDb() ? serviceMongoClient.getClusterDescription()
                : tenantMongoClient.getClusterDescription();
    }
}
