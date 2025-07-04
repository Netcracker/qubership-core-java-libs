package org.qubership.cloud.context.propagation.spring.webclient.annotation;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EnableWebclientContextProviderApiTest {

    @Test
    public void checkEnableWebclientContextProvider() {
        assertEquals("org.qubership.cloud.context.propagation.spring.webclient.annotation.EnableWebclientContextProvider", EnableWebclientContextProvider.class.getName());
    }
}
