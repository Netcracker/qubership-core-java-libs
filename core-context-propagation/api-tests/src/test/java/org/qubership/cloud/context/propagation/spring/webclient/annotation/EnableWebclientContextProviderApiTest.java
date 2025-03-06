package org.qubership.cloud.context.propagation.spring.webclient.annotation;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class EnableWebclientContextProviderApiTest {

    @Test
    public void checkEnableWebclientContextProvider() {
        assertEquals("org.qubership.cloud.context.propagation.spring.webclient.annotation.EnableWebclientContextProvider", EnableWebclientContextProvider.class.getName());
    }
}
