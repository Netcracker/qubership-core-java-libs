package com.netcracker.maas.declarative.kafka.spring.client.config;

import com.netcracker.cloud.maas.client.api.kafka.KafkaMaaSClient;
import com.netcracker.cloud.maas.client.impl.ApiUrlProvider;
import com.netcracker.cloud.maas.client.impl.apiversion.ServerApiVersion;
import com.netcracker.cloud.maas.client.impl.http.HttpClient;
import com.netcracker.cloud.maas.client.impl.kafka.KafkaMaaSClientImpl;
import com.netcracker.cloud.security.core.auth.M2MManager;
import com.netcracker.cloud.tenantmanager.client.TenantManagerConnector;
import com.netcracker.cloud.tenantmanager.client.impl.TenantManagerConnectorImpl;
import com.netcracker.maas.declarative.kafka.client.impl.tenant.api.InternalTenantService;
import com.netcracker.maas.declarative.kafka.client.impl.tenant.impl.InternalTenantServiceImpl;
import com.netcracker.maas.declarative.kafka.client.impl.topic.provider.api.MaasKafkaTopicServiceProvider;
import com.netcracker.maas.declarative.kafka.client.impl.topic.provider.impl.MaasKafkaTopicServiceProviderImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.netcracker.maas.declarative.kafka.client.impl.common.constant.MaasKafkaCommonConstants.KAFKA_LOCAL_DEV_ENABLED;


@Configuration
@ConditionalOnProperty(value = KAFKA_LOCAL_DEV_ENABLED, havingValue = "false", matchIfMissing = true)
public class MaasKafkaProdClientConfig {

    @Autowired
    MaasKafkaProps props;

    @Bean
    HttpClient maasHttpClient(@Autowired M2MManager m2MManager) {
        return new HttpClient(() -> m2MManager.getToken().getTokenValue());
    }

    @Bean
    TenantManagerConnector tenantManagerConnector(HttpClient httpClient) {
        return new TenantManagerConnectorImpl(httpClient);
    }

    @Bean
    KafkaMaaSClient kafkaMaaSClient(HttpClient client, TenantManagerConnector tenantManagerConnector) {
        return new KafkaMaaSClientImpl(
                client,
                () -> tenantManagerConnector,
                new ApiUrlProvider(new ServerApiVersion(client, props.maasAgentUrl), props.maasAgentUrl)
        );
    }

    @Bean
    MaasKafkaTopicServiceProvider maasKafkaTopicServiceProvider(KafkaMaaSClient kafkaMaaSClient) {
        return new MaasKafkaTopicServiceProviderImpl(kafkaMaaSClient);
    }

    @Bean
    InternalTenantService internalTenantService(TenantManagerConnector tenantManagerConnector) {
        return new InternalTenantServiceImpl(tenantManagerConnector);
    }

}
