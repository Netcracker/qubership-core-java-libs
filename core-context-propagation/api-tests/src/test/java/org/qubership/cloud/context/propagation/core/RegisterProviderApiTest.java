package org.qubership.cloud.context.propagation.core;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RegisterProviderApiTest {

    @Test
    public void checkRegisterProviderAnnotationName(){
        assertEquals("org.qubership.cloud.context.propagation.core.RegisterProvider", RegisterProvider.class.getName());
    }
}
