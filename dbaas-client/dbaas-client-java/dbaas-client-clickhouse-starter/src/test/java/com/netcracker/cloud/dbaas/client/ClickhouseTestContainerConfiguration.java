package com.netcracker.cloud.dbaas.client;

import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.testcontainers.containers.GenericContainer;

@Configuration
public class ClickhouseTestContainerConfiguration {
    GenericContainer<?> container;

    @Bean
    @Primary
    @Qualifier("clickhouseContainer")
    public GenericContainer<?> getContainer() {
        container = ClickhouseTestContainer.getInstance();
        container.start();
        return container;
    }

    @PreDestroy
    public void close() {
        if (container.isRunning()) {
            container.stop();
        }
    }
}
