package org.qubership.cloud.restlegacy.restclient.retry;

import org.junit.Test;

public class RetryTemplateBuilderTest {

    @Test(expected = IllegalStateException.class)
    public void testIncorrectCreateBuilder(){
        new RetryTemplateBuilder().withCircuitBreakerOpenTimeoutInMillis(100L).build();
    }
}
