package com.netcracker.cloud.consul.provider.common;

import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.FailsafeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * Probes the kubernetes way and falls back to m2m if the probe fails. The fallback is temporary: every so often the
 * pod tries the kubernetes way again, and the first success switches it over for good. Going back to m2m never
 * happens, so the state is a ratchet, and a later failure of the kubernetes way is a plain failure.
 *
 * <p>The recheck rides on the scheduled relogin rather than on a timer of its own, so it happens only while someone
 * asks for tokens. The whole class goes away with the m2m way, which is why it names the pair it serves instead of
 * taking two interchangeable providers.
 */
final class KubernetesWithM2MFallbackTokenProvider implements ConsulTokenProvider {
    private static final Logger log = LoggerFactory.getLogger(KubernetesWithM2MFallbackTokenProvider.class);

    static final int PROBE_TRIES = 3;
    private static final int ERROR_MESSAGE_LIMIT = 512;
    private static final Duration DEFAULT_PROBE_PAUSE = Duration.ofSeconds(1);
    private static final String KUBERNETES_WAY = "kubernetes";
    private static final String M2M_WAY = "m2m";

    private final ConsulTokenProvider kubernetesProvider;
    private final ConsulTokenProvider m2mProvider;
    private final String kubernetesAuthMethod;
    private final int tries;
    private final Duration probeDelay;
    private final Duration recheckInterval;
    private final Clock clock;

    private volatile boolean isKubernetesConfirmed;
    private Instant fellBackAt;

    KubernetesWithM2MFallbackTokenProvider(ConsulTokenProvider kubernetesProvider, ConsulTokenProvider m2mProvider,
                                           String kubernetesAuthMethod, Duration recheckInterval) {
        this(kubernetesProvider, m2mProvider, kubernetesAuthMethod, PROBE_TRIES, DEFAULT_PROBE_PAUSE, recheckInterval,
                Clock.systemUTC());
    }

    KubernetesWithM2MFallbackTokenProvider(ConsulTokenProvider kubernetesProvider, ConsulTokenProvider m2mProvider,
                                           String kubernetesAuthMethod, int tries, Duration probeDelay,
                                           Duration recheckInterval, Clock clock) {
        this.kubernetesProvider = kubernetesProvider;
        this.m2mProvider = m2mProvider;
        this.kubernetesAuthMethod = kubernetesAuthMethod;
        this.tries = tries;
        this.probeDelay = probeDelay;
        this.recheckInterval = recheckInterval;
        this.clock = clock;
    }

    /**
     * Returns a token from the kubernetes way once it is confirmed, and otherwise probes it whenever the recheck
     * interval has passed since the last failure. The probe spends fewer attempts than the scheduler so that an
     * unmigrated pod pays little for it. Any {@link Exception} out of the kubernetes way means the m2m way for now;
     * an {@link Error} passes through untouched.
     */
    @Override
    public synchronized Token getToken() throws IOException {
        if (isKubernetesConfirmed) {
            return kubernetesProvider.getToken();
        }
        if (recheckIsDue()) {
            try {
                Token token = probeKubernetesWay();
                confirmKubernetesWay();
                return token;
            } catch (Exception e) {
                fallBack(e);
            }
        }
        return m2mProvider.getToken();
    }

    /**
     * Reads the token through the kubernetes provider and adopts the state of whichever instance obtained it. The
     * choice of provider is arbitrary: the read sends the token the pod already holds and never looks at the
     * credentials, so both give the same answer.
     *
     * <p>Without the adoption a pod whose token was obtained in an earlier phase would start over: it would probe the
     * kubernetes way again even after that way was confirmed, and one failing probe would send a migrated pod back to
     * m2m for a whole recheck interval.
     */
    @Override
    public synchronized Token getSelfToken(String currentSecretId) throws IOException {
        Token token = kubernetesProvider.getSelfToken(currentSecretId);
        adopt(token.getAuthMethod());
        return token;
    }

    private void adopt(String authMethod) {
        if (isKubernetesConfirmed || fellBackAt != null || authMethod == null) {
            return;
        }
        if (kubernetesAuthMethod.equals(authMethod)) {
            confirmKubernetesWay();
        } else {
            fellBackAt = clock.instant();
        }
    }

    private boolean recheckIsDue() {
        return fellBackAt == null || !clock.instant().isBefore(fellBackAt.plus(recheckInterval));
    }

    private void confirmKubernetesWay() {
        isKubernetesConfirmed = true;

        boolean afterFallback = fellBackAt != null;
        if (afterFallback) {
            log.info("Consul ACL token is obtained by the {} auth method from now on, the fallback to the {} one is over",
                    KUBERNETES_WAY, M2M_WAY);
        } else {
            log.info("Consul ACL token is obtained by the {} auth method", KUBERNETES_WAY);
        }
    }

    private void fallBack(Exception e) {
        if (fellBackAt == null) {
            log.info("Consul login by the {} auth method failed, falling back to the {} one and retrying it every {}: {}",
                    KUBERNETES_WAY, M2M_WAY, recheckInterval, describe(e));
        }
        fellBackAt = clock.instant();
    }

    /**
     * Runs the kubernetes login with the probe budget.
     *
     * @throws IOException the failure failsafe wrapped, unwrapped back. The caller catches {@link Exception} either
     *         way, so this is for the log: {@link #describe(Exception)} would otherwise name the record after
     *         {@code FailsafeException} instead of the response code and body that explain the fallback
     */
    private Token probeKubernetesWay() throws IOException {
        try {
            return Failsafe.with(LoginRetryPolicies.<Token>onTransportFailure(tries, probeDelay)
                            .onFailedAttempt(event -> log.debug("Failed probe attempt {} of the {} auth method",
                                    event.getAttemptCount(), KUBERNETES_WAY, event.getLastFailure())))
                    .get(kubernetesProvider::getToken);
        } catch (FailsafeException e) {
            Throwable cause = e.getCause();
            throw cause instanceof IOException ? (IOException) cause : new IOException(cause);
        }
    }

    private static String describe(Exception e) {
        String message = e.getMessage() == null ? "" : e.getMessage();
        if (message.length() > ERROR_MESSAGE_LIMIT) {
            message = message.substring(0, ERROR_MESSAGE_LIMIT) + "...";
        }
        return e.getClass().getSimpleName() + ": " + message;
    }
}
