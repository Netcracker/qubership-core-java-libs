package com.netcracker.cloud.consul.provider.spring.resttemplate.config;

import com.netcracker.cloud.consul.provider.common.TokenStorage;
import com.netcracker.cloud.consul.provider.common.TokenStorageFactory;
import com.netcracker.cloud.consul.provider.spring.common.SpringTokenStorageFactory;
import com.netcracker.cloud.consul.provider.spring.common.config.ConsulLoginProperties;
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
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;


@Configuration
@EnableConfigurationProperties(ConsulLoginProperties.class)
@ConditionalOnConsulEnabled
//@EnableM2MManager // TODO why it is commented out?
@ConditionalOnProperty(value = "spring.cloud.consul.config.m2m.enabled", havingValue = "true", matchIfMissing = true)
public class ConsulM2MRestTemplateAutoConfiguration {
    private final static Logger LOGGER = LoggerFactory.getLogger(ConsulM2MRestTemplateAutoConfiguration.class);

    @Bean
    public TokenStorage consulTokenStorageViaM2MRestTemplate(ConsulConfigProperties consulConfigProperties,
                                                             ConsulProperties consulProperties,
                                                             M2MManager m2MManager,
                                                             ConsulLoginProperties loginProperties) {
        TokenStorageFactory factory = new SpringTokenStorageFactory(consulConfigProperties, new MicroserviceRestTemplate());

        return factory.create(createOptions(loginProperties, consulProperties, m2MManager, System.getenv("NAMESPACE")));
    }

    /**
     * Collects the raw inputs of the login. Values absent from the configuration stay {@code null} here, so that the
     * builder applies the defaults.
     */
    static TokenStorageFactory.CreateOptions createOptions(ConsulLoginProperties loginProperties,
                                                           ConsulProperties consulProperties,
                                                           M2MManager m2MManager,
                                                           String namespace) {
        return new TokenStorageFactory.CreateOptions.Builder()
                .consulUrl(Utils.formatConsulAddress(consulProperties))
                .namespace(namespace)
                .m2mSupplier(() -> m2MManager.getToken().getTokenValue())
                .mode(loginProperties.getMode())
                .authMethod(loginProperties.getAuthMethod())
                .audience(loginProperties.getAudience())
                .build();
    }


    @Bean
    @ConditionalOnMissingBean(M2MManager.class)
    public M2MManager m2MManager() {
        LOGGER.warn("Initialize dummy m2m manager. Do not use it in production mode.");
        return new DummyM2MManager();
    }
}
