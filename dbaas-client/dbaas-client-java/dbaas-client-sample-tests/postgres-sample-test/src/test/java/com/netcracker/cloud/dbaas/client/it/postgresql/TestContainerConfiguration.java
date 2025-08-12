package com.netcracker.cloud.dbaas.client.it.postgresql;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import jakarta.annotation.PreDestroy;

@Configuration
public class TestContainerConfiguration {
    PostgresqlContainerConfiguration container;
    @Bean
    @Primary
    @Qualifier("pgContainer")
    public PostgresqlContainerConfiguration getContainer() {
        container = PostgresqlContainerConfiguration.getInstance();
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
