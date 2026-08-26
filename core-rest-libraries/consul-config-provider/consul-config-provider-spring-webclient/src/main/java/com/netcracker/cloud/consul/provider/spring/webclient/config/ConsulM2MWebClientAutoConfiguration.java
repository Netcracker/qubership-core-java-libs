package com.netcracker.cloud.consul.provider.spring.webclient.config;

import com.netcracker.cloud.consul.provider.common.ConsulLoginMode;
import com.netcracker.cloud.consul.provider.common.TokenStorage;
import com.netcracker.cloud.consul.provider.common.TokenStorageFactory;
import com.netcracker.cloud.consul.provider.spring.common.SpringTokenStorageFactory;
import com.netcracker.cloud.consul.provider.spring.common.config.ConsulM2MConfigDataLocationResolver;
import com.netcracker.cloud.consul.provider.spring.common.Utils;
import com.netcracker.cloud.restclient.webclient.MicroserviceWebClient;
import com.netcracker.cloud.security.common.reactive.DummyM2MManager;
import com.netcracker.cloud.security.common.reactive.M2MManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.cloud.consul.ConditionalOnConsulEnabled;
import org.springframework.cloud.consul.ConsulProperties;
import org.springframework.cloud.consul.config.ConsulConfigProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
@ConditionalOnConsulEnabled
//@EnableReactiveM2MManager
@ConditionalOnProperty(value = "spring.cloud.consul.config.m2m.enabled", havingValue = "true", matchIfMissing = true)
public class ConsulM2MWebClientAutoConfiguration {

    public static final String PROP_LOGIN_MODE = ConsulM2MConfigDataLocationResolver.PROP_LOGIN_MODE;
    public static final String PROP_LOGIN_AUTH_METHOD = ConsulM2MConfigDataLocationResolver.PROP_LOGIN_AUTH_METHOD;
    public static final String PROP_LOGIN_AUDIENCE = ConsulM2MConfigDataLocationResolver.PROP_LOGIN_AUDIENCE;

    @Bean
    public TokenStorage consulTokenStorageViaM2MWebClient(ConsulConfigProperties consulConfigProperties,
                                                          ConsulProperties consulProperties,
                                                          M2MManager m2MManager,
                                                          Environment environment) {
        TokenStorageFactory factory = new SpringTokenStorageFactory(consulConfigProperties, new MicroserviceWebClient());

        return factory.create(createOptions(environment, consulProperties, m2MManager, System.getenv("NAMESPACE")));
    }

    static TokenStorageFactory.CreateOptions createOptions(Environment environment,
                                                           ConsulProperties consulProperties,
                                                           M2MManager m2MManager,
                                                           String namespace) {
        Binder binder = Binder.get(environment);

        return new TokenStorageFactory.CreateOptions.Builder()
                .consulUrl(Utils.formatConsulAddress(consulProperties))
                .namespace(namespace)
                .m2mSupplier(() -> m2MManager.getToken().block().getTokenValue())
                .mode(binder.bind(PROP_LOGIN_MODE, ConsulLoginMode.class).orElse(null))
                .authMethod(binder.bind(PROP_LOGIN_AUTH_METHOD, String.class).orElse(null))
                .audience(binder.bind(PROP_LOGIN_AUDIENCE, String.class).orElse(null))
                .build();
    }

    @Bean
    @ConditionalOnMissingBean(M2MManager.class)
    public M2MManager m2MManager() {
        return new DummyM2MManager();
    }
}
