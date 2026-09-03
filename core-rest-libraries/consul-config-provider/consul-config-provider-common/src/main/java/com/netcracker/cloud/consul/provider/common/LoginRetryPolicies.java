package com.netcracker.cloud.consul.provider.common;

import net.jodah.failsafe.RetryPolicy;

import java.io.IOException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

/**
 * Retry policies shared by the login callers. Only {@link IOException} is retried: by the exception contract of the
 * module it means a transport failure or a non-2xx answer, where another attempt may help. Anything else — a missing
 * projected token, an answer without a {@code SecretID} — comes out the same however many times it is tried.
 */
final class LoginRetryPolicies {

    static final double JITTER = 0.25;
    private static final int MAX_BACKOFF_FACTOR = 8;

    private LoginRetryPolicies() {
    }

    /**
     * Builds a policy of at most {@code attempts} attempts with an exponential delay and jitter, so that a fleet
     * restarted during a Consul outage does not retry in lockstep. A zero or negative {@code delay} retries without
     * waiting, which keeps tests fast.
     */
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
