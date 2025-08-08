package com.netcracker.cloud.microserviceframework;

import org.qubership.cloud.dbaas.client.config.EnableServiceDbaasPostgresql;
import org.qubership.cloud.restclient.MicroserviceRestClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.cloud.config.client.ConfigClientAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;

import static org.mockito.Mockito.mock;

@Configuration
@EnableServiceDbaasPostgresql
@EnableAutoConfiguration(exclude = {
        ElasticsearchDataAutoConfiguration.class,
        ConfigClientAutoConfiguration.class,
        MongoAutoConfiguration.class,
        RestClientAutoConfiguration.class
})
public class TestApplication extends BaseApplicationCommon {
    @Bean
    @Qualifier("dbaasRestClient")
    public MicroserviceRestClient mockRestClient() {
        return mock(MicroserviceRestClient.class);
    }

    @Bean
    public FilterRegistrationBean<AnonymousAuthenticationFilter> anonymousFilter() {
        return new FilterRegistrationBean<>(new AnonymousAuthenticationFilter("key"));
    }
}

