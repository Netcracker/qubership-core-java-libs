package com.netcracker.cloud.configserver.resttemplate;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.util.Timeout;
import com.netcracker.cloud.configserver.common.configuration.AbstractCustomConfigServerConfigDataLocationResolver;
import com.netcracker.cloud.restclient.MicroserviceRestClient;
import com.netcracker.cloud.restclient.resttemplate.MicroserviceRestTemplate;
import com.netcracker.cloud.security.core.auth.M2MManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ConfigurableBootstrapContext;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;

public class RestTemplateConfigServerConfigDataLocationResolver extends AbstractCustomConfigServerConfigDataLocationResolver {

    @Value("${connection.readTimeout:60000}")
    private int readTimeout;

    private ConfigurableBootstrapContext configurableBootstrapContext;

    public RestTemplateConfigServerConfigDataLocationResolver(DeferredLogFactory log, ConfigurableBootstrapContext configurableBootstrapContext) {
        super(log);
        this.configurableBootstrapContext = configurableBootstrapContext;
    }

    @Override
    public MicroserviceRestClient getMicroserviceRestClient() {
        return new MicroserviceRestTemplate(createM2MRestTemplate());
    }

    private RestTemplate createM2MRestTemplate() {
        RestTemplate template = new RestTemplate();
        SocketConfig socketConfig = SocketConfig.custom().setSoTimeout(Timeout.ofMilliseconds(readTimeout)).build();

        final PoolingHttpClientConnectionManager poolingHttpClientConnectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setDefaultSocketConfig(socketConfig)
                .build();
        HttpClient httpClient = HttpClients.custom().setConnectionManager(poolingHttpClientConnectionManager).build();

        template.setRequestFactory(new HttpComponentsClientHttpRequestFactory(httpClient));
        if (hasM2M(configurableBootstrapContext)) {
            template.setInterceptors(Collections.singletonList((request, body, execution) -> {
                request.getHeaders().setBearerAuth(getM2MToken(configurableBootstrapContext));
                return execution.execute(request, body);
            }));
        }

        return template;
    }

    private String getM2MToken(ConfigurableBootstrapContext configurableBootstrapContext) {
        return configurableBootstrapContext.get(M2MManager.class).getToken().getTokenValue();
    }

    private boolean hasM2M(ConfigurableBootstrapContext configurableBootstrapContext) {
        return configurableBootstrapContext.isRegistered(M2MManager.class);
    }
}
