package org.qubership.maas.declarative.kafka.quarkus.client.config.local.dev;

import org.qubership.maas.declarative.kafka.client.impl.common.constant.MaasKafkaCommonConstants;
import org.qubership.maas.declarative.kafka.client.impl.common.cred.extractor.impl.LocalDevInternalTopicCredentialsExtractorImpl;
import org.qubership.maas.declarative.kafka.client.impl.common.cred.extractor.provider.api.InternalMaasCredExtractorProvider;
import org.qubership.maas.declarative.kafka.client.impl.common.cred.extractor.provider.impl.LocalDevInternalTopicCredentialsExtractorProviderImpl;
import org.qubership.maas.declarative.kafka.client.impl.local.dev.config.api.MaasKafkaLocalDevConfigProviderService;
import org.qubership.maas.declarative.kafka.client.impl.local.dev.config.impl.MaasKafkaLocalDevConfigProviderImpl;
import org.qubership.maas.declarative.kafka.client.impl.local.dev.tenant.LocalDevInternalTenantServiceImpl;
import org.qubership.maas.declarative.kafka.client.impl.tenant.api.InternalTenantService;
import org.qubership.maas.declarative.kafka.client.impl.topic.provider.api.MaasKafkaTopicServiceProvider;
import org.qubership.maas.declarative.kafka.client.impl.topic.provider.impl.LocalDevMaasDirectKafkaTopicServiceProviderImpl;
import org.qubership.maas.declarative.kafka.quarkus.client.ConfigUtils;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.List;

import static org.qubership.maas.declarative.kafka.client.impl.common.constant.MaasKafkaCommonConstants.KAFKA_LOCAL_DEV_CONFIG;


@Singleton
public class MaasKafkaLocalDevConfig {

    @ConfigProperty(name = MaasKafkaCommonConstants.KAFKA_LOCAL_DEV_TENANTS_IDS)
    List<String> tenantIds;

    @Singleton
    @Produces
    public InternalTenantService localDevTenantService() {
        return new LocalDevInternalTenantServiceImpl(() -> tenantIds);
    }

    @Singleton
    @Produces
    public MaasKafkaLocalDevConfigProviderService localDevConfigProviderService(Config config) {
        return new MaasKafkaLocalDevConfigProviderImpl(ConfigUtils.configAsMap(config, KAFKA_LOCAL_DEV_CONFIG));
    }

    @Singleton
    @Produces
    public MaasKafkaTopicServiceProvider localDevTopicServiceProvider(
            MaasKafkaLocalDevConfigProviderService configProviderService
    ) {
        return new LocalDevMaasDirectKafkaTopicServiceProviderImpl(configProviderService);
    }


    @Singleton
    @Produces
    public InternalMaasCredExtractorProvider localDevCredExtractorProvider(
            MaasKafkaLocalDevConfigProviderService configProviderService
    ) {
        return new LocalDevInternalTopicCredentialsExtractorProviderImpl(
                new LocalDevInternalTopicCredentialsExtractorImpl(configProviderService.get())// TODO check on test
        );
    }
}
