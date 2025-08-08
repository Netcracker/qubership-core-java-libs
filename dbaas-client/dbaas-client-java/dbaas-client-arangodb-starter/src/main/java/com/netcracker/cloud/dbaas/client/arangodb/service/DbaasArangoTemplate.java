package org.qubership.cloud.dbaas.client.arangodb.service;

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
import org.qubership.cloud.dbaas.client.arangodb.configuration.DbaasArangoDBConfigurationProperties;
import org.qubership.cloud.dbaas.client.management.ArangoDatabaseProvider;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.dao.DataAccessException;

import java.util.Map;
import java.util.Optional;

@Slf4j
public class DbaasArangoTemplate extends ArangoTemplate {

    private final ArangoDatabaseProvider arangoDatabaseProvider;
    private final ArangoConverter arangoConverter;
    private final ResolverFactory resolverFactory;
    private final DbaasArangoDBConfigurationProperties dbaasArangoConfig;
    private final ApplicationContext applicationContext;

    private String dbName;
    private ArangoTemplate arangoTemplate;

    public DbaasArangoTemplate(ArangoDatabaseProvider arangoDatabaseProvider,
                               ArangoConverter arangoConverter,
                               ResolverFactory resolverFactory,
                               DbaasArangoDBConfigurationProperties dbaasArangoConfig,
                               ApplicationContext applicationContext) {
        super(null, "", null, null);
        this.applicationContext = applicationContext;
        this.arangoDatabaseProvider = arangoDatabaseProvider;
        this.arangoConverter = arangoConverter;
        this.resolverFactory = resolverFactory;
        this.dbaasArangoConfig = dbaasArangoConfig;
    }

    @Override
    public ArangoDB driver() {
        return getArangoTemplate().driver();
    }

    @Override
    public ArangoDBVersion getVersion() throws DataAccessException {
        return getArangoTemplate().getVersion();
    }

    @Override
    public <T> ArangoCursor<T> query(String query, Map<String, Object> bindVars, AqlQueryOptions options, Class<T> entityClass) throws DataAccessException {
        return getArangoTemplate().query(query, bindVars, options, entityClass);
    }

    @Override
    public <T> ArangoCursor<T> query(String query, Map<String, Object> bindVars, Class<T> entityClass) throws DataAccessException {
        return getArangoTemplate().query(query, bindVars, entityClass);
    }

    @Override
    public <T> ArangoCursor<T> query(String query, AqlQueryOptions options, Class<T> entityClass) throws DataAccessException {
        return getArangoTemplate().query(query, options, entityClass);
    }

    @Override
    public <T> ArangoCursor<T> query(String query, Class<T> entityClass) throws DataAccessException {
        return getArangoTemplate().query(query, entityClass);
    }

    @Override
    public <T> MultiDocumentEntity<DocumentDeleteEntity<T>> deleteAll(Iterable<?> values, DocumentDeleteOptions options, Class<T> entityClass) throws DataAccessException {
        return getArangoTemplate().deleteAll(values, options, entityClass);
    }

    @Override
    public MultiDocumentEntity<DocumentDeleteEntity<?>> deleteAll(Iterable<?> values, Class<?> entityClass) throws DataAccessException {
        return getArangoTemplate().deleteAll(values, entityClass);
    }

    @Override
    public <T> MultiDocumentEntity<DocumentDeleteEntity<T>> deleteAllById(Iterable<?> ids, DocumentDeleteOptions options, Class<T> entityClass) throws DataAccessException {
        return getArangoTemplate().deleteAllById(ids, options, entityClass);
    }

    @Override
    public MultiDocumentEntity<DocumentDeleteEntity<?>> deleteAllById(Iterable<?> ids, Class<?> entityClass) throws DataAccessException {
        return getArangoTemplate().deleteAllById(ids, entityClass);
    }

    @Override
    public <T> DocumentDeleteEntity<T> delete(Object id, DocumentDeleteOptions options, Class<T> entityClass) throws DataAccessException {
        return getArangoTemplate().delete(id, options, entityClass);
    }

    @Override
    public DocumentDeleteEntity<?> delete(Object id, Class<?> entityClass) throws DataAccessException {
        return getArangoTemplate().delete(id, entityClass);
    }

    @Override
    public <T> MultiDocumentEntity<DocumentUpdateEntity<T>> updateAll(Iterable<? extends T> values, DocumentUpdateOptions options, Class<T> entityClass) throws DataAccessException {
        return getArangoTemplate().updateAll(values, options, entityClass);
    }

    @Override
    public <T> MultiDocumentEntity<DocumentUpdateEntity<?>> updateAll(Iterable<? extends T> values, Class<T> entityClass) throws DataAccessException {
        return getArangoTemplate().updateAll(values, entityClass);
    }

    @Override
    public <T> DocumentUpdateEntity<T> update(Object id, T value, DocumentUpdateOptions options) throws DataAccessException {
        return getArangoTemplate().update(id, value, options);
    }

    @Override
    public DocumentUpdateEntity<?> update(Object id, Object value) throws DataAccessException {
        return getArangoTemplate().update(id, value);
    }

