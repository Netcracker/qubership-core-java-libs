package com.netcracker.cloud.consul.provider.spring.resttemplate.config;

import com.netcracker.cloud.consul.provider.common.TokenStorage;
import com.netcracker.cloud.consul.provider.common.TokenStorageFactory;
import com.netcracker.cloud.consul.provider.spring.common.SpringTokenStorageFactory;
import com.netcracker.cloud.consul.provider.spring.common.Utils;
import com.netcracker.cloud.restclient.resttemplate.MicroserviceRestTemplate;
import com.netcracker.cloud.security.core.auth.DummyM2MManager;
import com.netcracker.cloud.security.core.auth.M2MManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.consul.ConditionalOnConsulEnabled;
import org.springframework.cloud.consul.ConsulProperties;
import org.springframework.cloud.consul.config.ConsulConfigProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
@ConditionalOnConsulEnabled
//@EnableM2MManager // TODO why it is commented out?
@ConditionalOnProperty(value = "spring.cloud.consul.config.m2m.enabled", havingValue = "true", matchIfMissing = true)
public class ConsulM2MRestTemplateAutoConfiguration {
    private final static Logger LOGGER = LoggerFactory.getLogger(ConsulM2MRestTemplateAutoConfiguration.class);

    @Bean
    public TokenStorage consulTokenStorageViaM2MRestTemplate(ConsulConfigProperties consulConfigProperties,
                                                             ConsulProperties consulProperties,
                                                             M2MManager m2MManager) {
        TokenStorageFactory factory = new SpringTokenStorageFactory(consulConfigProperties, new MicroserviceRestTemplate());

        return factory.create(new TokenStorageFactory.CreateOptions.Builder()
                .consulUrl(Utils.formatConsulAddress(consulProperties))
                .namespace(System.getenv("NAMESPACE"))
                .m2mSupplier(() -> m2MManager.getToken().getTokenValue())
                .build());
    }


    @Bean
    @ConditionalOnMissingBean(M2MManager.class)
    public M2MManager m2MManager() {
        LOGGER.warn("Initialize dummy m2m manager. Do not use it in production mode.");
        return new DummyM2MManager();
    }
}
