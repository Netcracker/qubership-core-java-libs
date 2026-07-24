package com.netcracker.cloud.dbaas.client.arangodb.service;

import com.arangodb.ArangoCursor;
import com.arangodb.ArangoDB;
import com.arangodb.ArangoDatabase;
import com.arangodb.entity.*;
import com.arangodb.model.*;
import com.arangodb.springframework.core.ArangoOperations;
import com.arangodb.springframework.core.CollectionOperations;
import com.arangodb.springframework.core.UserOperations;
import com.arangodb.springframework.core.convert.ArangoConverter;
import com.arangodb.springframework.core.convert.resolver.ResolverFactory;
import com.arangodb.springframework.core.template.ArangoTemplate;
import com.netcracker.cloud.dbaas.client.arangodb.configuration.DbaasArangoDBConfigurationProperties;
import com.netcracker.cloud.dbaas.client.management.ArangoDatabaseProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.dao.DataAccessException;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

@Slf4j
public class DbaasArangoTemplate extends ArangoTemplate {

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);
    // Bounded variant of the injected provider: initArangoTemplate() always runs under this
    // template's write lock (both on first init and on recovery below), so every other operation
    // queues behind it. Reconnecting must fail fast here regardless of the retry policy the
    // caller configured for direct ArangoDatabaseProvider.provide() calls elsewhere.
    private final ArangoDatabaseProvider recoveryArangoDatabaseProvider;
    private final ArangoConverter arangoConverter;
    private final ResolverFactory resolverFactory;
    private final DbaasArangoDBConfigurationProperties dbaasArangoConfig;
    private final ApplicationContext applicationContext;

    private final AtomicReference<ArangoTemplate> arangoTemplateRef = new AtomicReference<>();
    private volatile String databaseName;

    public DbaasArangoTemplate(ArangoDatabaseProvider arangoDatabaseProvider,
                               ArangoConverter arangoConverter,
                               ResolverFactory resolverFactory,
                               DbaasArangoDBConfigurationProperties dbaasArangoConfig,
                               ApplicationContext applicationContext) {
        super(null, "", null, null);
        this.applicationContext = applicationContext;
        this.recoveryArangoDatabaseProvider = arangoDatabaseProvider == null ? null : arangoDatabaseProvider.withRetryPolicy(
                0, 0, ArangoDatabaseProvider.DEFAULT_CONNECTION_CHECK_TIMEOUT_MS);
        this.arangoConverter = arangoConverter;
        this.resolverFactory = resolverFactory;
        this.dbaasArangoConfig = dbaasArangoConfig;
    }

    @Override
    public ArangoDB driver() {
        return wrapWithRetry(() -> getArangoTemplate().driver());
    }

    @Override
    public ArangoDBVersion getVersion() throws DataAccessException {
        return wrapWithRetry(() -> getArangoTemplate().getVersion());
    }

    @Override
    public <T> ArangoCursor<T> query(String query, Map<String, Object> bindVars, AqlQueryOptions options, Class<T> entityClass) throws DataAccessException {
        return wrapWithRetry(() -> getArangoTemplate().query(query, bindVars, options, entityClass));
    }

    @Override
    public <T> ArangoCursor<T> query(String query, Map<String, Object> bindVars, Class<T> entityClass) throws DataAccessException {
        return wrapWithRetry(() -> getArangoTemplate().query(query, bindVars, entityClass));
    }

    @Override
    public <T> ArangoCursor<T> query(String query, AqlQueryOptions options, Class<T> entityClass) throws DataAccessException {
        return wrapWithRetry(() -> getArangoTemplate().query(query, options, entityClass));
    }

    @Override
    public <T> ArangoCursor<T> query(String query, Class<T> entityClass) throws DataAccessException {
        return wrapWithRetry(() -> getArangoTemplate().query(query, entityClass));
    }

    @Override
    public <T> MultiDocumentEntity<DocumentDeleteEntity<T>> deleteAll(Iterable<?> values, DocumentDeleteOptions options, Class<T> entityClass) throws DataAccessException {
        return wrapWithRetry(() -> getArangoTemplate().deleteAll(values, options, entityClass));
    }

    @Override
    public MultiDocumentEntity<DocumentDeleteEntity<?>> deleteAll(Iterable<?> values, Class<?> entityClass) throws DataAccessException {
        return wrapWithRetry(() -> getArangoTemplate().deleteAll(values, entityClass));
    }

    @Override
    public <T> MultiDocumentEntity<DocumentDeleteEntity<T>> deleteAllById(Iterable<?> ids, DocumentDeleteOptions options, Class<T> entityClass) throws DataAccessException {
        return wrapWithRetry(() -> getArangoTemplate().deleteAllById(ids, options, entityClass));
    }

    @Override
    public MultiDocumentEntity<DocumentDeleteEntity<?>> deleteAllById(Iterable<?> ids, Class<?> entityClass) throws DataAccessException {
        return wrapWithRetry(() -> getArangoTemplate().deleteAllById(ids, entityClass));
    }

    @Override
    public <T> DocumentDeleteEntity<T> delete(Object id, DocumentDeleteOptions options, Class<T> entityClass) throws DataAccessException {
        return wrapWithRetry(() -> getArangoTemplate().delete(id, options, entityClass));
    }

    @Override
    public DocumentDeleteEntity<?> delete(Object id, Class<?> entityClass) throws DataAccessException {
        return wrapWithRetry(() -> getArangoTemplate().delete(id, entityClass));
    }

    @Override
    public <T> MultiDocumentEntity<DocumentUpdateEntity<T>> updateAll(Iterable<? extends T> values, DocumentUpdateOptions options, Class<T> entityClass) throws DataAccessException {
        return wrapWithRetry(() -> getArangoTemplate().updateAll(values, options, entityClass));
    }

    @Override
    public <T> MultiDocumentEntity<DocumentUpdateEntity<?>> updateAll(Iterable<? extends T> values, Class<T> entityClass) throws DataAccessException {
        return wrapWithRetry(() -> getArangoTemplate().updateAll(values, entityClass));
    }

    @Override
    public <T> DocumentUpdateEntity<T> update(Object id, T value, DocumentUpdateOptions options) throws DataAccessException {
        return wrapWithRetry(() -> getArangoTemplate().update(id, value, options));
    }

    @Override
    public DocumentUpdateEntity<?> update(Object id, Object value) throws DataAccessException {
        return wrapWithRetry(() -> getArangoTemplate().update(id, value));
    }

    @Override
    public <T> MultiDocumentEntity<DocumentUpdateEntity<T>> replaceAll(Iterable<? extends T> values, DocumentReplaceOptions options, Class<T> entityClass) throws DataAccessException {
        return wrapWithRetry(() -> getArangoTemplate().replaceAll(values, options, entityClass));
    }

    @Override
    public <T> MultiDocumentEntity<DocumentUpdateEntity<?>> replaceAll(Iterable<? extends T> values, Class<T> entityClass) throws DataAccessException {
        return wrapWithRetry(() -> getArangoTemplate().replaceAll(values, entityClass));
    }


    @Override
    public <T> DocumentUpdateEntity<T> replace(Object id, T value, DocumentReplaceOptions options) throws DataAccessException {
        return wrapWithRetry(() -> getArangoTemplate().replace(id, value, options));
    }

    @Override
    public DocumentUpdateEntity<?> replace(Object id, Object value) throws DataAccessException {
        return wrapWithRetry(() -> getArangoTemplate().replace(id, value));
    }

    @Override
    public <T> Optional<T> find(Object id, Class<T> entityClass, DocumentReadOptions options) throws DataAccessException {
        return wrapWithRetry(() -> getArangoTemplate().find(id, entityClass, options));
    }

    @Override
    public <T> Optional<T> find(Object id, Class<T> entityClass) throws DataAccessException {
        return wrapWithRetry(() -> getArangoTemplate().find(id, entityClass));
    }

    @Override
    public <T> Iterable<T> findAll(Class<T> entityClass) throws DataAccessException {
        return wrapWithRetry(() -> getArangoTemplate().findAll(entityClass));
    }

    @Override
    public <T> Iterable<T> findAll(Iterable<?> ids, Class<T> entityClass) throws DataAccessException {
        return wrapWithRetry(() -> getArangoTemplate().findAll(ids, entityClass));
    }

    @Override
    public <T> MultiDocumentEntity<DocumentCreateEntity<T>> insertAll(Iterable<? extends T> values, DocumentCreateOptions options, Class<T> entityClass) throws DataAccessException {
        return wrapWithRetry(() -> getArangoTemplate().insertAll(values, options, entityClass));
    }

    @Override
    public <T> MultiDocumentEntity<DocumentCreateEntity<?>> insertAll(Iterable<? extends T> values, Class<T> entityClass) throws DataAccessException {
        return wrapWithRetry(() -> getArangoTemplate().insertAll(values, entityClass));
    }

    @Override
    public <T> DocumentCreateEntity<T> insert(T value, DocumentCreateOptions options) throws DataAccessException {
        return wrapWithRetry(() -> getArangoTemplate().insert(value, options));
    }

    @Override
    public DocumentCreateEntity<?> insert(Object value) throws DataAccessException {
        return wrapWithRetry(() -> getArangoTemplate().insert(value));
    }

    @Override
    public <T> T repsert(T value) throws DataAccessException {
        return wrapWithRetry(() -> getArangoTemplate().repsert(value));
    }

    @Override
    public <T> Iterable<T> repsertAll(Iterable<T> values, Class<? super T> entityClass) throws DataAccessException {
        return wrapWithRetry(() -> getArangoTemplate().repsertAll(values, entityClass));
    }

    @Override
    public boolean exists(Object id, Class<?> entityClass) throws DataAccessException {
        return wrapWithRetry(() -> getArangoTemplate().exists(id, entityClass));
    }

    @Override
    public void dropDatabase() throws DataAccessException {
        wrapWithRetry(() -> {
            getArangoTemplate().dropDatabase();
            return null;
        });
    }

    @Override
    public CollectionOperations collection(Class<?> entityClass) throws DataAccessException {
        return wrapWithRetry(() -> getArangoTemplate().collection(entityClass));
    }

    @Override
    public CollectionOperations collection(String name) throws DataAccessException {
        return wrapWithRetry(() -> getArangoTemplate().collection(name));
    }

    @Override
    public CollectionOperations collection(String name, CollectionCreateOptions options) throws DataAccessException {
        return wrapWithRetry(() -> getArangoTemplate().collection(name, options));
    }

    @Override
    public UserOperations user(String username) {
        return wrapWithRetry(() -> getArangoTemplate().user(username));
    }

    @Override
    public Iterable<UserEntity> getUsers() throws DataAccessException {
        return wrapWithRetry(() -> getArangoTemplate().getUsers());
    }

    @Override
    public ArangoConverter getConverter() {
        return wrapWithRetry(() -> getArangoTemplate().getConverter());
    }

    @Override
    public ResolverFactory getResolverFactory() {
        return wrapWithRetry(() -> getArangoTemplate().getResolverFactory());
    }

    @Override
    public RuntimeException translateException(RuntimeException e) {
        return wrapWithRetry(() -> getArangoTemplate().translateException(e));
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        // no-op: this instance's applicationContext is fixed at construction time and propagated
        // to each delegate ArangoTemplate in initArangoTemplate; the framework must not overwrite it.
    }

    private <T> T wrapWithRetry(final Supplier<T> supplier) {
        ArangoOperations currentTemplate = getArangoTemplate();
        lock.readLock().lock();
        try {
            return supplier.get();
        } catch (Exception e) {
            lock.readLock().unlock();
            lock.writeLock().lock();
            try {
                if (currentTemplate == getArangoTemplate()) {
                    log.warn("Some exception occurred during request to ArangoDB. Connection will be checked.");
                    if (checkConnection(arangoTemplateRef.get())) {
                        log.warn("Connection is ok, no recreation required.");
                        throw e;
                    }
                    log.warn("Arango connection check failed. Will attempt to create new connection.");
                    try {
                        initArangoTemplate();
                    } catch (RuntimeException recreateFailure) {
                        // e is what the request actually hit and triggered this recovery attempt;
                        // don't let it get lost behind the (possibly unrelated) recreate failure.
                        recreateFailure.addSuppressed(e);
                        throw recreateFailure;
                    }
                }
            } finally {
                lock.readLock().lock();
                lock.writeLock().unlock();
            }
            log.info("Retrying the request to ArangoDB.");
            return supplier.get();
        } finally {
            lock.readLock().unlock();
        }
    }

    protected ArangoTemplate getArangoTemplate() {
        ArangoTemplate current = arangoTemplateRef.get();
        if (current == null) {
            lock.writeLock().lock();
            try {
                current = arangoTemplateRef.get();
                if (current == null) {
                    initArangoTemplate();
                    current = arangoTemplateRef.get();
                }
            } finally {
                lock.writeLock().unlock();
            }
        }
        return current;
    }

    protected boolean checkConnection(ArangoOperations operations) {
        // Probe the same database the operations use (not _system — a tenant user may lack
        // access to it). The async API bounds our own wait; the request runs on the driver's
        // event-loop threads, so there is no app-managed thread pool to exhaust.
        return ArangoConnectionChecker.checkConnection(
                () -> operations.driver().async().db(databaseName).query("RETURN 42", Integer.class),
                dbaasArangoConfig.checkConnectionTimeoutMs());
    }

    protected void initArangoTemplate() {
        // No manual shutdown of the previous driver here: recoveryArangoDatabaseProvider.provide()
        // already evicts the stale connection through DatabasePool.removeCachedDatabase(), which
        // owns and closes it. Closing it again here risked shutting down a driver the pool still
        // considers cached (e.g. shared by another consumer of the same classifier).
        ArangoDatabase arangoDatabase = recoveryArangoDatabaseProvider.provide(dbaasArangoConfig.getArangodb().getOrDefault("dbId", "default"));
        ArangoTemplate newArangoTemplate = new ArangoTemplate(arangoDatabase.arango(), arangoDatabase.name(), arangoConverter, resolverFactory);
        newArangoTemplate.setApplicationContext(applicationContext);
        arangoTemplateRef.set(newArangoTemplate);
        databaseName = arangoDatabase.name();
    }
}
