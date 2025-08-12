package com.netcracker.maas.declarative.kafka.spring.client.config.local.dev;

import com.netcracker.cloud.maas.spring.localdev.MaasLocalDevConfig;
import com.netcracker.maas.declarative.kafka.client.impl.common.cred.extractor.impl.LocalDevInternalTopicCredentialsExtractorImpl;
import com.netcracker.maas.declarative.kafka.client.impl.common.cred.extractor.provider.api.InternalMaasCredExtractorProvider;
import com.netcracker.maas.declarative.kafka.client.impl.common.cred.extractor.provider.impl.LocalDevInternalTopicCredentialsExtractorProviderImpl;
import com.netcracker.maas.declarative.kafka.client.impl.local.dev.config.api.MaasKafkaLocalDevConfigProviderService;
import com.netcracker.maas.declarative.kafka.client.impl.local.dev.config.impl.MaasKafkaLocalDevConfigProviderImpl;
import com.netcracker.maas.declarative.kafka.client.impl.local.dev.tenant.LocalDevInternalTenantServiceImpl;
import com.netcracker.maas.declarative.kafka.client.impl.tenant.api.InternalTenantService;
import com.netcracker.maas.declarative.kafka.client.impl.topic.provider.api.MaasKafkaTopicServiceProvider;
import com.netcracker.maas.declarative.kafka.client.impl.topic.provider.impl.LocalDevMaasDirectKafkaTopicServiceProviderImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.util.List;

import static com.netcracker.maas.declarative.kafka.client.impl.common.constant.MaasKafkaCommonConstants.KAFKA_LOCAL_DEV_ENABLED;
import static com.netcracker.maas.declarative.kafka.client.impl.common.constant.MaasKafkaCommonConstants.KAFKA_LOCAL_DEV_TENANTS_IDS;


@Configuration
@ConditionalOnProperty(value = KAFKA_LOCAL_DEV_ENABLED, havingValue = "true")
@Import(MaasLocalDevConfig.class)
public class MaasKafkaLocalDevConfig {

    @Value("${" + KAFKA_LOCAL_DEV_TENANTS_IDS + ":" + "#{null}" + "}")
    List<String> tenantIds;

    @Autowired
    MaasKafkaLocalDevConfigProps configProps;

    @Bean
    public InternalTenantService localDevTenantService() {
        return new LocalDevInternalTenantServiceImpl(() -> tenantIds);
    }

    @Bean
    public MaasKafkaLocalDevConfigProviderService localDevConfigProviderService() {
        return new MaasKafkaLocalDevConfigProviderImpl(configProps.getAsStringObjectMap());
    }

    @Bean
    public MaasKafkaTopicServiceProvider localDevTopicServiceProvider(
            MaasKafkaLocalDevConfigProviderService configProviderService
    ) {
        return new LocalDevMaasDirectKafkaTopicServiceProviderImpl(configProviderService);
    }

    @Bean
    public InternalMaasCredExtractorProvider localDevCredExtractorProvider(
            MaasKafkaLocalDevConfigProviderService configProviderService
    ) {
        return new LocalDevInternalTopicCredentialsExtractorProviderImpl(
                new LocalDevInternalTopicCredentialsExtractorImpl(configProviderService.get())// TODO check on test
        );
    }
}
