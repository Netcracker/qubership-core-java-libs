package org.qubership.cloud.quarkus.dbaas.mongoclient;

import com.mongodb.ClientSessionOptions;
import com.mongodb.client.ChangeStreamIterable;
import com.mongodb.client.ClientSession;
import com.mongodb.client.ListDatabasesIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoIterable;
import com.mongodb.connection.ClusterDescription;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.List;

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
    public ClusterDescription getClusterDescription() {
        return isServiceDb() ? serviceMongoClient.getClusterDescription()
                : tenantMongoClient.getClusterDescription();
    }
}
