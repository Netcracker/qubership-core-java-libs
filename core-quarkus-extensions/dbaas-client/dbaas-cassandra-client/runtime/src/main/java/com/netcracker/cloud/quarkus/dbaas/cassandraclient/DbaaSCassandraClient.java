package com.netcracker.cloud.quarkus.dbaas.cassandraclient;

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

import java.util.Optional;
import java.util.concurrent.CompletionStage;

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

    private CqlSession getCassandraClient() {
        return getOrCreateCassandraDatabase().getConnectionProperties().getSession();
    }

    @Override
    public String getName() {
        return getCassandraClient().getName();
    }

    @Override
    public Metadata getMetadata() {
        return getCassandraClient().getMetadata();
    }

    @Override
    public boolean isSchemaMetadataEnabled() {
        return getCassandraClient().isSchemaMetadataEnabled();
    }

    @Override
    public CompletionStage<Metadata> setSchemaMetadataEnabled(Boolean aBoolean) {
        return getCassandraClient().setSchemaMetadataEnabled(aBoolean);
    }

    @Override
    public CompletionStage<Metadata> refreshSchemaAsync() {
        return getCassandraClient().refreshSchemaAsync();
    }

    @Override
    public CompletionStage<Boolean> checkSchemaAgreementAsync() {
        return getCassandraClient().checkSchemaAgreementAsync();
    }

    @Override
    public DriverContext getContext() {
        return getCassandraClient().getContext();
    }

    @Override
    public Optional<CqlIdentifier> getKeyspace() {
        return getCassandraClient().getKeyspace();
    }

    @Override
    public Optional<Metrics> getMetrics() {
        return getCassandraClient().getMetrics();
    }

    @Override
    public <R extends Request, T> T execute(R request, GenericType<T> genericType) {
        return getCassandraClient().execute(request, genericType);
    }

    @Override
    public CompletionStage<Void> closeFuture() {
        return getCassandraClient().closeFuture();
    }

    @Override
    public CompletionStage<Void> closeAsync() {
        return getCassandraClient().closeAsync();
    }

    @Override
    public CompletionStage<Void> forceCloseAsync() {
        return getCassandraClient().forceCloseAsync();
    }
}
