package org.qubership.cloud.consul.provider.common;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class TokenUpdaterTest {
    private TokenUpdater tokenUpdater;
    private TokenProvider tokenProvider;
    private ScheduledExecutorService scheduledExecutorService;
    private final Instant currentTime = Instant.now();

    @BeforeEach
    public void init() {

        tokenProvider = Mockito.mock(TokenProvider.class);
        scheduledExecutorService = Mockito.mock(ScheduledExecutorService.class);
        tokenUpdater = new TokenUpdater(tokenProvider, scheduledExecutorService, Clock.fixed(currentTime, ZoneId.of("UTC")), 2);
    }

    @Test
    void mustGetNewTokenScheduleUpdates() throws IOException {
        String secretId = "test-token";
        OffsetDateTime secretExpirationTime = OffsetDateTime.ofInstant(currentTime, ZoneId.of("UTC")).plusMinutes(30);
        when(tokenProvider.getNewConsulToken()).thenReturn(new Token(secretId, secretExpirationTime));

        AtomicReference<String> updater = new AtomicReference<>("");
        tokenUpdater.watch(updater::set, "");
        assertEquals(secretId, updater.get());

        verify(scheduledExecutorService).scheduleWithFixedDelay(
                any(),
                eq(ChronoUnit.SECONDS.between(currentTime, secretExpirationTime.minusMinutes(5))),
                eq(ChronoUnit.SECONDS.between(currentTime, secretExpirationTime.minusMinutes(5))),
                eq(TimeUnit.SECONDS)
        );
    }

    @Test
    void mustUseSelfTokenIfProvidedScheduleUpdates() throws IOException {
        String secretId = "test-self-token";
        OffsetDateTime secretExpirationTime = OffsetDateTime.ofInstant(currentTime, ZoneId.of("UTC")).plusMinutes(30);
        when(tokenProvider.getSelf(secretId)).thenReturn(new Token(secretId, secretExpirationTime));

        AtomicReference<String> updater = new AtomicReference<>("");
        tokenUpdater.watch(updater::set, secretId);

        verify(scheduledExecutorService).scheduleWithFixedDelay(
                any(),
                eq(ChronoUnit.SECONDS.between(currentTime, secretExpirationTime.minusMinutes(5))),
                eq(ChronoUnit.SECONDS.between(currentTime, secretExpirationTime.minusMinutes(5))),
                eq(TimeUnit.SECONDS)
        );
    }

    @Test
    void mustRetryOnFailure() throws IOException {
        String secretId = "test-self-token";
        OffsetDateTime secretExpirationTime = OffsetDateTime.ofInstant(currentTime, ZoneId.of("UTC")).plusMinutes(30);
        when(tokenProvider.getNewConsulToken())
                .thenThrow(new IOException())
                .thenReturn(new Token(secretId, secretExpirationTime));

        AtomicReference<String> updater = new AtomicReference<>("");
        tokenUpdater.watch(updater::set, "");

        verify(tokenProvider, times(2)).getNewConsulToken();
    }

    @Test
    void scheduledTaskMustRetryOnFailure() throws IOException {
        String secretId = "test-token";
        OffsetDateTime secretExpirationTime = OffsetDateTime.ofInstant(currentTime, ZoneId.of("UTC")).plusMinutes(30);
        when(tokenProvider.getNewConsulToken())
                .thenReturn(new Token(secretId, secretExpirationTime))
                .thenThrow(new IOException())
                .thenThrow(new IOException())
                .thenThrow(new IOException());

        AtomicReference<String> updater = new AtomicReference<>("");

        when(scheduledExecutorService.scheduleWithFixedDelay(any(),
                eq(ChronoUnit.SECONDS.between(currentTime, secretExpirationTime.minusMinutes(5))),
                eq(ChronoUnit.SECONDS.between(currentTime, secretExpirationTime.minusMinutes(5))),
                eq(TimeUnit.SECONDS))).thenAnswer(invocationOnMock -> {
            assertEquals(secretId, updater.get());
            Runnable task = invocationOnMock.getArgument(0);
            Assertions.assertDoesNotThrow(() -> task.run());
            return null;
        });

        tokenUpdater.watch(updater::set, "");
        assertEquals(secretId, updater.get());
    }
}
