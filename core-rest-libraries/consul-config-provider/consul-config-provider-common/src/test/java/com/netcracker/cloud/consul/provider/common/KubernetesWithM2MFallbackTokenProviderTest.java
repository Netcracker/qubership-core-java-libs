package com.netcracker.cloud.consul.provider.common;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class KubernetesWithM2MFallbackTokenProviderTest {

    private static final String BEARER_TOKEN = "my-secret-bearer-token";
    private static final String SECRET_ID = "my-secret-acl-token";

    private static final String AUTH_METHOD_NOT_FOUND =
            "ACL not found: auth method \"k8s-does-not-exist\" not found";
    private static final String TOKEN_REVIEW_UNREACHABLE =
            "Post \"https://kubernetes.default.svc/apis/authentication.k8s.io/v1/tokenreviews\": "
                    + "dial tcp 10.96.0.1:443: connect: connection refused";

    private ConsulTokenProvider kubernetes;
    private ConsulTokenProvider m2m;
    private ListAppender<ILoggingEvent> appender;
    private ch.qos.logback.classic.Logger logger;

    @BeforeEach
    void init() {
        kubernetes = mock(ConsulTokenProvider.class);
        m2m = mock(ConsulTokenProvider.class);

        logger = ((LoggerContext) LoggerFactory.getILoggerFactory()).getLogger(KubernetesWithM2MFallbackTokenProvider.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.INFO);
    }

    @AfterEach
    void detachAppender() {
        logger.detachAppender(appender);
    }

    private KubernetesWithM2MFallbackTokenProvider probing() {
        return new KubernetesWithM2MFallbackTokenProvider(kubernetes, m2m, 2, Duration.ZERO);
    }

    private List<String> infoRecords() {
        return appender.list.stream()
                .filter(event -> event.getLevel() == Level.INFO)
                .map(ILoggingEvent::getFormattedMessage)
                .collect(Collectors.toList());
    }

    @Test
    void successOnFirstAttemptSticksToTheNewWay() throws IOException {
        Token token = new Token(SECRET_ID, null);
        when(kubernetes.getToken()).thenReturn(token);

        KubernetesWithM2MFallbackTokenProvider probing = probing();
        assertEquals(token, probing.getToken());
        assertEquals(token, probing.getToken());

        verify(kubernetes, times(2)).getToken();
        verifyNoInteractions(m2m);
    }

    @Test
    void anyExceptionFromTheNewWayLeadsToTheOldOneAndSticksIt() throws IOException {
        Token token = new Token(SECRET_ID, null);
        when(kubernetes.getToken()).thenThrow(new IllegalArgumentException("Unknown token audience: netcracker"));
        when(m2m.getToken()).thenReturn(token);

        KubernetesWithM2MFallbackTokenProvider probing = probing();
        assertEquals(token, probing.getToken());
        assertEquals(token, probing.getToken());

        verify(kubernetes, times(1)).getToken();
        verify(m2m, times(2)).getToken();
    }

    @Test
    void errorPassesThrough() throws IOException {
        when(kubernetes.getToken()).thenThrow(new Error("Unable to locate implementation for TokenSource"));

        assertThrows(Error.class, () -> probing().getToken());
        verifyNoInteractions(m2m);
    }

    @Test
    void probeSpendsFewerTriesThanTokenUpdater() throws IOException {
        when(kubernetes.getToken()).thenThrow(new IOException(
                "login to consul failed: response code=500; body='" + TOKEN_REVIEW_UNREACHABLE + "'"));
        when(m2m.getToken()).thenReturn(new Token(SECRET_ID, null));

        new KubernetesWithM2MFallbackTokenProvider(kubernetes, m2m).getToken();

        verify(kubernetes, times(KubernetesWithM2MFallbackTokenProvider.PROBE_TRIES)).getToken();
    }

    @Test
    void chosenWayIsLoggedOnceOnSuccess() throws IOException {
        when(kubernetes.getToken()).thenReturn(new Token(SECRET_ID, null));

        KubernetesWithM2MFallbackTokenProvider probing = probing();
        probing.getToken();
        probing.getToken();

        List<String> records = infoRecords();
        assertEquals(1, records.size());
        assertTrue(records.get(0).contains("kubernetes"));
    }

    @Test
    void fallbackIsLoggedOnceWithReasonCodeAndBody() throws IOException {
        when(kubernetes.getToken()).thenThrow(new IOException(
                "login to consul failed: response code=500; body='" + TOKEN_REVIEW_UNREACHABLE + "'"));
        when(m2m.getToken()).thenReturn(new Token(SECRET_ID, null));

        KubernetesWithM2MFallbackTokenProvider probing = probing();
        probing.getToken();
        probing.getToken();

        List<String> records = infoRecords();
        assertEquals(1, records.size());
        assertTrue(records.get(0).contains("m2m"));
        assertTrue(records.get(0).contains("IOException"));
        assertTrue(records.get(0).contains("response code=500"));
        assertTrue(records.get(0).contains("connection refused"));
    }

    @Test
    void notReadyConfigurationIsNamedInTheFallbackRecord() throws IOException {
        when(kubernetes.getToken()).thenThrow(new IOException(
                "consul auth method is not ready: response code=403; body='" + AUTH_METHOD_NOT_FOUND + "'"));
        when(m2m.getToken()).thenReturn(new Token(SECRET_ID, null));

        probing().getToken();

        List<String> records = infoRecords();
        assertEquals(1, records.size());
        assertTrue(records.get(0).contains("not ready"));
        assertTrue(records.get(0).contains("response code=403"));
    }

    @Test
    void longErrorBodyIsTruncatedInTheFallbackRecord() throws IOException {
        String longBody = "x".repeat(4096);
        when(kubernetes.getToken()).thenThrow(new IOException(
                "login to consul failed: response code=500; body='" + longBody + "'"));
        when(m2m.getToken()).thenReturn(new Token(SECRET_ID, null));

        probing().getToken();

        String record = infoRecords().get(0);
        assertTrue(record.length() < longBody.length());
    }

    @Test
    void secretsNeverReachTheLog() throws IOException {
        when(kubernetes.getToken()).thenThrow(new IOException(
                "login to consul failed: response code=500; body='" + TOKEN_REVIEW_UNREACHABLE + "'"));
        when(m2m.getToken()).thenReturn(new Token(SECRET_ID, null));

        probing().getToken();

        String logged = String.join("\n", infoRecords());
        assertFalse(logged.contains(BEARER_TOKEN));
        assertFalse(logged.contains(SECRET_ID));
    }
}
