package com.netcracker.cloud.quarkus.security.auth;

import com.netcracker.cloud.security.core.auth.DummyM2MManager;
import com.netcracker.cloud.security.core.auth.Token;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;
import java.util.ServiceLoader;

@Slf4j
public class M2MManager implements com.netcracker.cloud.security.core.auth.M2MManager {
    private static M2MManager instance = null;
    private final com.netcracker.cloud.security.core.auth.M2MManager delegate;

    M2MManager() {
        delegate = loadPreferred();
    }

    // local copy: M2MManager.loadPreferred() exists in tls-utils only since 3.2.8
    private static com.netcracker.cloud.security.core.auth.M2MManager loadPreferred() {
        com.netcracker.cloud.security.core.auth.M2MManager manager = ServiceLoader.load(com.netcracker.cloud.security.core.auth.M2MManager.class)
                .stream()
                .map(ServiceLoader.Provider::get)
                .max(Comparator.comparingInt(com.netcracker.cloud.security.core.auth.M2MManager::priority))
                .orElseGet(DummyM2MManager::new);
        if (manager instanceof DummyM2MManager) {
            log.warn("Using the dummy token for M2M authorization: no M2MManager implementation is registered,"
                    + " register one in production");
        } else {
            log.info("Using {} for M2M authorization", manager.getClass().getSimpleName());
        }
        return manager;
    }

    public static com.netcracker.cloud.security.core.auth.M2MManager getInstance() {
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
