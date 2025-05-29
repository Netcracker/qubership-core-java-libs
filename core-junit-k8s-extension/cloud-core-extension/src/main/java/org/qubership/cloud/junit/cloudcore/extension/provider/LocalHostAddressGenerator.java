package org.qubership.cloud.junit.cloudcore.extension.provider;

import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Slf4j
public class LocalHostAddressGenerator {
    private static final int MAX_IP = 2147483646; //127.255.255.254

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

    protected static synchronized InetAddress getNextAddress(String host) {
        // 127.0.0.1 — 127.255.255.254
        try {
            int ipInt = ipToInt(currentIp);
            if (ipInt >= MAX_IP) {
                throw new IllegalArgumentException("The next ip is out of bounds");
            }

            ipInt++;

            byte[] address = intToIPBytes(ipInt);
            currentIp = InetAddress.getByAddress(address);
            return InetAddress.getByAddress(host, address);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    private static int ipToInt(InetAddress ip) {
        byte[] bytes = ip.getAddress();
        return ((bytes[0] & 0xFF) << 24) |
               ((bytes[1] & 0xFF) << 16) |
               ((bytes[2] & 0xFF) << 8) |
               (bytes[3] & 0xFF);
    }

    private static byte[] intToIPBytes(int ip) {
        return new byte[] {
                (byte) ((ip >> 24) & 0xFF),
                (byte) ((ip >> 16) & 0xFF),
                (byte) ((ip >> 8) & 0xFF),
                (byte) (ip & 0xFF)
        };
    }

    public static synchronized void cleanup(String host, Supplier<Boolean> shouldCleanup) {
        if (shouldCleanup.get()) hostAddressMap.remove(host);
    }

    /**
     * FOR TEST PURPOSES ONLY
     */
    static void setCurrentIp(InetAddress inetAddress) {
        currentIp = inetAddress;
    }

    /**
     * FOR TEST PURPOSES ONLY
     */
    static synchronized void cleanupAll() {
        hostAddressMap.clear();
    }
}
