package com.netcracker.cloud.quarkus.dbaas.cassandraclient;

import com.datastax.oss.driver.api.core.AllNodesFailedException;
import com.datastax.oss.driver.api.core.CqlIdentifier;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.context.DriverContext;
import com.datastax.oss.driver.api.core.metadata.Metadata;
import com.datastax.oss.driver.api.core.metrics.Metrics;
import com.datastax.oss.driver.api.core.session.Request;
import com.datastax.oss.driver.api.core.type.reflect.GenericType;
import com.netcracker.cloud.dbaas.client.cassandra.entity.database.CassandraDatabase;
import com.netcracker.cloud.dbaas.client.management.classifier.DbaaSClassifierBuilder;
import com.netcracker.cloud.quarkus.dbaas.cassandraclient.service.CassandraClientCreation;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

@Slf4j
public class DbaaSCassandraClient implements CqlSession {
    private CassandraClientCreation cassandraClientCreation;
    private DbaaSClassifierBuilder classifierBuilder;

    public DbaaSCassandraClient(DbaaSClassifierBuilder classifierBuilder, CassandraClientCreation cassandraClientCreation) {
        this.cassandraClientCreation = cassandraClientCreation;
        this.classifierBuilder = classifierBuilder;
    }

    CassandraDatabase getOrCreateCassandraDatabase() {
        return cassandraClientCreation.getOrCreateCassandraDatabase(classifierBuilder.build());
    }

    private <T> T withReconnect(Function<CqlSession, T> operation) {
        try {
            return operation.apply(getOrCreateCassandraDatabase().getConnectionProperties().getSession());
        } catch (AllNodesFailedException e) {
            log.info("Cassandra operation failed with AllNodesFailedException, evicting stale session and retrying.", e);
            cassandraClientCreation.evictCassandraDatabase(classifierBuilder.build());
            return operation.apply(getOrCreateCassandraDatabase().getConnectionProperties().getSession());
        }
    }

    @Override
    public String getName() {
        return withReconnect(CqlSession::getName);
    }

    @Override
    public Metadata getMetadata() {
        return withReconnect(CqlSession::getMetadata);
    }

    @Override
    public boolean isSchemaMetadataEnabled() {
        return withReconnect(CqlSession::isSchemaMetadataEnabled);
    }

    @Override
    public CompletionStage<Metadata> setSchemaMetadataEnabled(Boolean aBoolean) {
        return withReconnect(s -> s.setSchemaMetadataEnabled(aBoolean));
    }

    @Override
    public CompletionStage<Metadata> refreshSchemaAsync() {
        return withReconnect(CqlSession::refreshSchemaAsync);
    }

    @Override
    public CompletionStage<Boolean> checkSchemaAgreementAsync() {
        return withReconnect(CqlSession::checkSchemaAgreementAsync);
    }

    @Override
    public DriverContext getContext() {
        return withReconnect(CqlSession::getContext);
    }

    @Override
    public Optional<CqlIdentifier> getKeyspace() {
        return withReconnect(CqlSession::getKeyspace);
    }

    @Override
    public Optional<Metrics> getMetrics() {
        return withReconnect(CqlSession::getMetrics);
    }

    @Override
    public <R extends Request, T> T execute(R request, GenericType<T> genericType) {
        return withReconnect(s -> s.execute(request, genericType));
    }

    @Override
    public CompletionStage<Void> closeFuture() {
        return withReconnect(CqlSession::closeFuture);
    }

    @Override
    public CompletionStage<Void> closeAsync() {
        return withReconnect(CqlSession::closeAsync);
    }

    @Override
    public CompletionStage<Void> forceCloseAsync() {
        return withReconnect(CqlSession::forceCloseAsync);
    }
}
