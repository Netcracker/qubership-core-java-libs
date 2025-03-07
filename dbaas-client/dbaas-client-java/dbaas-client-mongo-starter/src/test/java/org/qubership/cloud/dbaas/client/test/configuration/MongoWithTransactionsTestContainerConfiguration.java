package org.qubership.cloud.dbaas.client.test.configuration;

import org.qubership.cloud.dbaas.client.test.container.MongoWithTransactionsTestContainer;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class MongoWithTransactionsTestContainerConfiguration {
    MongoWithTransactionsTestContainer container;

    @Bean
    @Primary
    @Qualifier("mongoWithTransactionsContainer")
    public MongoWithTransactionsTestContainer getContainer() {
        container = MongoWithTransactionsTestContainer.getInstance();
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
