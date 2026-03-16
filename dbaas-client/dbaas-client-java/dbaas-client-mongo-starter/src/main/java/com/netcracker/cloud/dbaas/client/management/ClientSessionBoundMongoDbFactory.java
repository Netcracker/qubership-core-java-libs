package com.netcracker.cloud.dbaas.client.management;

import com.mongodb.ClientSessionOptions;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoCollection;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.support.PersistenceExceptionTranslator;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.SessionAwareMethodInterceptor;

/**
 * {@link ClientSession} bound {@link MongoDatabaseFactory} decorating the database with a
 * {@link SessionAwareMethodInterceptor}.
 */
@AllArgsConstructor
@Getter
final class ClientSessionBoundMongoDbFactory implements MongoDatabaseFactory {

    private final ClientSession session;
    private final MongoDatabaseFactory delegate;

    @Override
    public com.mongodb.client.MongoDatabase getMongoDatabase() throws DataAccessException {
        return proxyMongoDatabase(delegate.getMongoDatabase());
    }

    @Override
    public com.mongodb.client.MongoDatabase getMongoDatabase(String dbName) throws DataAccessException {
        return proxyMongoDatabase(delegate.getMongoDatabase(dbName));
    }

    @Override
    public PersistenceExceptionTranslator getExceptionTranslator() {
        return delegate.getExceptionTranslator();
    }

    @Override
    public ClientSession getSession(ClientSessionOptions options) {
        return delegate.getSession(options);
    }

    @Override
    public MongoDatabaseFactory withSession(ClientSession session) {
        return delegate.withSession(session);
    }

    @Override
    public boolean isTransactionActive() {
        return session != null && session.hasActiveTransaction();
    }

    private com.mongodb.client.MongoDatabase proxyMongoDatabase(com.mongodb.client.MongoDatabase database) {
        return createProxyInstance(session, database, com.mongodb.client.MongoDatabase.class);
    }

    private com.mongodb.client.MongoDatabase proxyDatabase(com.mongodb.session.ClientSession session, com.mongodb.client.MongoDatabase database) {
        return createProxyInstance(session, database, com.mongodb.client.MongoDatabase.class);
    }

    private MongoCollection<?> proxyCollection(com.mongodb.session.ClientSession session, MongoCollection<?> collection) {
        return createProxyInstance(session, collection, MongoCollection.class);
    }

    private <T> T createProxyInstance(com.mongodb.session.ClientSession session, T target, Class<T> targetType) {
        ProxyFactory factory = new ProxyFactory();
        factory.setTarget(target);
        factory.setInterfaces(targetType);
        factory.setOpaque(true);

        factory.addAdvice(new SessionAwareMethodInterceptor<>(session, target, ClientSession.class, com.mongodb.client.MongoDatabase.class,
            this::proxyDatabase, MongoCollection.class, this::proxyCollection));

        return targetType.cast(factory.getProxy(target.getClass().getClassLoader()));
    }
}
