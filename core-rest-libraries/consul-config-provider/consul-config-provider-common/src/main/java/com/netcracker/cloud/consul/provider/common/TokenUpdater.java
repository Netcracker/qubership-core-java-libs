package com.netcracker.cloud.consul.provider.common;

import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.FailsafeException;
import net.jodah.failsafe.RetryPolicy;
import net.jodah.failsafe.function.CheckedSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Keeps the ACL token of the pod fresh: obtains the first one, then relogins on a schedule while the pod lives. Knows
 * nothing about how the token is obtained.
 */
public class TokenUpdater {

    private static final Logger log = LoggerFactory.getLogger(TokenUpdater.class);
    private static final int DEFAULT_TRIES = 10;
    private static final Duration DEFAULT_RETRY_PAUSE = Duration.ofSeconds(1);
    private static final double DELAY_MULTIPLIER = 0.8;
    static final long MIN_DELAY_SECONDS = 10;

    private final ConsulTokenProvider tokenProvider;
    private final SelfTokenReader selfTokenReader;
    private ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    private Clock clock = Clock.systemDefaultZone();
    private final Integer tries;
    private final Duration retryPause;

    public TokenUpdater(ConsulTokenProvider tokenProvider, SelfTokenReader selfTokenReader) {
        this.tokenProvider = tokenProvider;
        this.selfTokenReader = selfTokenReader;
        this.tries = DEFAULT_TRIES;
        this.retryPause = DEFAULT_RETRY_PAUSE;
    }

    TokenUpdater(ConsulTokenProvider tokenProvider, SelfTokenReader selfTokenReader, ScheduledExecutorService executor, Clock clock, int tries, Duration retryPause) {
        this.tokenProvider = tokenProvider;
        this.selfTokenReader = selfTokenReader;
        this.executor = executor;
        this.clock = clock;
        this.tries = tries;
        this.retryPause = retryPause;
    }

    /**
     * Obtains the token and, when it expires, schedules a relogin. An empty {@code currentSecretId} means a login;
     * otherwise the pod already holds a token and only its expiration is read. Only a token with an expiration is
     * scheduled for: Consul omits the field for auth methods without {@code MaxTokenTTL}, and such a token never
     * expires.
     *
     * @param updater receives every new {@code SecretID}, including the ones from scheduled relogins
     * @throws RuntimeException when the attempts run out
     */
    synchronized public void watch(Consumer<String> updater, String currentSecretId) {
        log.debug("Start token refreshing process for consul");
        Token token;
        if (currentSecretId == null || currentSecretId.isEmpty()) {
            token = withRetry(tokenProvider::getToken, tries);
            updater.accept(token.getSecretId());
        } else {
            token = withRetry(() -> selfTokenReader.read(currentSecretId), tries);
        }

        if (token.getExpirationTime() != null) {
            scheduleRelogin(updater, reloginDelaySeconds(token.getExpirationTime()));
        }
    }

    /**
     * Schedules one relogin and, from its result, the next one. The period follows the expiration of the token just
     * received rather than of the first one: the way of obtaining the token can change while the pod lives, and the
     * two auth methods carry different {@code MaxTokenTTL}. A failed relogin keeps the previous period, and a token
     * without an expiration ends the schedule.
     */
    private void scheduleRelogin(Consumer<String> updater, long delay) {
        executor.schedule(() -> {
            log.debug("Get new consul token with {} retry attempts", tries);
            try {
                Token newToken = withRetry(tokenProvider::getToken, tries);
                updater.accept(newToken.getSecretId());
                if (newToken.getExpirationTime() == null) {
                    log.debug("Consul token has no expiration time, stop refreshing");
                    return;
                }
                scheduleRelogin(updater, reloginDelaySeconds(newToken.getExpirationTime()));
            } catch (Exception e) {
                log.error("Error occurred during getting new consul token. Will try in {} seconds.", delay, e);
                scheduleRelogin(updater, delay);
            }
        }, delay, TimeUnit.SECONDS);
    }

    /**
     * Returns the delay before the next relogin as a share of the remaining lifetime. The same value serves as the
     * period of the schedule, so a constant offset from the expiration would degenerate on short-lived tokens. The
     * lower bound covers clocks that ran ahead of the expiration.
     */
    private long reloginDelaySeconds(OffsetDateTime expirationTime) {
        long remaining = ChronoUnit.SECONDS.between(OffsetDateTime.now(clock), expirationTime);
        return Math.max((long) (remaining * DELAY_MULTIPLIER), MIN_DELAY_SECONDS);
    }

    private Token withRetry(CheckedSupplier<Token> c, int tries) {
        try {
            return Failsafe.with(getRetryPolicy(tries)).get(c);
        } catch (FailsafeException e) {
            throw new RuntimeException("can not update consul token: ", e.getCause());
        }
    }

    private RetryPolicy<Token> getRetryPolicy(int tries) {
        return LoginRetryPolicies.<Token>onTransportFailure(tries, retryPause)
                .onFailedAttempt(event -> log.debug("Failed attempt {} to get a consul token",
                        event.getAttemptCount(), event.getLastFailure()));
    }
}
