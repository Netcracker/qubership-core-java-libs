package com.netcracker.cloud.microserviceframework;

import com.netcracker.cloud.dbaas.client.config.EnableServiceDbaasPostgresql;
import com.netcracker.cloud.restclient.MicroserviceRestClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.data.elasticsearch.autoconfigure.DataElasticsearchAutoConfiguration;
import org.springframework.boot.mongodb.autoconfigure.MongoAutoConfiguration;
import org.springframework.boot.restclient.autoconfigure.RestClientAutoConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.cloud.config.client.ConfigClientAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;

import static org.mockito.Mockito.mock;

@Configuration
@EnableServiceDbaasPostgresql
@EnableAutoConfiguration(exclude = {
        DataElasticsearchAutoConfiguration.class,
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

