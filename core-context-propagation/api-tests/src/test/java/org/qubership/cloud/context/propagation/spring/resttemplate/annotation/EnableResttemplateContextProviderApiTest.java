package org.qubership.cloud.context.propagation.spring.resttemplate.annotation;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EnableResttemplateContextProviderApiTest {
    @Test
    void checkEnableResttemplateContextProvider() {
        assertEquals("org.qubership.cloud.context.propagation.spring.resttemplate.annotation.EnableResttemplateContextProvider", EnableResttemplateContextProvider.class.getName());
    }
}
