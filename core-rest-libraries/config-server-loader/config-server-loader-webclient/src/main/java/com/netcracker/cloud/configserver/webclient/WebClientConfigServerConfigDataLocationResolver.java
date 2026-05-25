package com.netcracker.cloud.configserver.webclient;

import com.netcracker.cloud.configserver.common.configuration.AbstractCustomConfigServerConfigDataLocationResolver;
import com.netcracker.cloud.restclient.MicroserviceRestClient;
import com.netcracker.cloud.restclient.okhttp.MicroserviceOkHttpRestClient;
import com.netcracker.cloud.restclient.webclient.MicroserviceWebClient;
import com.netcracker.cloud.security.core.auth.M2MManager;
import com.netcracker.cloud.security.core.utils.k8s.M2MClientFactory;
import org.springframework.boot.bootstrap.ConfigurableBootstrapContext;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.web.reactive.function.client.WebClient;

public class WebClientConfigServerConfigDataLocationResolver extends AbstractCustomConfigServerConfigDataLocationResolver {

    private ConfigurableBootstrapContext configurableBootstrapContext;

    public WebClientConfigServerConfigDataLocationResolver(DeferredLogFactory log, ConfigurableBootstrapContext configurableBootstrapContext) {
        super(log);
        this.configurableBootstrapContext = configurableBootstrapContext;
    }

    @Override
    public MicroserviceRestClient getMicroserviceRestClient() {
        if (hasM2M(configurableBootstrapContext)) {
            var client = M2MClientFactory.getM2mOkHttpClient(() -> getM2MToken(configurableBootstrapContext), k8sM2mEnabled);
            return new MicroserviceOkHttpRestClient(client);
        }
        return createM2MWebClient();
    }

    private MicroserviceRestClient createM2MWebClient() {
        return new MicroserviceWebClient(WebClient.builder().build());
    }

    private String getM2MToken(ConfigurableBootstrapContext configurableBootstrapContext) {
        return configurableBootstrapContext.get(M2MManager.class).getToken().getTokenValue();
    }

    private boolean hasM2M(ConfigurableBootstrapContext configurableBootstrapContext) {
        return configurableBootstrapContext.isRegistered(M2MManager.class);
    }

}
