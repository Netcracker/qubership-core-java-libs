package com.netcracker.cloud.configserver.webclient;

import com.netcracker.cloud.configserver.common.configuration.AbstractCustomConfigServerConfigDataLocationResolver;
import com.netcracker.cloud.restclient.MicroserviceRestClient;
import com.netcracker.cloud.restclient.webclient.MicroserviceWebClient;
import com.netcracker.cloud.security.core.auth.M2MManager;
import org.springframework.boot.bootstrap.ConfigurableBootstrapContext;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.WebClient;
import com.netcracker.cloud.security.core.utils.k8s.M2MClientFactory;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

public class WebClientConfigServerConfigDataLocationResolver extends AbstractCustomConfigServerConfigDataLocationResolver {

    private ConfigurableBootstrapContext configurableBootstrapContext;

    public WebClientConfigServerConfigDataLocationResolver(DeferredLogFactory log, ConfigurableBootstrapContext configurableBootstrapContext) {
        super(log);
        this.configurableBootstrapContext = configurableBootstrapContext;
    }

    @Override
    public MicroserviceRestClient getMicroserviceRestClient() {
        return new MicroserviceWebClient(createM2MWebClient());
    }

    private WebClient createM2MWebClient() {
        var client = M2MClientFactory.getM2MClient(() -> getM2MToken(configurableBootstrapContext));
        ClientHttpConnector connector = new OkHttp3ClientHttpRequestFactory(client);
        WebClient.Builder builder = WebClient.builder()
                .clientConnector(connector);
        return builder.build();
    }

    private String getM2MToken(ConfigurableBootstrapContext configurableBootstrapContext) {
        return configurableBootstrapContext.get(M2MManager.class).getToken().getTokenValue();
    }

    private boolean hasM2M(ConfigurableBootstrapContext configurableBootstrapContext) {
        return configurableBootstrapContext.isRegistered(M2MManager.class);
    }

}
