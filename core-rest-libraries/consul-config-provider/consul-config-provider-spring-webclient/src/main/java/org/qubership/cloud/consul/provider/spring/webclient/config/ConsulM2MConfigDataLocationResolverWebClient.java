package org.qubership.cloud.consul.provider.spring.webclient.config;

import org.qubership.cloud.consul.provider.spring.common.config.ConsulM2MConfigDataLocationResolver;
import org.qubership.cloud.restclient.MicroserviceRestClient;
import org.qubership.cloud.restclient.webclient.MicroserviceWebClient;
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
