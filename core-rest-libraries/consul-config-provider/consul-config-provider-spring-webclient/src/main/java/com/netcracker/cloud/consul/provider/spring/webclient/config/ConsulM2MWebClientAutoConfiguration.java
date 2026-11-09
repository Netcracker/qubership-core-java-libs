package com.netcracker.cloud.consul.provider.spring.webclient.config;

import com.netcracker.cloud.consul.provider.common.TokenStorage;
import com.netcracker.cloud.consul.provider.common.TokenStorageFactory;
import com.netcracker.cloud.consul.provider.spring.common.SpringTokenStorageFactory;
import com.netcracker.cloud.consul.provider.spring.common.config.ConsulLoginProperties;
import com.netcracker.cloud.consul.provider.spring.common.Utils;
import com.netcracker.cloud.restclient.webclient.MicroserviceWebClient;
import com.netcracker.cloud.security.core.auth.DummyM2MManager;
import com.netcracker.cloud.security.core.auth.M2MManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.consul.ConditionalOnConsulEnabled;
import org.springframework.cloud.consul.ConsulProperties;
import org.springframework.cloud.consul.config.ConsulConfigProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.function.Supplier;

@Configuration
@EnableConfigurationProperties(ConsulLoginProperties.class)
@ConditionalOnConsulEnabled
//@EnableReactiveM2MManager
@ConditionalOnProperty(value = "spring.cloud.consul.config.m2m.enabled", havingValue = "true", matchIfMissing = true)
public class ConsulM2MWebClientAutoConfiguration {

    @Bean
    public TokenStorage consulTokenStorageViaM2MWebClient(ConsulConfigProperties consulConfigProperties,
                                                          ConsulProperties consulProperties,
                                                          M2MManager m2MManager,
                                                          ConsulLoginProperties loginProperties) {
        TokenStorageFactory factory = new SpringTokenStorageFactory(consulConfigProperties, new MicroserviceWebClient());

        return factory.create(createOptions(loginProperties, consulProperties, m2MManager, System.getenv("NAMESPACE")));
    }

    static TokenStorageFactory.CreateOptions createOptions(ConsulLoginProperties loginProperties,
                                                           ConsulProperties consulProperties,
                                                           M2MManager m2MManager,
                                                           String namespace) {
        return loginProperties.toOptionsBuilder()
                .consulUrl(Utils.formatConsulAddress(consulProperties))
                .namespace(namespace)
                .m2mSupplier(m2mTokenSupplier(m2MManager))
                .build();
    }

    /**
     * The bearer of the m2m login. A {@link DummyM2MManager} is refused: its token is the literal {@code dummy}, which
     * Consul rejects as a malformed JWT, so the relogin would fail with a misleading error.
     */
    static Supplier<String> m2mTokenSupplier(M2MManager m2MManager) {
        return () -> {
            if (m2MManager instanceof DummyM2MManager) {
                throw new IllegalStateException("no M2MManager is registered: the consul m2m login needs a real M2M token");
            }
            return m2MManager.getToken().getTokenValue();
        };
    }

    @Bean
    @ConditionalOnMissingBean(M2MManager.class)
    public M2MManager m2MManager() {
        return new DummyM2MManager();
    }
}
