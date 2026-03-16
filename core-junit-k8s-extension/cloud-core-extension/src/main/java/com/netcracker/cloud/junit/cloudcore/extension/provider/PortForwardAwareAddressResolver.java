package com.netcracker.cloud.junit.cloudcore.extension.provider;

import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.spi.InetAddressResolver;
import java.util.stream.Stream;

@Slf4j
public class PortForwardAwareAddressResolver implements InetAddressResolver {

    private InetAddressResolver builtinResolver;

    public PortForwardAwareAddressResolver(InetAddressResolver builtinResolver) {
        this.builtinResolver = builtinResolver;
    }

    @Override
    public Stream<InetAddress> lookupByName(String host, LookupPolicy lookupPolicy) throws UnknownHostException {
        InetAddress inetAddress = LocalHostAddressGenerator.get(host);
        return inetAddress != null ? Stream.of(inetAddress) : this.builtinResolver.lookupByName(host, lookupPolicy);
    }

    @Override
    public String lookupByAddress(byte[] addr) throws UnknownHostException {
        return this.builtinResolver.lookupByAddress(addr);
    }
}
