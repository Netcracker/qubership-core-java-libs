package com.netcracker.cloud.consul.provider.common;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.LongSupplier;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class TokenUpdaterTest {

    private static final LongSupplier NO_JITTER = () -> 0L;
    private static final Duration NO_VALIDATION = Duration.ZERO;
    private static final Duration VALIDATION_INTERVAL = Duration.ofMinutes(5);

    private TokenUpdater tokenUpdater;
    private ConsulTokenProvider tokenProvider;
    private ScheduledExecutorService scheduledExecutorService;
    private final Instant currentTime = Instant.now();

    @BeforeEach
    public void init() {

        tokenProvider = Mockito.mock(ConsulTokenProvider.class);
        scheduledExecutorService = Mockito.mock(ScheduledExecutorService.class);
        tokenUpdater = new TokenUpdater(tokenProvider, scheduledExecutorService, Clock.fixed(currentTime, ZoneId.of("UTC")), 2,
                Duration.ZERO, NO_VALIDATION, NO_JITTER);
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

        new TokenUpdater(login, executor, Clock.fixed(now, ZoneId.of("UTC")), 2, Duration.ZERO, NO_VALIDATION, NO_JITTER)
                .watch(unused -> {
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
                Clock.fixed(currentTime, ZoneId.of("UTC")), 2, retryPause, NO_VALIDATION, NO_JITTER);

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
        runScheduledTasks(1, clock);
    }

    private void runScheduledTasks(int times, TestClock clock) {
        AtomicInteger runs = new AtomicInteger();
        when(scheduledExecutorService.schedule(any(Runnable.class), anyLong(), eq(TimeUnit.SECONDS)))
                .thenAnswer(invocationOnMock -> {
                    if (runs.getAndIncrement() < times) {
                        if (clock != null) {
                            clock.advance(Duration.ofSeconds(invocationOnMock.<Long>getArgument(1)));
                        }
                        Runnable task = invocationOnMock.getArgument(0);
                        task.run();
                    }
                    return null;
                });
    }


    private TokenUpdater validating(Duration validationInterval, LongSupplier jitter) {
        return new TokenUpdater(tokenProvider, scheduledExecutorService, Clock.fixed(currentTime, ZoneId.of("UTC")), 2,
                Duration.ZERO, validationInterval, jitter);
    }

    private static Token endless(String secretId) {
        return new Token(secretId, null);
    }

    @Test
    void aValidationTickThatMeetsARefusedTokenForcesARelogin() throws IOException {
        when(tokenProvider.getToken())
                .thenReturn(endless("test-token"))
                .thenReturn(endless("test-rotated-token"));
        when(tokenProvider.getSelfToken("test-token"))
                .thenThrow(new ConsulResponseException(403, "ACL not found"));

        AtomicReference<String> updater = new AtomicReference<>("");
        runScheduledTasks(2, null);
        validating(VALIDATION_INTERVAL, NO_JITTER).watch(updater::set, "");

        assertEquals("test-rotated-token", updater.get());
    }

    @Test
    void aValidationTickThatMeetsAServerErrorKeepsTheToken() throws IOException {
        when(tokenProvider.getToken()).thenReturn(endless("test-token"));
        when(tokenProvider.getSelfToken("test-token"))
                .thenThrow(new ConsulResponseException(500, "consul is unavailable"));

        AtomicReference<String> updater = new AtomicReference<>("");
        runScheduledTasks(2, null);
        validating(VALIDATION_INTERVAL, NO_JITTER).watch(updater::set, "");

        assertEquals("test-token", updater.get());
        verify(tokenProvider, times(1)).getToken();
    }

    @Test
    void aTokenWithoutExpirationIsStillValidatedOnASchedule() throws IOException {
        when(tokenProvider.getToken()).thenReturn(endless("test-endless-token"));

        validating(VALIDATION_INTERVAL, NO_JITTER).watch(unused -> {
        }, "");

        verify(scheduledExecutorService).schedule(any(Runnable.class), eq(300L), eq(TimeUnit.SECONDS));
    }

    @Test
    void aSignalAboutAnAlreadyReplacedTokenChangesNothing() throws IOException {
        when(tokenProvider.getToken()).thenReturn(endless("test-token"));

        tokenUpdater.watch(unused -> {
        }, "");
        tokenUpdater.invalidate("test-replaced-token");

        verify(tokenProvider, times(1)).getToken();
        verifyNoInteractions(scheduledExecutorService);
    }

    @Test
    void aSecondSignalDuringTheFirstReloginIsDropped() throws IOException {
        when(tokenProvider.getToken()).thenReturn(endless("test-token"));
        TokenUpdater updater = validating(NO_VALIDATION, () -> 17L);

        updater.watch(unused -> {
        }, "");
        updater.invalidate("test-token");
        updater.invalidate("test-token");

        verify(scheduledExecutorService, times(1)).schedule(any(Runnable.class), eq(17L), eq(TimeUnit.SECONDS));
    }

    @Test
    void aSecondForcedReloginWithinTheMinimumIntervalIsDropped() throws IOException {
        when(tokenProvider.getToken())
                .thenReturn(endless("test-token"))
                .thenReturn(endless("test-rotated-token"));
        TokenUpdater updater = validating(NO_VALIDATION, NO_JITTER);

        runScheduledTasks(1, null);
        updater.watch(unused -> {
        }, "");
        updater.invalidate("test-token");
        updater.invalidate("test-rotated-token");

        verify(tokenProvider, times(2)).getToken();
    }

    @Test
    void oneRecordNamesTheRefusalPerForcedRelogin() throws IOException {
        when(tokenProvider.getToken()).thenReturn(endless("test-token"));
        ch.qos.logback.classic.Logger logger =
                ((LoggerContext) LoggerFactory.getILoggerFactory()).getLogger(TokenUpdater.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.WARN);
        TokenUpdater updater = validating(NO_VALIDATION, NO_JITTER);

        try {
            updater.watch(unused -> {
            }, "");
            updater.invalidate("test-token");
            updater.invalidate("test-token");
        } finally {
            logger.detachAppender(appender);
            logger.setLevel(null);
        }

        List<String> records = appender.list.stream()
                .filter(event -> event.getLevel() == Level.WARN)
                .map(ILoggingEvent::getFormattedMessage)
                .collect(Collectors.toList());
        assertEquals(1, records.size(), records.toString());
        Assertions.assertTrue(records.get(0).contains("ACL not found"), records.get(0));
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
        TokenUpdater updater = new TokenUpdater(tokenProvider, scheduledExecutorService, clock, 2, Duration.ZERO,
                NO_VALIDATION, NO_JITTER);

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
    void consecutiveFailedReloginsDoubleTheRetryDelayUpToTheCeiling() throws IOException {
        TestClock clock = new TestClock(currentTime);
        OffsetDateTime expiration = OffsetDateTime.ofInstant(currentTime, ZoneId.of("UTC")).plusSeconds(20);
        when(tokenProvider.getToken())
                .thenReturn(new Token("test-token", expiration))
                .thenThrow(new IOException());
        TokenUpdater updater = new TokenUpdater(tokenProvider, scheduledExecutorService, clock, 1, Duration.ZERO,
                NO_VALIDATION, NO_JITTER);

        runScheduledTasks(7, clock);
        updater.watch(unused -> {
        }, "");

        ArgumentCaptor<Long> delays = ArgumentCaptor.forClass(Long.class);
        verify(scheduledExecutorService, times(8)).schedule(any(Runnable.class), delays.capture(), eq(TimeUnit.SECONDS));
        assertEquals(List.of(16L, 10L, 20L, 40L, 80L, 160L, 300L, 300L), delays.getAllValues());
    }

    @Test
    void aSuccessfulReloginResetsTheRetryDelay() throws IOException {
        TestClock clock = new TestClock(currentTime);
        OffsetDateTime firstExpiration = OffsetDateTime.ofInstant(currentTime, ZoneId.of("UTC")).plusSeconds(20);
        OffsetDateTime secondExpiration = OffsetDateTime.ofInstant(currentTime, ZoneId.of("UTC")).plusSeconds(66);
        when(tokenProvider.getToken())
                .thenReturn(new Token("test-token", firstExpiration))
                .thenThrow(new IOException())
                .thenThrow(new IOException())
                .thenReturn(new Token("test-rotated-token", secondExpiration))
                .thenThrow(new IOException());
        TokenUpdater updater = new TokenUpdater(tokenProvider, scheduledExecutorService, clock, 1, Duration.ZERO,
                NO_VALIDATION, NO_JITTER);

        runScheduledTasks(4, clock);
        updater.watch(unused -> {
        }, "");

        ArgumentCaptor<Long> delays = ArgumentCaptor.forClass(Long.class);
        verify(scheduledExecutorService, times(5)).schedule(any(Runnable.class), delays.capture(), eq(TimeUnit.SECONDS));
        assertEquals(List.of(16L, 10L, 20L, 16L, 10L), delays.getAllValues());
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

    @Test
    void anErrorInTheScheduledTaskDoesNotKillTheSchedule() throws IOException {
        OffsetDateTime expiration = OffsetDateTime.ofInstant(currentTime, ZoneId.of("UTC")).plusMinutes(30);
        when(tokenProvider.getToken())
                .thenReturn(new Token("test-token", expiration))
                .thenThrow(new Error("Unable to locate implementation for TokenSource"))
                .thenThrow(new Error("Unable to locate implementation for TokenSource"));

        runScheduledTaskOnce();
        tokenUpdater.watch(unused -> {
        }, "");

        verify(scheduledExecutorService, times(2)).schedule(any(Runnable.class), anyLong(), eq(TimeUnit.SECONDS));
    }
}
