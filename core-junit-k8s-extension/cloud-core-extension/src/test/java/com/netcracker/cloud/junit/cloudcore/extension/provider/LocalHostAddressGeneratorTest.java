package com.netcracker.cloud.junit.cloudcore.extension.provider;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
public class LocalHostAddressGeneratorTest {

    @BeforeEach
    void setUp() {
        LocalHostAddressGenerator.setup();
    }

    @AfterEach
    void tearDown() {
        LocalHostAddressGenerator.cleanupAll();
    }

    @Test
    void testSetup() {
        InetAddress address = LocalHostAddressGenerator.get("test-host");
        assertNull(address);
    }

    @Test
    void testAddressGenerationAndReuse() {
        try (MockedConstruction<Socket> mockSocket = Mockito.mockConstruction(Socket.class, (mock, context) -> {
            doThrow(new IOException()).when(mock).connect(any(SocketAddress.class), anyInt());
        })) {
            // Test initial address generation
            InetAddress address1 = LocalHostAddressGenerator.getOrNext("test1");
            verifyAddress(address1, "127.0.0.2");
            LocalHostAddressGenerator.put("test1", address1);

            // Test new address generation
            InetAddress address2 = LocalHostAddressGenerator.getOrNext("test2");
            verifyAddress(address2, "127.0.0.3");
            LocalHostAddressGenerator.put("test2", address2);

            // Test address reuse
            InetAddress address3 = LocalHostAddressGenerator.getOrNext("test1");
            verifyAddress(address3, "127.0.0.2");
            assertEquals(address1, address3, "Should return the same address instance");
        }
    }

    @Test
    void testGetAndPut() {
        // Test get with non-existent host
        assertNull(LocalHostAddressGenerator.get("non-existent-host"));

        // Test put and get
        InetAddress testAddress = LocalHostAddressGenerator.getOrNext("test-host");
        LocalHostAddressGenerator.put("test-host", testAddress);
        assertEquals(testAddress, LocalHostAddressGenerator.get("test-host"));
    }

    @Test
    void testGetOrNext() {
        // Test getting new address for non-existent host
        InetAddress address1 = LocalHostAddressGenerator.getOrNext("test-host-1");
        LocalHostAddressGenerator.put("test-host-1", address1);
        verifyAddress(address1, "127.0.0.2");

        // Test getting same address for same host
        InetAddress address2 = LocalHostAddressGenerator.getOrNext("test-host-1");
        assertEquals(address1, address2);

        // Test getting next address for different host
        InetAddress address3 = LocalHostAddressGenerator.getOrNext("test-host-2");
        LocalHostAddressGenerator.put("test-host-2", address3);
        verifyAddress(address3, "127.0.0.3");
    }

    @ParameterizedTest
    @CsvSource({
        "127.0.0.1, 127.0.0.2",
        "127.1.2.10, 127.1.2.11",
        "127.0.0.255, 127.0.1.0",
        "127.0.255.255, 127.1.0.0"
    })
    void testGetNextAddress(String currentIp, String expectedNextIp) throws UnknownHostException {
        LocalHostAddressGenerator.setCurrentIp(InetAddress.getByName(currentIp));
        InetAddress nextAddress = LocalHostAddressGenerator.getNextAddress("test-host-3");
        verifyAddress(nextAddress, expectedNextIp);
    }

    @Test
    void testGetNextAddressOutOfBoundaries() throws UnknownHostException {
        LocalHostAddressGenerator.setCurrentIp(InetAddress.getByName("127.255.255.254"));
        assertThrows(IllegalArgumentException.class, () -> LocalHostAddressGenerator.getNextAddress("test-host-3"));
    }

    private void verifyAddress(InetAddress address, String expectedIp) {
        assertNotNull(address, "Address should not be null");
        assertEquals(expectedIp, address.getHostAddress(), "IP address mismatch");
    }
}
