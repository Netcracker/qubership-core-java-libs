package org.qubership.cloud.security.common;

import org.qubership.cloud.security.core.auth.DummyM2MManager;
import org.qubership.cloud.security.core.auth.M2MManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@Configurable
public class DummyM2MManagerConfiguration {
    private static final Logger log = LoggerFactory.getLogger(DummyM2MManagerConfiguration.class);

    @Bean
    @ConditionalOnMissingBean(M2MManager.class)
    public M2MManager m2mManager() {
        log.warn("Initialized dummy m2m manager. Consider using real m2m manager in production environment");
        return new DummyM2MManager();
    }
}
