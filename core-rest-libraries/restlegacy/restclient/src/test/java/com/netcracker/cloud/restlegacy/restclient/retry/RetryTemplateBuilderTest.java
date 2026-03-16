package com.netcracker.cloud.restlegacy.restclient.retry;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class RetryTemplateBuilderTest {

    @Test
    void testIncorrectCreateBuilder() {
        assertThrows(IllegalStateException.class, () -> new RetryTemplateBuilder().withCircuitBreakerOpenTimeoutInMillis(100L).build());
    }
}
