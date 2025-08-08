package com.netcracker.cloud.context.propagation.spring.common.annotation;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EnableSpringContextProviderApiTest {

    @Test
    void checkEnableSpringContextProviderAnnotationName() {
        assertEquals("org.qubership.cloud.context.propagation.spring.common.annotation.EnableSpringContextProvider", EnableSpringContextProvider.class.getName());
    }
}
