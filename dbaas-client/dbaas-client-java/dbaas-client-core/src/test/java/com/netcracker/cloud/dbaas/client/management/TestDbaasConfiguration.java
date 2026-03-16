package com.netcracker.cloud.dbaas.client.management;

import com.netcracker.cloud.dbaas.client.DbaasClient;
import com.netcracker.cloud.dbaas.client.DbaasClientImpl;
import com.netcracker.cloud.dbaas.client.config.DefaultMSInfoProvider;
import com.netcracker.cloud.dbaas.client.config.EnableDbaasDefault;
import com.netcracker.cloud.dbaas.client.config.MSInfoProvider;
import com.netcracker.cloud.dbaas.client.entity.mongo.MongoDatabase;
import com.netcracker.cloud.dbaas.client.entity.postgres.PostgresDatabase;
import com.netcracker.cloud.restclient.MicroserviceRestClient;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

@Configuration
@EnableDbaasDefault
public class TestDbaasConfiguration {

    @Primary
    @Bean
    public RetryTemplate testDbaasRetryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();

        FixedBackOffPolicy fixedBackOffPolicy = new FixedBackOffPolicy();
        fixedBackOffPolicy.setBackOffPeriod(10);
        retryTemplate.setBackOffPolicy(fixedBackOffPolicy);

        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(2);
        retryTemplate.setRetryPolicy(retryPolicy);

        return retryTemplate;
    }

    @Bean
    public MicroserviceRestClient restClient(){
        return Mockito.mock(MicroserviceRestClient.class);
    }

    @Bean
    @Primary
    public DbaasClient dbaasClient(@Autowired RetryTemplate retryTemplate) {
        return new DbaasClientImpl(restClient(), retryTemplate, "http://ms-name.namespace:8080");
    }

    @Bean("testMongoPostProcessor")
    public PostConnectProcessor<MongoDatabase> testMongoDatabasePostProcessor() {
        PostConnectProcessor mock = Mockito.mock(PostConnectProcessor.class);
        return mock;
    }

    @Bean("testPostgresPostProcessor")
    public PostConnectProcessor<PostgresDatabase> testPostgresDatabasePostProcessor() {
        PostConnectProcessor mock = Mockito.mock(PostConnectProcessor.class);
        return mock;
    }


    @Bean
    public MSInfoProvider msInfoProvider() {
        return new DefaultMSInfoProvider();
    }
}
