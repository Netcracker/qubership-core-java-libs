package org.qubership.cloud.context.propagation.spring.resttemplate.annotation;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class EnableResttemplateContextProviderApiTest {
    @Test
    public void checkEnableResttemplateContextProvider() {
        assertEquals("org.qubership.cloud.context.propagation.spring.resttemplate.annotation.EnableResttemplateContextProvider", EnableResttemplateContextProvider.class.getName());
    }
}
