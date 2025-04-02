package org.qubership.cloud.junit.cloudcore.extension.provider;

import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Slf4j
public class LocalHostAddressGenerator {

    private static final Map<String, InetAddress> hostAddressMap = new ConcurrentHashMap<>();
    private static InetAddress currentIp;

    static {
        setup();
    }

    public static void setup() {
        try {
            // disable DNS cache
            java.security.Security.setProperty("networkaddress.cache.ttl", "0");
            currentIp = InetAddress.getByName("127.0.0.1");
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static synchronized InetAddress get(String host) {
        return hostAddressMap.get(host);
    }

    public static synchronized void put(String host, InetAddress address) {
        hostAddressMap.put(host, address);
    }

    public static synchronized InetAddress getOrNext(String host) {
        InetAddress inetAddress = hostAddressMap.get(host);
        if (inetAddress != null) {
            log.info("Found IP address '{}' for host '{}'", inetAddress.getHostAddress(), host);
            return inetAddress;
        }
        return getNextAddress(host);
    }

    protected static InetAddress getNextAddress(String host) {
        // 127.0.0.1 — 127.255.255.254
        try {
            byte[] address = currentIp.getAddress();
            if (++address[3] == -1) {
                address[3]++;
                address[2]++;
                if (address[2] == 0) {
                    address[1]++;
                    if (address[1] == 0) {
                        address[0]++;
                        if (address[0] == -128) {
                            throw new IllegalArgumentException(String.format("The next ip '%s' is out of bounds",
                                    InetAddress.getByAddress(address).getHostAddress()));
                        }
                    }
                }
            }
            currentIp = InetAddress.getByAddress(address);
            return InetAddress.getByAddress(host, address);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    public static synchronized void cleanup(String host, Supplier<Boolean> shouldCleanup) {
        if (shouldCleanup.get()) hostAddressMap.remove(host);
    }
}
