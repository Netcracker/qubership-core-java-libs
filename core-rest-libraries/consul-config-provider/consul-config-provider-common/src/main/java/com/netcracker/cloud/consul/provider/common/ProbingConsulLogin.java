package com.netcracker.cloud.consul.provider.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;

final class ProbingConsulLogin implements ConsulLogin {

    static final int PROBE_TRIES = 3;
    static final int ERROR_MESSAGE_LIMIT = 512;

    private static final Duration DEFAULT_PROBE_PAUSE = Duration.ofSeconds(1);
    private static final String KUBERNETES_WAY = "kubernetes";
    private static final String M2M_WAY = "m2m";

    private static final Logger log = LoggerFactory.getLogger(ProbingConsulLogin.class);

    private final ConsulLogin probed;
    private final ConsulLogin fallback;
    private final int tries;
    private final Duration probePause;

    private volatile ConsulLogin chosen;

    ProbingConsulLogin(ConsulLogin probed, ConsulLogin fallback) {
        this(probed, fallback, PROBE_TRIES, DEFAULT_PROBE_PAUSE);
    }

    ProbingConsulLogin(ConsulLogin probed, ConsulLogin fallback, int tries, Duration probePause) {
        this.probed = probed;
        this.fallback = fallback;
        this.tries = tries;
        this.probePause = probePause;
    }

    @Override
    public synchronized Token perform() throws IOException {
        if (chosen != null) {
            return chosen.perform();
        }
        try {
            Token token = probe();
            chosen = probed;
            log.info("Consul ACL token is obtained by the {} auth method", KUBERNETES_WAY);
            return token;
        } catch (Exception e) {
            chosen = fallback;
            log.info("Consul login by the {} auth method failed, falling back to the {} one until the pod restarts: {}",
                    KUBERNETES_WAY, M2M_WAY, describe(e));
            return fallback.perform();
        }
    }

    private Token probe() throws IOException {
        int count = 0;
        while (true) {
            try {
                return probed.perform();
            } catch (IOException e) {
                if (++count >= tries) {
                    throw e;
                }
                pauseBeforeNextAttempt();
            }
        }
    }

    private void pauseBeforeNextAttempt() {
        if (probePause.isZero() || probePause.isNegative()) {
            return;
        }
        try {
            Thread.sleep(probePause.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("interrupted while probing the consul login", e);
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
