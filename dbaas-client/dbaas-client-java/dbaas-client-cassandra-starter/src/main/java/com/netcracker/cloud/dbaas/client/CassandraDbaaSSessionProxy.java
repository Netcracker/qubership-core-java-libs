package org.qubership.cloud.dbaas.client;

import com.datastax.oss.driver.api.core.AllNodesFailedException;
import com.datastax.oss.driver.api.core.CqlIdentifier;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.context.DriverContext;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.Statement;
import com.datastax.oss.driver.api.core.metadata.Metadata;
import com.datastax.oss.driver.api.core.metrics.Metrics;
import com.datastax.oss.driver.api.core.session.Request;
import com.datastax.oss.driver.api.core.type.reflect.GenericType;
import org.qubership.cloud.dbaas.client.cassandra.entity.connection.CassandraDBConnection;
import org.qubership.cloud.dbaas.client.cassandra.entity.database.type.CassandraDBType;
import org.qubership.cloud.dbaas.client.management.DatabaseConfig;
import org.qubership.cloud.dbaas.client.management.DatabasePool;
import org.qubership.cloud.dbaas.client.management.classifier.DbaaSClassifierBuilder;

import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@Slf4j
public class CassandraDbaaSSessionProxy implements CqlSession {
    private final DatabasePool pool;
    private final DbaaSClassifierBuilder builder;

    private DatabaseConfig databaseConfig;

    @FunctionalInterface
    public interface Callable<V> {
        V call();
    }

    @SneakyThrows
    private <T> T getWrappedSession(Function<CqlSession, T> func) {
        Callable<CassandraDBConnection> connectionFn = () -> pool
                .getOrCreateDatabase(CassandraDBType.INSTANCE, builder.build(), databaseConfig)
                .getConnectionProperties();

        Runnable connectionEvictFn = () -> pool.removeCachedDatabase(CassandraDBType.INSTANCE, builder.build());

        try {
            return func.apply(connectionFn.call().getSession());
        } catch (AllNodesFailedException e) {
            log.info("Operation failed. Trying recconect.", e);
            connectionEvictFn.run();
            CassandraDBConnection connection = connectionFn.call();
            return func.apply(connection.getSession());
        }
    }

    @NonNull
    @Override
    public String getName() {
        return getWrappedSession(CqlSession::getName);
    }

    @NonNull
    @Override
    public Metadata getMetadata() {
        return getWrappedSession(CqlSession::getMetadata);
    }

    @Override
    public boolean isSchemaMetadataEnabled() {
        return getWrappedSession(CqlSession::isSchemaMetadataEnabled);
    }

    @NonNull
    @Override
    public CompletionStage<Metadata> setSchemaMetadataEnabled(@Nullable Boolean newValue) {
        return getWrappedSession(s -> s.setSchemaMetadataEnabled(newValue));
    }

    @NonNull
    @Override
    public CompletionStage<Metadata> refreshSchemaAsync() {
        return getWrappedSession(CqlSession::refreshSchemaAsync);
    }

    @NonNull
    @Override
    public CompletionStage<Boolean> checkSchemaAgreementAsync() {
        return getWrappedSession(CqlSession::checkSchemaAgreementAsync);
    }

    @NonNull
    @Override
    public DriverContext getContext() {
        return getWrappedSession(CqlSession::getContext);
    }

    @NonNull
    @Override
    public Optional<CqlIdentifier> getKeyspace() {
        return getWrappedSession(CqlSession::getKeyspace);
    }

    @NonNull
    @Override
    public Optional<Metrics> getMetrics() {
        return getWrappedSession(CqlSession::getMetrics);
    }

    @Nullable
    @Override
    public <RequestT extends Request, ResultT> ResultT execute(
            @NonNull RequestT request, @NonNull GenericType<ResultT> resultType) {
        return getWrappedSession(s -> {
            try {
                return s.execute(request, resultType);
            } catch (IllegalStateException e) {
                log.error("Exception occurred during execution query", e);
                if (resultType == Statement.SYNC
                        && request instanceof BoundStatement boundStatement) {
                    // run re-prepare statement
                    s.prepare(boundStatement.getPreparedStatement().getQuery());
                    return s.execute(request, resultType);
                }
                throw e;
            }
        });
    }

    @NonNull
    @Override
    public CompletionStage<Void> closeFuture() {
        return getWrappedSession(CqlSession::closeFuture);
    }

    @NonNull
    @Override
    public CompletionStage<Void> closeAsync() {
        return getWrappedSession(CqlSession::closeAsync);
    }

    @NonNull
    @Override
    public CompletionStage<Void> forceCloseAsync() {
        return getWrappedSession(CqlSession::forceCloseAsync);
    }
}
