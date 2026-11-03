package com.netcracker.cloud.security.core.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.ServiceLoader;

public interface M2MManager {
    Token getToken();

    default int priority() {
        return 0;
    }

    static M2MManager loadPreferred() {
        M2MManager manager = ServiceLoader.load(M2MManager.class).stream()
                .map(ServiceLoader.Provider::get)
                .max(Comparator.comparingInt(M2MManager::priority))
                .orElseGet(DummyM2MManager::new);
        Logger log = LoggerFactory.getLogger(M2MManager.class);
        if (manager instanceof DummyM2MManager) {
            log.warn("Using the dummy token for M2M authorization: no M2MManager implementation is registered,"
                    + " register one in production");
        } else {
            log.info("Using {} for M2M authorization", manager.getClass().getSimpleName());
        }
        return manager;
    }
}
