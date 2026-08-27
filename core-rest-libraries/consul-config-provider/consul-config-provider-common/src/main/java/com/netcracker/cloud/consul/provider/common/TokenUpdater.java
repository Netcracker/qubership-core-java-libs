package com.netcracker.cloud.consul.provider.common;

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
    //todo vlla это СУПЕР не понятные и излишние константы. Почеме не просто 0.8?
    private static final long DELAY_NUMERATOR = 8;
    private static final long DELAY_DENOMINATOR = 10;
    static final long MIN_DELAY_SECONDS = 10;
    private final ConsulLogin consulLogin;
    private final SelfTokenReader selfTokenReader;
    private ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    private Clock clock = Clock.systemDefaultZone();
    private final Integer tries;
    private final Duration retryPause;

    public TokenUpdater(ConsulLogin consulLogin, SelfTokenReader selfTokenReader) {
        this.consulLogin = consulLogin;
        this.selfTokenReader = selfTokenReader;
        this.tries = DEFAULT_TRIES;
        this.retryPause = DEFAULT_RETRY_PAUSE;
    }

    TokenUpdater(ConsulLogin consulLogin, SelfTokenReader selfTokenReader, ScheduledExecutorService executor, Clock clock, int tries, Duration retryPause) {
        this.consulLogin = consulLogin;
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
            token = withRetry(unused -> consulLogin.perform(), tries);
            updater.accept(token.getSecretId());
        } else {
            token = withRetry(unused -> selfTokenReader.read(currentSecretId), tries);
        }

        if (token.getExpirationTime() != null) {
            long delay = relogonDelay(token.getExpirationTime());
            Runnable task = () -> {
                log.debug("Get new consul token with {} retry attempts", tries);
                try {
                    Token newToken = withRetry(unused -> consulLogin.perform(), tries);
                    updater.accept(newToken.getSecretId());
                } catch (Exception e) {
                    log.error("Error occurred during getting new consul token. Will try in {} second.", delay, e);
                }
            };
            executor.scheduleWithFixedDelay(task, delay, delay, TimeUnit.SECONDS);
        }
    }

    //todo vlla неудачное название
    private long relogonDelay(OffsetDateTime expirationTime) {
        long remaining = ChronoUnit.SECONDS.between(OffsetDateTime.now(clock), expirationTime);
        return Math.max(remaining * DELAY_NUMERATOR / DELAY_DENOMINATOR, MIN_DELAY_SECONDS);
    }

    private Token withRetry(CheckedFunction<Void, Token> c, int tries) {
        int count = 0;
        while (true) {
            try {
                return c.apply(null);
            } catch (IOException e) {
                if (++count >= tries) {
                    throw new RuntimeException("can not update consul token: ", e);
                }
                log.debug("Failed {} retry attempt, exception: {}", count, e);
                pauseBeforeNextAttempt();
            }
        }
    }

    //todo vlla аналогично: может есть более изящный способ сделать паузы, нежели Thread.sleep в продакшен-коде?
    private void pauseBeforeNextAttempt() {
        if (retryPause.isZero() || retryPause.isNegative()) {
            return;
        }
        try {
            Thread.sleep(retryPause.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("interrupted while waiting for the next consul token attempt", e);
        }
    }

    @FunctionalInterface
    public interface CheckedFunction<T, R> {
        R apply(T t) throws IOException;
    }
}
