package org.qubership.cloud.context.propagation.spring.common.annotation;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class EnableSpringContextProviderApiTest {

    @Test
    public void checkEnableSpringContextProviderAnnotationName() {
        assertEquals("org.qubership.cloud.context.propagation.spring.common.annotation.EnableSpringContextProvider", EnableSpringContextProvider.class.getName());
    }
}
