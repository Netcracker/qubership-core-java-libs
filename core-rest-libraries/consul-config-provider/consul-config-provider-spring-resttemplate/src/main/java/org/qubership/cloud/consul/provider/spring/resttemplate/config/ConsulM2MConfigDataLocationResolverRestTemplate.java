package org.qubership.cloud.consul.provider.spring.resttemplate.config;

import org.qubership.cloud.consul.provider.spring.common.config.ConsulM2MConfigDataLocationResolver;
import org.qubership.cloud.restclient.MicroserviceRestClient;
import org.qubership.cloud.restclient.resttemplate.MicroserviceRestTemplate;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

@Order(Ordered.HIGHEST_PRECEDENCE)
public class ConsulM2MConfigDataLocationResolverRestTemplate extends ConsulM2MConfigDataLocationResolver {

    public ConsulM2MConfigDataLocationResolverRestTemplate(DeferredLogFactory log) {
        super(log);
    }

    @Override
    protected MicroserviceRestClient createMicroserviceRestClient() {
        return new MicroserviceRestTemplate();
    }
}
