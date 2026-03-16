package com.netcracker.cloud.configserver.webclient;

import com.netcracker.cloud.configserver.common.configuration.AbstractCustomConfigServerConfigDataLocationResolver;
import com.netcracker.cloud.restclient.MicroserviceRestClient;
import com.netcracker.cloud.restclient.webclient.MicroserviceWebClient;
import com.netcracker.cloud.security.core.auth.M2MManager;
import org.springframework.boot.ConfigurableBootstrapContext;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.WebClient;

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
        WebClient.Builder builder = WebClient.builder();
        if (hasM2M(configurableBootstrapContext)) {
            builder.filter(
                    (request, next) ->
                            next.exchange(ClientRequest.from(request).
                                    header(AUTHORIZATION, "Bearer " + getM2MToken(configurableBootstrapContext)).build())
            );
        }
        return builder.build();
    }

    private String getM2MToken(ConfigurableBootstrapContext configurableBootstrapContext) {
        return configurableBootstrapContext.get(M2MManager.class).getToken().getTokenValue();
    }

    private boolean hasM2M(ConfigurableBootstrapContext configurableBootstrapContext) {
        return configurableBootstrapContext.isRegistered(M2MManager.class);
    }

}
