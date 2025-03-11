package org.qubership.cloud.core.quarkus.dbaas.datasource.service.agroal.auth;

import io.agroal.api.security.AgroalDefaultSecurityProvider;
import io.agroal.api.security.SimplePassword;

import java.util.Properties;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * DbaasSecurityProvider holds current database password that can be updated from client code in thread safe manner
 */
public class DbaasSecurityProvider extends AgroalDefaultSecurityProvider {
    private final Lock lock = new ReentrantLock();

    private SimplePassword currentDatabasePassword;

    public DbaasSecurityProvider(SimplePassword currentDatabasePassword) {
        this.currentDatabasePassword = currentDatabasePassword;
    }

    @Override
    public Properties getSecurityProperties(Object securityObject) {
        this.lock.lock();
        try {
            if (securityObject instanceof SimplePassword) {
                return currentDatabasePassword.asProperties();
            }
        } finally {
            this.lock.unlock();
        }
        return super.getSecurityProperties(securityObject);
    }

    public void setCurrentDatabasePassword(SimplePassword currentDatabasePassword) {
        this.lock.lock();
        this.currentDatabasePassword = currentDatabasePassword;
        this.lock.unlock();
    }
}
