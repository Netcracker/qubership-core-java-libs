package com.netcracker.cloud.junit.cloudcore.extension.provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.spi.InetAddressResolver;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PortForwardAwareAddressResolverTest {

    private static final String TEST_HOST = "PortForwardAwareAddressResolverTest-test-host";
    private static final String UNKNOWN_HOST = "unknown-host";
    private static final byte[] TEST_IP_ADDRESS = new byte[]{8, 8, 8, 8};
    private static final String EXPECTED_HOSTNAME = "dns.google";

    @Mock
    private InetAddressResolver builtinResolver;

    private PortForwardAwareAddressResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new PortForwardAwareAddressResolver(builtinResolver);
    }

    @Test
    void lookupByName_WhenLocalHostAddressGeneratorReturnsAddress_ShouldReturnThatAddress() throws UnknownHostException {
        // Arrange
        InetAddress expectedAddress = InetAddress.getByName("127.0.0.1");
        LocalHostAddressGenerator.put(TEST_HOST, expectedAddress);

        // Act
        Stream<InetAddress> result = resolver.lookupByName(TEST_HOST, null);

        // Assert
        verifyStreamContainsSingleAddress(result, expectedAddress);
        verifyNoInteractions(builtinResolver);
    }

    @Test
    void lookupByName_WhenLocalHostAddressGeneratorReturnsNull_ShouldUseBuiltinResolver() throws UnknownHostException {
        // Arrange
        InetAddress expectedAddress = InetAddress.getByName("8.8.8.8");
        when(builtinResolver.lookupByName(eq(UNKNOWN_HOST), any())).thenReturn(Stream.of(expectedAddress));

        // Act
        Stream<InetAddress> result = resolver.lookupByName(UNKNOWN_HOST, null);

        // Assert
        verifyStreamContainsSingleAddress(result, expectedAddress);
        verify(builtinResolver).lookupByName(UNKNOWN_HOST, null);
    }

    @Test
    void lookupByAddress_ShouldDelegateToBuiltinResolver() throws UnknownHostException {
        // Arrange
        when(builtinResolver.lookupByAddress(TEST_IP_ADDRESS)).thenReturn(EXPECTED_HOSTNAME);

        // Act
        String result = resolver.lookupByAddress(TEST_IP_ADDRESS);

        // Assert
        assertEquals(EXPECTED_HOSTNAME, result);
        verify(builtinResolver).lookupByAddress(TEST_IP_ADDRESS);
    }

    @Test
    void lookupByAddress_WhenBuiltinResolverThrowsException_ShouldPropagateException() throws UnknownHostException {
        // Arrange
        when(builtinResolver.lookupByAddress(TEST_IP_ADDRESS))
            .thenThrow(new UnknownHostException("Test exception"));

        // Act & Assert
        assertThrows(UnknownHostException.class, () -> resolver.lookupByAddress(TEST_IP_ADDRESS));
        verify(builtinResolver).lookupByAddress(TEST_IP_ADDRESS);
    }

    private void verifyStreamContainsSingleAddress(Stream<InetAddress> stream, InetAddress expectedAddress) {
        assertNotNull(stream);
        List<InetAddress> addresses = stream.toList();
        assertEquals(1, addresses.size(), "Stream should contain exactly one address");
        assertEquals(expectedAddress, addresses.getFirst(), "Stream should contain the expected address");
    }
}
