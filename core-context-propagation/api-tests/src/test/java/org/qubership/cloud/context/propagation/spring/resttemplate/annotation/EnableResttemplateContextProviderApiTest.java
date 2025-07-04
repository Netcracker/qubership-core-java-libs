package org.qubership.cloud.context.propagation.spring.resttemplate.annotation;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EnableResttemplateContextProviderApiTest {
    @Test
    public void checkEnableResttemplateContextProvider() {
        assertEquals("org.qubership.cloud.context.propagation.spring.resttemplate.annotation.EnableResttemplateContextProvider", EnableResttemplateContextProvider.class.getName());
    }
}
