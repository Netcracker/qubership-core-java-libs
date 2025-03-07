package org.qubership.cloud.dbaas.client.it.mongodb;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import jakarta.annotation.PreDestroy;

@Configuration
public class MongoTestContainerConfiguration {
    MongoTestContainer container;
    @Bean
    @Primary
    @Qualifier("mongoContainer")
    public MongoTestContainer getContainer() {
        container = MongoTestContainer.getInstance();
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
