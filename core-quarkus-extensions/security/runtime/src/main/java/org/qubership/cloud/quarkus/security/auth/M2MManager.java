package org.qubership.cloud.quarkus.security.auth;

import lombok.extern.slf4j.Slf4j;
import org.qubership.cloud.security.core.auth.DummyM2MManager;
import org.qubership.cloud.security.core.auth.Token;

import java.util.Comparator;
import java.util.ServiceLoader;

@Slf4j
public class M2MManager implements org.qubership.cloud.security.core.auth.M2MManager {
    private static M2MManager instance = null;
    private final org.qubership.cloud.security.core.auth.M2MManager delegate;

    M2MManager() {
        delegate = ServiceLoader.load(org.qubership.cloud.security.core.auth.M2MManager.class)
                .stream()
                .map(ServiceLoader.Provider::get)
                .max(Comparator.comparingInt(org.qubership.cloud.security.core.auth.M2MManager::priority))
                .orElseGet(() -> {
                    log.warn("Use dummy M2MManager instead. Consider using real M2MManager implementation in production instead.");
                    return new DummyM2MManager();
                });
        log.info("Resolved M2MManager delegate is {}", delegate.getClass().getSimpleName());
    }

    public static org.qubership.cloud.security.core.auth.M2MManager getInstance() {
        if (instance == null) {
            synchronized (M2MManager.class) {
                if (instance == null) {
                    instance = new M2MManager();
                }
            }
        }

        return instance;
    }


    @Override
    public Token getToken() {
        return delegate.getToken();
    }
}