    @Override
    public <T> MultiDocumentEntity<DocumentUpdateEntity<T>> replaceAll(Iterable<? extends T> values, DocumentReplaceOptions options, Class<T> entityClass) throws DataAccessException {
        return getArangoTemplate().replaceAll(values, options, entityClass);
    }

    @Override
    public <T> MultiDocumentEntity<DocumentUpdateEntity<?>> replaceAll(Iterable<? extends T> values, Class<T> entityClass) throws DataAccessException {
        return getArangoTemplate().replaceAll(values, entityClass);
    }


    @Override
    public <T> DocumentUpdateEntity<T> replace(Object id, T value, DocumentReplaceOptions options) throws DataAccessException {
        return getArangoTemplate().replace(id, value, options);
    }

    @Override
    public DocumentUpdateEntity<?> replace(Object id, Object value) throws DataAccessException {
        return getArangoTemplate().replace(id, value);
    }

    @Override
    public <T> Optional<T> find(Object id, Class<T> entityClass, DocumentReadOptions options) throws DataAccessException {
        return getArangoTemplate().find(id, entityClass, options);
    }

    @Override
    public <T> Optional<T> find(Object id, Class<T> entityClass) throws DataAccessException {
        return getArangoTemplate().find(id, entityClass);
    }

    @Override
    public <T> Iterable<T> findAll(Class<T> entityClass) throws DataAccessException {
        return getArangoTemplate().findAll(entityClass);
    }

    @Override
    public <T> Iterable<T> findAll(Iterable<?> ids, Class<T> entityClass) throws DataAccessException {
        return getArangoTemplate().findAll(ids, entityClass);
    }

    @Override
    public <T> MultiDocumentEntity<DocumentCreateEntity<T>> insertAll(Iterable<? extends T> values, DocumentCreateOptions options, Class<T> entityClass) throws DataAccessException {
        return getArangoTemplate().insertAll(values, options, entityClass);
    }

    @Override
    public <T> MultiDocumentEntity<DocumentCreateEntity<?>> insertAll(Iterable<? extends T> values, Class<T> entityClass) throws DataAccessException {
        return getArangoTemplate().insertAll(values, entityClass);
    }

    @Override
    public <T> DocumentCreateEntity<T> insert(T value, DocumentCreateOptions options) throws DataAccessException {
        return getArangoTemplate().insert(value, options);
    }

    @Override
    public DocumentCreateEntity<?> insert(Object value) throws DataAccessException {
        return getArangoTemplate().insert(value);
    }

    @Override
    public <T> T repsert(T value) throws DataAccessException {
        return getArangoTemplate().repsert(value);
    }

    @Override
    public <T> Iterable<T> repsertAll(Iterable<T> values, Class<? super T> entityClass) throws DataAccessException {
        return getArangoTemplate().repsertAll(values, entityClass);
    }

    @Override
    public boolean exists(Object id, Class<?> entityClass) throws DataAccessException {
        return getArangoTemplate().exists(id, entityClass);
    }

    @Override
    public void dropDatabase() throws DataAccessException {
        getArangoTemplate().dropDatabase();
    }

    @Override
    public CollectionOperations collection(Class<?> entityClass) throws DataAccessException {
        return getArangoTemplate().collection(entityClass);
    }

    @Override
    public CollectionOperations collection(String name) throws DataAccessException {
        return getArangoTemplate().collection(name);
    }

    @Override
    public CollectionOperations collection(String name, CollectionCreateOptions options) throws DataAccessException {
        return getArangoTemplate().collection(name, options);
    }

    @Override
    public UserOperations user(String username) {
        return getArangoTemplate().user(username);
    }

    @Override
    public Iterable<UserEntity> getUsers() throws DataAccessException {
        return getArangoTemplate().getUsers();
    }

    @Override
    public ArangoConverter getConverter() {
        return getArangoTemplate().getConverter();
    }

    @Override
    public ResolverFactory getResolverFactory() {
        return getArangoTemplate().getResolverFactory();
    }

    @Override
    public RuntimeException translateException(RuntimeException e) {
        return getArangoTemplate().translateException(e);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    }

    protected ArangoTemplate getArangoTemplate() {
        if (arangoTemplate == null || !checkConnection(arangoTemplate)) {
            log.warn("Arango connection check failed. Will attempt to create new connection.");
            initArangoTemplate();
        }
        return arangoTemplate;
    }

    protected boolean checkConnection(ArangoOperations operations) {
        try {
            Integer checkValue;
            try (ArangoCursor<Integer> query = operations.query("RETURN 42", Integer.class)) {
                checkValue = query.next();
                if (checkValue == null || checkValue != 42) throw new RuntimeException("Wrong check query result: " + checkValue);
            }
            log.debug("Connection check succeeded, check value: {}", checkValue);
        } catch (Exception e) {
            log.debug("Connection check failed with exception", e);
            return false;
        }
        return true;
    }

    protected void initArangoTemplate() {
        ArangoDatabase arangoDatabase = arangoDatabaseProvider.provide(dbaasArangoConfig.getArangodb().getOrDefault("dbId", "default"));
        dbName = arangoDatabase.name();
        arangoTemplate = new ArangoTemplate(arangoDatabase.arango(), dbName, arangoConverter, resolverFactory);
        arangoTemplate.setApplicationContext(applicationContext);
    }
}
