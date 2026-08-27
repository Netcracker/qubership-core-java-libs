package com.netcracker.cloud.consul.provider.common;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class TokenUpdaterTest {
    private TokenUpdater tokenUpdater;
    private ConsulLogin consulLogin;
    private SelfTokenReader selfTokenReader;
    private ScheduledExecutorService scheduledExecutorService;
    private final Instant currentTime = Instant.now();

    @BeforeEach
    public void init() {

        consulLogin = Mockito.mock(ConsulLogin.class);
        selfTokenReader = Mockito.mock(SelfTokenReader.class);
        scheduledExecutorService = Mockito.mock(ScheduledExecutorService.class);
        tokenUpdater = new TokenUpdater(consulLogin, selfTokenReader, scheduledExecutorService, Clock.fixed(currentTime, ZoneId.of("UTC")), 2, Duration.ZERO);
    }

    @Test
    void mustGetNewTokenScheduleUpdates() throws IOException {
        String secretId = "test-token";
        OffsetDateTime secretExpirationTime = OffsetDateTime.ofInstant(currentTime, ZoneId.of("UTC")).plusMinutes(30);
        when(consulLogin.perform()).thenReturn(new Token(secretId, secretExpirationTime));

        AtomicReference<String> updater = new AtomicReference<>("");
        tokenUpdater.watch(updater::set, "");
        assertEquals(secretId, updater.get());

        verify(scheduledExecutorService).scheduleWithFixedDelay(
                any(),
                eq(1440L),
                eq(1440L),
                eq(TimeUnit.SECONDS)
        );
    }

    @Test
    void mustUseSelfTokenIfProvidedScheduleUpdates() throws IOException {
        String secretId = "test-self-token";
        OffsetDateTime secretExpirationTime = OffsetDateTime.ofInstant(currentTime, ZoneId.of("UTC")).plusMinutes(30);
        when(selfTokenReader.read(secretId)).thenReturn(new Token(secretId, secretExpirationTime));

        AtomicReference<String> updater = new AtomicReference<>("");
        tokenUpdater.watch(updater::set, secretId);

        verify(scheduledExecutorService).scheduleWithFixedDelay(
                any(),
                eq(1440L),
                eq(1440L),
                eq(TimeUnit.SECONDS)
        );
    }

    @Test
    void mustRetryOnFailure() throws IOException {
        String secretId = "test-self-token";
        OffsetDateTime secretExpirationTime = OffsetDateTime.ofInstant(currentTime, ZoneId.of("UTC")).plusMinutes(30);
        when(consulLogin.perform())
                .thenThrow(new IOException())
                .thenReturn(new Token(secretId, secretExpirationTime));

        AtomicReference<String> updater = new AtomicReference<>("");
        tokenUpdater.watch(updater::set, "");

        verify(consulLogin, times(2)).perform();
    }

    @Test
    void scheduledTaskMustRetryOnFailure() throws IOException {
        String secretId = "test-token";
        OffsetDateTime secretExpirationTime = OffsetDateTime.ofInstant(currentTime, ZoneId.of("UTC")).plusMinutes(30);
        when(consulLogin.perform())
                .thenReturn(new Token(secretId, secretExpirationTime))
                .thenThrow(new IOException())
                .thenThrow(new IOException())
                .thenThrow(new IOException());

        AtomicReference<String> updater = new AtomicReference<>("");

        when(scheduledExecutorService.scheduleWithFixedDelay(any(),
                eq(1440L),
                eq(1440L),
                eq(TimeUnit.SECONDS))).thenAnswer(invocationOnMock -> {
            assertEquals(secretId, updater.get());
            Runnable task = invocationOnMock.getArgument(0);
            Assertions.assertDoesNotThrow(() -> task.run());
            return null;
        });

        tokenUpdater.watch(updater::set, "");
        assertEquals(secretId, updater.get());
    }

    @Test
    void scheduledTaskLoginsThroughTheSameConsulLogin() throws IOException {
        String secretId = "test-token";
        String rotatedSecretId = "test-rotated-token";
        OffsetDateTime secretExpirationTime = OffsetDateTime.ofInstant(currentTime, ZoneId.of("UTC")).plusMinutes(30);
        when(consulLogin.perform())
                .thenReturn(new Token(secretId, secretExpirationTime))
                .thenReturn(new Token(rotatedSecretId, secretExpirationTime));

        AtomicReference<String> updater = new AtomicReference<>("");
        when(scheduledExecutorService.scheduleWithFixedDelay(any(), anyLong(), anyLong(), eq(TimeUnit.SECONDS)))
                .thenAnswer(invocationOnMock -> {
                    Runnable task = invocationOnMock.getArgument(0);
                    task.run();
                    return null;
                });

        tokenUpdater.watch(updater::set, "");

        assertEquals(rotatedSecretId, updater.get());
        verify(consulLogin, times(2)).perform();
        verifyNoInteractions(selfTokenReader);
    }

    private long scheduledDelay(Instant now, OffsetDateTime expirationTime) throws IOException {
        ScheduledExecutorService executor = Mockito.mock(ScheduledExecutorService.class);
        ConsulLogin login = Mockito.mock(ConsulLogin.class);
        when(login.perform()).thenReturn(new Token("test-token", expirationTime));

        new TokenUpdater(login, Mockito.mock(SelfTokenReader.class), executor,
                Clock.fixed(now, ZoneId.of("UTC")), 2, Duration.ZERO).watch(unused -> {
        }, "");

        ArgumentCaptor<Long> delay = ArgumentCaptor.forClass(Long.class);
        verify(executor).scheduleWithFixedDelay(any(), delay.capture(), anyLong(), eq(TimeUnit.SECONDS));
        return delay.getValue();
    }

    @Test
    void delayIsAFractionOfRemainingLifetime() throws IOException {
        Instant loginTime = Instant.parse("2026-08-26T07:21:30.522472777Z");

        assertEquals(48L, scheduledDelay(loginTime, OffsetDateTime.parse("2026-08-26T07:22:30.522472777Z")));
        assertEquals(240L, scheduledDelay(loginTime, OffsetDateTime.parse("2026-08-26T07:26:30.522472777Z")));
        assertEquals(2880L, scheduledDelay(loginTime, OffsetDateTime.parse("2026-08-26T08:21:30.522472777Z")));
        assertEquals(69120L, scheduledDelay(loginTime, OffsetDateTime.parse("2026-08-27T07:21:30.522472777Z")));
    }

    @Test
    void delayKeepsLowerBoundWhenClockRanAhead() throws IOException {
        Instant clockAhead = Instant.parse("2026-08-26T09:00:00Z");

        assertEquals(TokenUpdater.MIN_DELAY_SECONDS,
                scheduledDelay(clockAhead, OffsetDateTime.parse("2026-08-26T07:26:30.522472777Z")));
    }

    @Test
    void retryWaitsBackoffDelayBetweenAttempts() throws IOException {
        Duration retryPause = Duration.ofMillis(200);
        OffsetDateTime secretExpirationTime = OffsetDateTime.ofInstant(currentTime, ZoneId.of("UTC")).plusMinutes(30);
        when(consulLogin.perform())
                .thenThrow(new IOException())
                .thenReturn(new Token("test-token", secretExpirationTime));

        TokenUpdater updater = new TokenUpdater(consulLogin, selfTokenReader, scheduledExecutorService,
                Clock.fixed(currentTime, ZoneId.of("UTC")), 2, retryPause);

        long startedAt = System.nanoTime();
        updater.watch(unused -> {
        }, "");
        long elapsed = System.nanoTime() - startedAt;

        long lowestJitteredDelay = (long) (retryPause.toNanos() * (1 - LoginRetryPolicies.JITTER));
        verify(consulLogin, times(2)).perform();
        Assertions.assertTrue(elapsed >= lowestJitteredDelay,
                "expected a backoff delay of at least " + lowestJitteredDelay + " ns between retries, got " + elapsed);
    }
}
