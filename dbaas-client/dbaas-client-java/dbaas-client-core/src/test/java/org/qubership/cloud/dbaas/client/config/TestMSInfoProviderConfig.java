package org.qubership.cloud.dbaas.client.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class TestMSInfoProviderConfig {
    @Bean
    @Primary
    public MSInfoProvider testMSInfoProvider() {
        return new DefaultMSInfoProvider();
    }
}
