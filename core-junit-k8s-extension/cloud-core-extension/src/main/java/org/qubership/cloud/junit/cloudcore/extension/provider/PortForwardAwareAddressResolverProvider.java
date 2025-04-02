package org.qubership.cloud.junit.cloudcore.extension.provider;

import lombok.extern.slf4j.Slf4j;

import java.net.spi.InetAddressResolver;
import java.net.spi.InetAddressResolverProvider;

@Slf4j
public class PortForwardAwareAddressResolverProvider extends InetAddressResolverProvider {
    @Override
    public InetAddressResolver get(Configuration configuration) {
        return new PortForwardAwareAddressResolver(configuration.builtinResolver());
    }

    @Override
    public String name() {
        return "PortForwardAwareAddressResolverProvider";
    }
}
