package org.qubership.maas.declarative.kafka.spring.client.config;

import org.qubership.cloud.maas.client.api.kafka.KafkaMaaSClient;
import org.qubership.cloud.maas.client.impl.ApiUrlProvider;
import org.qubership.cloud.maas.client.impl.apiversion.ServerApiVersion;
import org.qubership.cloud.maas.client.impl.http.HttpClient;
import org.qubership.cloud.maas.client.impl.kafka.KafkaMaaSClientImpl;
import org.qubership.cloud.security.core.auth.M2MManager;
import org.qubership.cloud.tenantmanager.client.TenantManagerConnector;
import org.qubership.cloud.tenantmanager.client.impl.TenantManagerConnectorImpl;
import org.qubership.maas.declarative.kafka.client.impl.tenant.api.InternalTenantService;
import org.qubership.maas.declarative.kafka.client.impl.tenant.impl.InternalTenantServiceImpl;
import org.qubership.maas.declarative.kafka.client.impl.topic.provider.api.MaasKafkaTopicServiceProvider;
import org.qubership.maas.declarative.kafka.client.impl.topic.provider.impl.MaasKafkaTopicServiceProviderImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.qubership.maas.declarative.kafka.client.impl.common.constant.MaasKafkaCommonConstants.KAFKA_LOCAL_DEV_ENABLED;


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
