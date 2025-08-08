package org.qubership.cloud.dbaas.client.cassandra.auth;

import com.datastax.oss.driver.api.core.metadata.EndPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;


public class DbaaSAuthProviderTest {

    private EndPoint mockEndpoint = mock(EndPoint.class);

    @Test
    public void testPlainPassword() {
        DbaaSAuthProvider authProvider = new DbaaSAuthProvider("user", "plain-password");
        authProvider.newAuthenticator(mockEndpoint, "");
    }
}
