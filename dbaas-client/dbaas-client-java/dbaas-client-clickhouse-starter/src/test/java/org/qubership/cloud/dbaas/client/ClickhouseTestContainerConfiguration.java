package org.qubership.cloud.dbaas.client;

import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.testcontainers.clickhouse.ClickHouseContainer;

@Configuration
public class ClickhouseTestContainerConfiguration {
    ClickHouseContainer container;

    @Bean
    @Primary
    @Qualifier("clickhouseContainer")
    public ClickHouseContainer getContainer() {
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