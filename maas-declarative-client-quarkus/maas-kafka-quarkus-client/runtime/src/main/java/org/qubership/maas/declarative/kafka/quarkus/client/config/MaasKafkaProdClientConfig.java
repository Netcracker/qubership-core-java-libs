package org.qubership.maas.declarative.kafka.quarkus.client.config;

import org.qubership.cloud.maas.client.api.kafka.KafkaMaaSClient;
import org.qubership.cloud.maas.client.impl.ApiUrlProvider;
import org.qubership.cloud.maas.client.impl.apiversion.ServerApiVersion;
import org.qubership.cloud.maas.client.impl.http.HttpClient;
import org.qubership.cloud.maas.client.impl.kafka.KafkaMaaSClientImpl;
import org.qubership.cloud.security.core.auth.M2MManager;
import org.qubership.cloud.tenantmanager.client.impl.TenantManagerConnectorImpl;
import org.qubership.maas.declarative.kafka.client.impl.tenant.api.InternalTenantService;
import org.qubership.maas.declarative.kafka.client.impl.tenant.impl.InternalTenantServiceImpl;
import org.qubership.maas.declarative.kafka.client.impl.topic.provider.api.MaasKafkaTopicServiceProvider;
import org.qubership.maas.declarative.kafka.client.impl.topic.provider.impl.MaasKafkaTopicServiceProviderImpl;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;


@Singleton
public class MaasKafkaProdClientConfig {
    @Singleton
    @Produces
    KafkaMaaSClient kafkaMaaSClient(MaasKafkaProps props, M2MManager m2mManager) {
        HttpClient httpClient = new HttpClient(() -> m2mManager.getToken().getTokenValue());
        return new KafkaMaaSClientImpl(
                httpClient,
                () -> new TenantManagerConnectorImpl(httpClient),
                new ApiUrlProvider(new ServerApiVersion(httpClient, props.maasAgentUrl), props.maasAgentUrl)
        );
    }

    @Singleton
    @Produces
    MaasKafkaTopicServiceProvider maasKafkaTopicServiceProvider(KafkaMaaSClient kafkaMaaSClient) {
        return new MaasKafkaTopicServiceProviderImpl(
                kafkaMaaSClient
        );
    }

    @Singleton
    @Produces
    @DefaultBean
    InternalTenantService internalTenantService(M2MManager m2mManager) {
        HttpClient httpClient = new HttpClient(() -> m2mManager.getToken().getTokenValue());
        TenantManagerConnectorImpl tenantManagerConnector = new TenantManagerConnectorImpl(httpClient);
        return new InternalTenantServiceImpl(tenantManagerConnector);
    }
}