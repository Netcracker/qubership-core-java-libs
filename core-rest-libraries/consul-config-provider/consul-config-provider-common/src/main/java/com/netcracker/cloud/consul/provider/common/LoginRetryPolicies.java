package com.netcracker.cloud.consul.provider.common;

import net.jodah.failsafe.RetryPolicy;

import java.io.IOException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

//todo vlla javadoc
final class LoginRetryPolicies {

    static final double JITTER = 0.25;
    private static final int MAX_BACKOFF_FACTOR = 8;

    private LoginRetryPolicies() {
    }

    static <T> RetryPolicy<T> onTransportFailure(int attempts, Duration delay) {
        RetryPolicy<T> policy = new RetryPolicy<T>()
                .handle(IOException.class)
                .withMaxAttempts(attempts);
        if (delay.isZero() || delay.isNegative()) {
            return policy;
        }
        return policy
                .withBackoff(delay.toMillis(), delay.multipliedBy(MAX_BACKOFF_FACTOR).toMillis(), ChronoUnit.MILLIS)
                .withJitter(JITTER);
    }
}
