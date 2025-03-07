package org.qubership.cloud.consul.provider.spring.resttemplate.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.config.ConfigDataLocation;
import org.springframework.boot.context.config.ConfigDataLocationResolverContext;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.boot.logging.DeferredLogs;
import org.springframework.web.util.UriComponents;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ConsulM2MConfigDataLocationResolverRestTemplateTest {
    ConsulM2MConfigDataLocationResolverRestTemplatePublic resolverRestTemplate = new ConsulM2MConfigDataLocationResolverRestTemplatePublic(new DeferredLogs());


    @Test
    void parseLocation() {
        UriComponents location = resolverRestTemplate.parseLocation(null, ConfigDataLocation.of("consul:http://test-consul:8500"));
        assertNotNull(location);
        assertEquals("test-consul", location.getHost());
        assertEquals(8500, location.getPort());
    }

    class ConsulM2MConfigDataLocationResolverRestTemplatePublic extends ConsulM2MConfigDataLocationResolverRestTemplate {

        public ConsulM2MConfigDataLocationResolverRestTemplatePublic(DeferredLogFactory log) {
            super(log);
        }

        @Override
        public UriComponents parseLocation(ConfigDataLocationResolverContext context, ConfigDataLocation location) {
            return super.parseLocation(context, location);
        }
    }
}