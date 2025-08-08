package com.netcracker.cloud.dbaas.client.config.container;

import jakarta.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class CassandraTestContainerConfiguration {
    CassandraTestContainer container;

    @Bean
    @Primary
    @Qualifier("cassandraContainer")
    public CassandraTestContainer getContainer() {
        container = CassandraTestContainer.getInstance();
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
