package org.qubership.cloud.consul.config.source.runtime;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ConsulHttpClientFactoryTest {

    @Test
    void increaseReadSocketTimeout_readTimeoutMustBeDefault() {
        int minuteMillis = 60 * 1000;
        Assertions.assertEquals(ConsulHttpClientFactory.DEFAULT_READ_TIMEOUT_MS, ConsulHttpClientFactory.getIncreasedReadSocketTimeoutMillis(minuteMillis));
    }

    @Test
    void increaseReadSocketTimeout_readTimeoutMustBeHigherThanInput() {
        int inputHigher = 2 * ConsulHttpClientFactory.DEFAULT_READ_TIMEOUT_MS;
        Assertions.assertTrue(ConsulHttpClientFactory.getIncreasedReadSocketTimeoutMillis(inputHigher) > ConsulHttpClientFactory.DEFAULT_READ_TIMEOUT_MS);
    }

    @Test
    void returnsNotNull() {
        Assertions.assertNotNull(ConsulHttpClientFactory.get(300));
    }
}
