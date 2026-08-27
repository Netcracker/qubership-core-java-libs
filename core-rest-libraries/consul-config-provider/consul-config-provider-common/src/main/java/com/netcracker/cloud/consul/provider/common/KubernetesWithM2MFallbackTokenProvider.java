package com.netcracker.cloud.consul.provider.common;

import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.FailsafeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;

/**
 * Probes the kubernetes way and falls back to m2m if the probe fails. The choice sticks until the pod restarts, so a
 * later failure never switches the way back, and the pod logs one {@code INFO} record telling which way it settled on.
 * The whole class goes away with the m2m way, which is why it names the pair it serves instead of taking two
 * interchangeable providers.
 */
final class KubernetesWithM2MFallbackTokenProvider implements ConsulTokenProvider {

    static final int PROBE_TRIES = 3;
    static final int ERROR_MESSAGE_LIMIT = 512;

    private static final Duration DEFAULT_PROBE_PAUSE = Duration.ofSeconds(1);
    private static final String KUBERNETES_WAY = "kubernetes";
    private static final String M2M_WAY = "m2m";

    private static final Logger log = LoggerFactory.getLogger(KubernetesWithM2MFallbackTokenProvider.class);

    private final ConsulTokenProvider kubernetesProvider;
    private final ConsulTokenProvider m2mProvider;
    private final int tries;
    private final Duration probeDelay;

    private volatile ConsulTokenProvider chosen;

    KubernetesWithM2MFallbackTokenProvider(ConsulTokenProvider kubernetesProvider, ConsulTokenProvider m2mProvider) {
        this(kubernetesProvider, m2mProvider, PROBE_TRIES, DEFAULT_PROBE_PAUSE);
    }

    KubernetesWithM2MFallbackTokenProvider(ConsulTokenProvider kubernetesProvider, ConsulTokenProvider m2mProvider, int tries, Duration probeDelay) {
        this.kubernetesProvider = kubernetesProvider;
        this.m2mProvider = m2mProvider;
        this.tries = tries;
        this.probeDelay = probeDelay;
    }

    /**
     * Returns a token from the way already chosen, or, on the first call, chooses one. The probe spends fewer attempts
     * than the scheduler so that an unmigrated pod pays little for it. Any {@link Exception} out of the kubernetes way
     * means the fallback; an {@link Error} passes through untouched.
     */
    @Override
    public synchronized Token getToken() throws IOException {
        if (chosen != null) {
            return chosen.getToken();
        }
        try {
            Token token = probe();
            chosen = kubernetesProvider;
            log.info("Consul ACL token is obtained by the {} auth method", KUBERNETES_WAY);
            return token;
        } catch (Exception e) {
            chosen = m2mProvider;
            log.info("Consul login by the {} auth method failed, falling back to the {} one until the pod restarts: {}",
                    KUBERNETES_WAY, M2M_WAY, describe(e));
            return m2mProvider.getToken();
        }
    }

    private Token probe() throws IOException {
        try {
            return Failsafe.with(LoginRetryPolicies.<Token>onTransportFailure(tries, probeDelay)
                            .onFailedAttempt(event -> log.debug("Failed probe attempt {} of the {} auth method",
                                    event.getAttemptCount(), KUBERNETES_WAY, event.getLastFailure())))
                    .get(kubernetesProvider::getToken);
        } catch (FailsafeException e) {
            throw (IOException) e.getCause();
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
