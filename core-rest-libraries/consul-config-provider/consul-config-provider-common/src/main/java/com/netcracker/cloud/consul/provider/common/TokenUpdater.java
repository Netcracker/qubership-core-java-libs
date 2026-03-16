package com.netcracker.cloud.consul.provider.common;

import com.netcracker.cloud.consul.provider.common.client.ConsulClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class TokenUpdater {

    private static final Logger log = LoggerFactory.getLogger(TokenUpdater.class);
    private static final int DEFAULT_TRIES = 10;
    private final TokenProvider tokenProvider;
    private ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    private Clock clock = Clock.systemDefaultZone();
    private final Integer tries;

    public TokenUpdater(ConsulClient client, String authMethod) {
        this.tokenProvider = new TokenProvider(client, authMethod);
        this.tries = DEFAULT_TRIES;
    }

    TokenUpdater(TokenProvider tokenProvider, ScheduledExecutorService executor, Clock clock, int tries) {
        this.tokenProvider = tokenProvider;
        this.executor = executor;
        this.clock = clock;
        this.tries = tries;
    }

    synchronized public void watch(Consumer<String> updater, String currentSecretId) {
        log.debug("Start token refreshing process for consul");
        Token token;
        if (currentSecretId == null || currentSecretId.isEmpty()) {
            token = withRetry(unused -> tokenProvider.getNewConsulToken(), tries);
            updater.accept(token.getSecretId());
        } else {
            token = withRetry(unused -> tokenProvider.getSelf(currentSecretId), tries);
        }

        if (token.getExpirationTime() != null) {
            long delay = ChronoUnit.SECONDS.between(OffsetDateTime.now(clock), token.getExpirationTime().minusMinutes(5));
            Runnable task = () -> {
                log.debug("Get new consul token with {} retry attempts", tries);
                try {
                    Token newToken = withRetry(unused -> tokenProvider.getNewConsulToken(), tries);
                    updater.accept(newToken.getSecretId());
                } catch (Exception e) {
                    log.error("Error occurred during getting new consul token. Will try in {} second.", delay, e);
                }
            };
            executor.scheduleWithFixedDelay(task, delay, delay, TimeUnit.SECONDS);
        }
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
            }
        }
    }

    @FunctionalInterface
    public interface CheckedFunction<T, R> {
        R apply(T t) throws IOException;
    }
}
