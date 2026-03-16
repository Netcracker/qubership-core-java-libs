package com.netcracker.cloud.context.propagation.core;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RegisterProviderApiTest {

    @Test
    void checkRegisterProviderAnnotationName(){
        assertEquals("com.netcracker.cloud.context.propagation.core.RegisterProvider", RegisterProvider.class.getName());
    }
}
