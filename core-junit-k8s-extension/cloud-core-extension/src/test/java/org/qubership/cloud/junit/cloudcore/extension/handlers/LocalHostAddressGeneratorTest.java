package org.qubership.cloud.junit.cloudcore.extension.handlers;

import org.qubership.cloud.junit.cloudcore.extension.provider.LocalHostAddressGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
public class LocalHostAddressGeneratorTest {

    @Test
    void testCleanup() {
        try (MockedConstruction<Socket> mockSocket = Mockito.mockConstruction(Socket.class, (mock, context) -> {
            doThrow(new IOException()).when(mock).connect(any(SocketAddress.class), anyInt());
        })) {
            InetAddress inetAddress = LocalHostAddressGenerator.getOrNext("test1");
            String ip = inetAddress.getHostAddress();
            assertEquals("127.0.0.2", ip);
            LocalHostAddressGenerator.put("test1", inetAddress);

            inetAddress = LocalHostAddressGenerator.getOrNext("test2");
            ip = inetAddress.getHostAddress();
            assertEquals("127.0.0.3", ip);
            LocalHostAddressGenerator.put("test2", inetAddress);

            inetAddress = LocalHostAddressGenerator.getOrNext("test1");
            ip = inetAddress.getHostAddress();
            assertEquals("127.0.0.2", ip);
            LocalHostAddressGenerator.put("test1", inetAddress);
        }
    }

}
