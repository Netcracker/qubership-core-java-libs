package org.qubership.cloud.framework.contexts.tenant;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TenantNotFoundExceptionTest {

    @Test
    void testConstructorWithMessage() {
        String errorMessage = "Tenant not found.";
        TenantNotFoundException exception = new TenantNotFoundException(errorMessage);
        assertEquals(errorMessage, exception.getMessage());
    }

    @Test
    void testConstructorWithEmptyMessage() {
        String errorMessage = "";
        TenantNotFoundException exception = new TenantNotFoundException(errorMessage);
        assertEquals(errorMessage, exception.getMessage());
    }
}

