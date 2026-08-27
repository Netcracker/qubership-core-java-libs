package com.netcracker.cloud.consul.provider.common;

import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.FailsafeException;
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

public class TokenUpdater {

    private static final Logger log = LoggerFactory.getLogger(TokenUpdater.class);
    static final int DEFAULT_TRIES = 10;
    private static final Duration DEFAULT_RETRY_PAUSE = Duration.ofSeconds(1);
    private static final double DELAY_FRACTION = 0.8;
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

    synchronized public void watch(Consumer<String> updater, String currentSecretId) {
        log.debug("Start token refreshing process for consul");
        Token token;
        if (currentSecretId == null || currentSecretId.isEmpty()) {
            token = withRetry(unused -> tokenProvider.getToken(), tries);
            updater.accept(token.getSecretId());
        } else {
            token = withRetry(unused -> selfTokenReader.read(currentSecretId), tries);
        }

        if (token.getExpirationTime() != null) {
            long delay = reloginDelaySeconds(token.getExpirationTime());
            Runnable task = () -> {
                log.debug("Get new consul token with {} retry attempts", tries);
                try {
                    Token newToken = withRetry(unused -> tokenProvider.getToken(), tries);
                    updater.accept(newToken.getSecretId());
                } catch (Exception e) {
                    log.error("Error occurred during getting new consul token. Will try in {} second.", delay, e);
                }
            };
            executor.scheduleWithFixedDelay(task, delay, delay, TimeUnit.SECONDS);
        }
    }

    private long reloginDelaySeconds(OffsetDateTime expirationTime) {
        long remaining = ChronoUnit.SECONDS.between(OffsetDateTime.now(clock), expirationTime);
        return Math.max((long) (remaining * DELAY_FRACTION), MIN_DELAY_SECONDS);
    }

    private Token withRetry(CheckedFunction<Void, Token> c, int tries) {
        try {
            return Failsafe.with(LoginRetryPolicies.<Token>onTransportFailure(tries, retryPause)
                            .onFailedAttempt(event -> log.debug("Failed attempt {} to get a consul token",
                                    event.getAttemptCount(), event.getLastFailure())))
                    .get(() -> c.apply(null));
        } catch (FailsafeException e) {
            throw new RuntimeException("can not update consul token: ", e.getCause());
        }
    }

    @FunctionalInterface
    public interface CheckedFunction<T, R> {
        R apply(T t) throws IOException;
    }
}
