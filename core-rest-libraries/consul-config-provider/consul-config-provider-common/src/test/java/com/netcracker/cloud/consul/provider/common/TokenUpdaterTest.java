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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class TokenUpdaterTest {
    private TokenUpdater tokenUpdater;
    private ConsulTokenProvider tokenProvider;
    private ScheduledExecutorService scheduledExecutorService;
    private final Instant currentTime = Instant.now();

    @BeforeEach
    public void init() {

        tokenProvider = Mockito.mock(ConsulTokenProvider.class);
        scheduledExecutorService = Mockito.mock(ScheduledExecutorService.class);
        tokenUpdater = new TokenUpdater(tokenProvider, scheduledExecutorService, Clock.fixed(currentTime, ZoneId.of("UTC")), 2, Duration.ZERO);
    }

    @Test
    void mustGetNewTokenScheduleUpdates() throws IOException {
        String secretId = "test-token";
        OffsetDateTime secretExpirationTime = OffsetDateTime.ofInstant(currentTime, ZoneId.of("UTC")).plusMinutes(30);
        when(tokenProvider.getToken()).thenReturn(new Token(secretId, secretExpirationTime));

        AtomicReference<String> updater = new AtomicReference<>("");
        tokenUpdater.watch(updater::set, "");
        assertEquals(secretId, updater.get());

        verify(scheduledExecutorService).schedule(
                any(Runnable.class),
                eq(1440L),
                eq(TimeUnit.SECONDS)
        );
    }

    @Test
    void mustUseSelfTokenIfProvidedScheduleUpdates() throws IOException {
        String secretId = "test-self-token";
        OffsetDateTime secretExpirationTime = OffsetDateTime.ofInstant(currentTime, ZoneId.of("UTC")).plusMinutes(30);
        when(tokenProvider.getSelfToken(secretId)).thenReturn(new Token(secretId, secretExpirationTime));

        AtomicReference<String> updater = new AtomicReference<>("");
        tokenUpdater.watch(updater::set, secretId);

        verify(scheduledExecutorService).schedule(
                any(Runnable.class),
                eq(1440L),
                eq(TimeUnit.SECONDS)
        );
    }

    @Test
    void mustRetryOnFailure() throws IOException {
        String secretId = "test-self-token";
        OffsetDateTime secretExpirationTime = OffsetDateTime.ofInstant(currentTime, ZoneId.of("UTC")).plusMinutes(30);
        when(tokenProvider.getToken())
                .thenThrow(new IOException())
                .thenReturn(new Token(secretId, secretExpirationTime));

        AtomicReference<String> updater = new AtomicReference<>("");
        tokenUpdater.watch(updater::set, "");

        verify(tokenProvider, times(2)).getToken();
    }

    @Test
    void scheduledTaskMustRetryOnFailure() throws IOException {
        String secretId = "test-token";
        OffsetDateTime secretExpirationTime = OffsetDateTime.ofInstant(currentTime, ZoneId.of("UTC")).plusMinutes(30);
        when(tokenProvider.getToken())
                .thenReturn(new Token(secretId, secretExpirationTime))
                .thenThrow(new IOException())
                .thenThrow(new IOException())
                .thenThrow(new IOException());

        AtomicReference<String> updater = new AtomicReference<>("");

        when(scheduledExecutorService.schedule(any(Runnable.class), eq(1440L), eq(TimeUnit.SECONDS)))
                .thenAnswer(invocationOnMock -> {
                    assertEquals(secretId, updater.get());
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
        when(tokenProvider.getToken())
                .thenReturn(new Token(secretId, secretExpirationTime))
                .thenReturn(new Token(rotatedSecretId, secretExpirationTime));

        AtomicReference<String> updater = new AtomicReference<>("");
        runScheduledTaskOnce();

        tokenUpdater.watch(updater::set, "");

        assertEquals(rotatedSecretId, updater.get());
        verify(tokenProvider, times(2)).getToken();
        verify(tokenProvider, never()).getSelfToken(any());
    }

    private long scheduledDelay(Instant now, OffsetDateTime expirationTime) throws IOException {
        ScheduledExecutorService executor = Mockito.mock(ScheduledExecutorService.class);
        ConsulTokenProvider login = Mockito.mock(ConsulTokenProvider.class);
        when(login.getToken()).thenReturn(new Token("test-token", expirationTime));

        new TokenUpdater(login, executor, Clock.fixed(now, ZoneId.of("UTC")), 2, Duration.ZERO).watch(unused -> {
        }, "");

        ArgumentCaptor<Long> delay = ArgumentCaptor.forClass(Long.class);
        verify(executor).schedule(any(Runnable.class), delay.capture(), eq(TimeUnit.SECONDS));
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
        when(tokenProvider.getToken())
                .thenThrow(new IOException())
                .thenReturn(new Token("test-token", secretExpirationTime));

        TokenUpdater updater = new TokenUpdater(tokenProvider, scheduledExecutorService,
                Clock.fixed(currentTime, ZoneId.of("UTC")), 2, retryPause);

        long startedAt = System.nanoTime();
        updater.watch(unused -> {
        }, "");
        long elapsed = System.nanoTime() - startedAt;

        long lowestJitteredDelay = (long) (retryPause.toNanos() * (1 - LoginRetryPolicies.JITTER));
        verify(tokenProvider, times(2)).getToken();
        Assertions.assertTrue(elapsed >= lowestJitteredDelay,
                "expected a backoff delay of at least " + lowestJitteredDelay + " ns between retries, got " + elapsed);
    }

    private void runScheduledTaskOnce() {
        runScheduledTaskOnce(null);
    }

    private void runScheduledTaskOnce(TestClock clock) {
        AtomicInteger runs = new AtomicInteger();
        when(scheduledExecutorService.schedule(any(Runnable.class), anyLong(), eq(TimeUnit.SECONDS)))
                .thenAnswer(invocationOnMock -> {
                    if (runs.getAndIncrement() == 0) {
                        if (clock != null) {
                            clock.advance(Duration.ofSeconds(invocationOnMock.<Long>getArgument(1)));
                        }
                        Runnable task = invocationOnMock.getArgument(0);
                        task.run();
                    }
                    return null;
                });
    }

    private static final class TestClock extends Clock {

        private Instant now;

        private TestClock(Instant now) {
            this.now = now;
        }

        private void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    @Test
    void nextReloginIsScheduledByTheExpirationOfTheNewToken() throws IOException {
        OffsetDateTime firstExpiration = OffsetDateTime.ofInstant(currentTime, ZoneId.of("UTC")).plusMinutes(30);
        OffsetDateTime secondExpiration = OffsetDateTime.ofInstant(currentTime, ZoneId.of("UTC")).plusMinutes(10);
        when(tokenProvider.getToken())
                .thenReturn(new Token("test-token", firstExpiration))
                .thenReturn(new Token("test-rotated-token", secondExpiration));

        runScheduledTaskOnce();
        tokenUpdater.watch(unused -> {
        }, "");

        ArgumentCaptor<Long> delays = ArgumentCaptor.forClass(Long.class);
        verify(scheduledExecutorService, times(2)).schedule(any(Runnable.class), delays.capture(), eq(TimeUnit.SECONDS));
        assertEquals(1440L, delays.getAllValues().get(0));
        assertEquals(480L, delays.getAllValues().get(1));
    }

    @Test
    void aFailedReloginRetriesBeforeTheCurrentTokenExpires() throws IOException {
        TestClock clock = new TestClock(currentTime);
        OffsetDateTime expiration = OffsetDateTime.ofInstant(currentTime, ZoneId.of("UTC")).plusMinutes(30);
        when(tokenProvider.getToken())
                .thenReturn(new Token("test-token", expiration))
                .thenThrow(new IOException())
                .thenThrow(new IOException());
        TokenUpdater updater = new TokenUpdater(tokenProvider, scheduledExecutorService, clock, 2, Duration.ZERO);

        runScheduledTaskOnce(clock);
        updater.watch(unused -> {
        }, "");

        ArgumentCaptor<Long> delays = ArgumentCaptor.forClass(Long.class);
        verify(scheduledExecutorService, times(2)).schedule(any(Runnable.class), delays.capture(), eq(TimeUnit.SECONDS));
        assertEquals(1440L, delays.getAllValues().get(0));
        assertEquals(288L, delays.getAllValues().get(1));
        Assertions.assertTrue(delays.getAllValues().get(1) < 1800L - delays.getAllValues().get(0),
                "the retry must land before the current token expires, got " + delays.getAllValues());
    }

    @Test
    void anExistingSecretIdIsReadThroughTheProviderInsteadOfALogin() throws IOException {
        String secretId = "test-token";
        when(tokenProvider.getSelfToken(secretId)).thenReturn(new Token(secretId, null, "core-k8s"));

        tokenUpdater.watch(unused -> {
        }, secretId);

        verify(tokenProvider).getSelfToken(secretId);
        verify(tokenProvider, never()).getToken();
    }

    @Test
    void aTokenWithoutExpirationStopsTheSchedule() throws IOException {
        OffsetDateTime expiration = OffsetDateTime.ofInstant(currentTime, ZoneId.of("UTC")).plusMinutes(30);
        when(tokenProvider.getToken())
                .thenReturn(new Token("test-token", expiration))
                .thenReturn(new Token("test-endless-token", null));

        runScheduledTaskOnce();
        tokenUpdater.watch(unused -> {
        }, "");

        verify(scheduledExecutorService, times(1)).schedule(any(Runnable.class), anyLong(), eq(TimeUnit.SECONDS));
    }
}
