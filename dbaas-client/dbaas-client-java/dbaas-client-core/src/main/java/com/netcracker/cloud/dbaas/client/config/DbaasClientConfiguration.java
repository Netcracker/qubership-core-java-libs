package org.qubership.cloud.dbaas.client.config;

import org.qubership.cloud.dbaas.client.DbaasClient;
import org.qubership.cloud.dbaas.client.DbaasClientImpl;
import org.qubership.cloud.dbaas.client.exceptions.DatabaseNotReadyException;
import org.qubership.cloud.dbaas.client.management.classifier.DbaasClassifierFactory;
import org.qubership.cloud.dbaas.client.util.RetryTemplateUtils;
import org.qubership.cloud.restclient.MicroserviceRestClient;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;


@Slf4j
@Configuration
@ComponentScan(basePackages = "org.qubership.cloud.dbaas.client.restclient") // For searching a dbaasRestClient bean
public class DbaasClientConfiguration {

    @Bean
    public SpringDbaasApiProperties springDbaasApiProperties() {
        return new SpringDbaasApiProperties();
    }

    @Bean
    @ConditionalOnMissingBean(DbaasClient.class)
    public DbaasClient dbaasClient(@Autowired(required = false) @Qualifier("dbaasClientDefaultRetryTemplate") RetryTemplate retryTemplate,
                                   @Qualifier("dbaasRestClient") MicroserviceRestClient dbaasRestClient,
                                   @Qualifier("dbaasClientAwaitAsyncDbCreationRetryTemplate") RetryTemplate dbaasClientAwaitAsyncDbCreationRetryTemplate,
                                   SpringDbaasApiProperties apiProperties) {
        DbaasClientImpl dbaasClient = new DbaasClientImpl(dbaasRestClient, retryTemplate, dbaasClientAwaitAsyncDbCreationRetryTemplate, apiProperties.getAddress());
        log.debug("Created dbaasClient bean {}", dbaasClient);
        return dbaasClient;
    }

    @Bean("dbaasClientAwaitAsyncDbCreationRetryTemplate")
    @ConditionalOnMissingBean(name = "dbaasClientAwaitAsyncDbCreationRetryTemplate")
    public RetryTemplate dbaasClientAwaitAsyncDbCreationRetryTemplate(SpringDbaasApiProperties apiProperties) {
        return RetryTemplateUtils.createAwaitAsyncDbCreationRetryTemplate(apiProperties.getDbaasAsyncRetryTimeoutInS());
    }


    /* By default the retry template is enabled */
    @Bean("dbaasClientDefaultRetryTemplate")
    @ConditionalOnProperty(name = {"dbaas.api.retry.default.template.enabled"}, havingValue = "true", matchIfMissing = true)
    public RetryTemplate dbaasClientDefaultRetryTemplate(SpringDbaasApiProperties apiProperties) {
        RetryTemplate retryTemplate = new RetryTemplate();
        FixedBackOffPolicy fixedBackOffPolicy = new FixedBackOffPolicy();
        fixedBackOffPolicy.setBackOffPeriod(apiProperties.getDbaasDefaultRetryBackOffPeriodInMs());
        retryTemplate.setBackOffPolicy(fixedBackOffPolicy);

        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(apiProperties.getDbaasDefaultRetryMaxAttempts());
        retryPolicy.setNotRecoverable(DatabaseNotReadyException.class);
        retryTemplate.setRetryPolicy(retryPolicy);
        retryTemplate.registerListener(new DefaultRetryListener());

        return retryTemplate;
    }

    @Bean
    @ConditionalOnMissingBean(MSInfoProvider.class)
    MSInfoProvider defaultMsInfoProvider() {
        return new DefaultMSInfoProvider();
    }

    @Bean
    @ConditionalOnMissingBean
    DbaasClassifierFactory dbaasClassifierFactory(MSInfoProvider msInfoProvider) {
        return new DbaasClassifierFactory(msInfoProvider);
    }
}

