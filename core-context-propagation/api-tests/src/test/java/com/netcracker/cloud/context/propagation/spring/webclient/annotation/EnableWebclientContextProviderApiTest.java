package com.netcracker.cloud.context.propagation.spring.webclient.annotation;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EnableWebclientContextProviderApiTest {

    @Test
    void checkEnableWebclientContextProvider() {
        assertEquals("com.netcracker.cloud.context.propagation.spring.webclient.annotation.EnableWebclientContextProvider", EnableWebclientContextProvider.class.getName());
    }
}
