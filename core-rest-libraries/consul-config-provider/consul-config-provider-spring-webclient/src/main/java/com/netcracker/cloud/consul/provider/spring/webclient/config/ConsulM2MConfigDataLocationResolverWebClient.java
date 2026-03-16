package com.netcracker.cloud.consul.provider.spring.webclient.config;

import com.netcracker.cloud.consul.provider.spring.common.config.ConsulM2MConfigDataLocationResolver;
import com.netcracker.cloud.restclient.MicroserviceRestClient;
import com.netcracker.cloud.restclient.webclient.MicroserviceWebClient;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

@Order(Ordered.HIGHEST_PRECEDENCE)
public class ConsulM2MConfigDataLocationResolverWebClient extends ConsulM2MConfigDataLocationResolver {

    public ConsulM2MConfigDataLocationResolverWebClient(DeferredLogFactory log) {
        super(log);
    }

    @Override
    protected MicroserviceRestClient createMicroserviceRestClient() {
        return new MicroserviceWebClient();
    }
}
