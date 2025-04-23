package org.qubership.cloud.framework.contexts.tenant;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
public class TenantNotFoundExceptionTest {

    @Test
    public void testConstructorWithMessage() {
        String errorMessage = "Tenant not found.";
        TenantNotFoundException exception = new TenantNotFoundException(errorMessage);
        assertEquals(errorMessage, exception.getMessage());
    }

    @Test
    public void testConstructorWithEmptyMessage() {
        String errorMessage = "";
        TenantNotFoundException exception = new TenantNotFoundException(errorMessage);
        assertEquals(errorMessage, exception.getMessage());
    }
}

