package com.netcracker.cloud.consul.provider.common;

import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.FailsafeException;
import net.jodah.failsafe.RetryPolicy;
import net.jodah.failsafe.function.CheckedSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

/**
 * Keeps the ACL token of the pod fresh: obtains the first one, then replaces it while the pod lives. Knows nothing
 * about how the token is obtained.
 *
 * <p>Three things start a replacement. The expiration schedules one in advance. A consumer that met {@code 403}
 * reports it through {@link #reportRefusal()}. A scheduled read of the token covers a pod that sends nothing while
 * Consul stops resolving its token, and runs whether or not the token expires at all.
 *
 * <p>The last two reach Consul the same way: read the token back, and log in again only on {@code 403}. Verifying
 * before logging in keeps a {@code Permission denied} answer, which carries the same code as a token Consul cannot
 * find, from driving a login that would return a token with the same policy.
 */
public class TokenUpdater implements ConsulTokenSource {
    private static final Logger log = LoggerFactory.getLogger(TokenUpdater.class);

    private static final int DEFAULT_TRIES = 10;
    private static final Duration DEFAULT_RETRY_PAUSE = Duration.ofSeconds(1);
    private static final double DELAY_MULTIPLIER = 0.8;
    static final long MIN_DELAY_SECONDS = 10;
    static final long MAX_RETRY_DELAY_SECONDS = 300;
    static final int REFUSED = 403;
    static final Duration MIN_CHECK_INTERVAL = Duration.ofSeconds(60);
    static final long CHECK_JITTER_SECONDS = 30;

    private final ConsulTokenProvider tokenProvider;
    private ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    private Clock clock = Clock.systemDefaultZone();
    private final Integer tries;
    private final Duration retryPause;
    private final Duration validationInterval;
    private final LongSupplier checkDelaySeconds;
    private long retryDelaySeconds = MIN_DELAY_SECONDS;

    private volatile Consumer<String> updater;
    private volatile String currentSecretId;
    private volatile Instant lastCheckAt;
    private final AtomicBoolean checkInFlight = new AtomicBoolean();

    public TokenUpdater(ConsulTokenProvider tokenProvider) {
        this(tokenProvider, TokenStorageFactory.CreateOptions.DEFAULT_VALIDATION_INTERVAL);
    }

    public TokenUpdater(ConsulTokenProvider tokenProvider, TokenStorageFactory.CreateOptions options) {
        this(tokenProvider, options.getValidationInterval());
    }

    private TokenUpdater(ConsulTokenProvider tokenProvider, Duration validationInterval) {
        this.tokenProvider = tokenProvider;
        this.tries = DEFAULT_TRIES;
        this.retryPause = DEFAULT_RETRY_PAUSE;
        this.validationInterval = validationInterval;
        this.checkDelaySeconds = randomJitter();
    }

    TokenUpdater(ConsulTokenProvider tokenProvider, ScheduledExecutorService executor, Clock clock, int tries,
                 Duration retryPause, Duration validationInterval, LongSupplier checkDelaySeconds) {
        this.tokenProvider = tokenProvider;
        this.executor = executor;
        this.clock = clock;
        this.tries = tries;
        this.retryPause = retryPause;
        this.validationInterval = validationInterval;
        this.checkDelaySeconds = checkDelaySeconds;
    }

    private static LongSupplier randomJitter() {
        return () -> ThreadLocalRandom.current().nextLong(CHECK_JITTER_SECONDS + 1);
    }

    /**
     * Obtains the token and, when it expires, schedules a relogin. An empty {@code currentSecretId} means a login;
     * otherwise the pod already holds a token and only its expiration is read. Only a token with an expiration is
     * scheduled for: Consul omits the field for auth methods without {@code MaxTokenTTL}, and such a token never
     * expires.
     *
     * <p>The scheduled check starts here too, and unlike the relogin it does not depend on the expiration: a token
     * that never expires is the one nothing else would ever replace.
     *
     * @param updater receives every new {@code SecretID}, including the ones from scheduled relogins and from a
     *                refusal
     * @throws RuntimeException when the attempts run out
     */
    synchronized public void watch(Consumer<String> updater, String currentSecretId) {
        log.debug("Start token refreshing process for consul");
        this.updater = updater;
        Token token;
        if (currentSecretId == null || currentSecretId.isEmpty()) {
            token = withRetry(tokenProvider::getToken, tries);
            publish(token);
        } else {
            token = withRetry(() -> tokenProvider.getSelfToken(currentSecretId), tries);
            this.currentSecretId = token.getSecretId();
        }

        if (token.getExpirationTime() != null) {
            scheduleRelogin(updater, token.getExpirationTime());
        }
        scheduleCheck();
    }

    @Override
    public String get() {
        String secretId = currentSecretId;
        return secretId == null ? "" : secretId;
    }

    /**
     * Schedules a check unless one is already under way or the previous one was too recent. The delay is random within
     * {@link #CHECK_JITTER_SECONDS} so that a fleet meeting the same refusal does not reach Consul together.
     */
    @Override
    public void reportRefusal() {
        if (updater == null) {
            log.debug("Ignoring a refusal that arrived before the first login");
            return;
        }
        if (!checkInFlight.compareAndSet(false, true)) {
            log.debug("Ignoring a refusal while a check is already under way");
            return;
        }
        Instant previous = lastCheckAt;
        if (previous != null && clock.instant().isBefore(previous.plus(MIN_CHECK_INTERVAL))) {
            log.debug("Ignoring a refusal less than {} after the previous check", MIN_CHECK_INTERVAL);
            checkInFlight.set(false);
            return;
        }
        long delaySeconds = checkDelaySeconds.getAsLong();
        log.info("Consul refused a request carrying the ACL token of this pod; checking the token in {} seconds",
                delaySeconds);
        executor.schedule(this::checkAndReplace, delaySeconds, TimeUnit.SECONDS);
    }

    /**
     * Reads the token the pod holds and reschedules itself. A zero or negative interval turns the check off, leaving
     * the pod with the relogin schedule and with what its consumers report.
     */
    private void scheduleCheck() {
        if (validationInterval == null || validationInterval.isZero() || validationInterval.isNegative()) {
            return;
        }
        executor.schedule(this::runScheduledCheck, validationInterval.getSeconds(), TimeUnit.SECONDS);
    }

    private void runScheduledCheck() {
        try {
            if (checkInFlight.compareAndSet(false, true)) {
                checkAndReplace();
            }
        } finally {
            scheduleCheck();
        }
    }

    /**
     * Reads the token back from Consul and replaces it when Consul no longer resolves it. Any other answer means
     * Consul is unreachable rather than the token being dead, so the pod keeps the token it holds.
     *
     * <p>The caller owns {@link #checkInFlight} and this method releases it.
     */
    private void checkAndReplace() {
        lastCheckAt = clock.instant();
        try {
            currentSecretId = tokenProvider.getSelfToken(currentSecretId).getSecretId();
        } catch (ConsulResponseException e) {
            if (e.getCode() == REFUSED) {
                replaceRefusedToken();
            } else {
                log.debug("Could not read the consul token back; keeping it until the next check", e);
            }
        } catch (Throwable e) {
            log.debug("Could not read the consul token back; keeping it until the next check", e);
        } finally {
            checkInFlight.set(false);
        }
    }

    /**
     * Logs in and hands the token over, leaving the relogin schedule alone: the task the expiration of the replaced
     * token scheduled is still pending, and starting a second schedule beside it would double every later relogin.
     */
    private void replaceRefusedToken() {
        log.warn("Consul no longer resolves the ACL token of this pod; getting a new one");
        try {
            publish(withRetry(tokenProvider::getToken, tries));
        } catch (Throwable e) {
            log.error("Error occurred during getting a new consul token after consul refused the current one", e);
        }
    }

    private void publish(Token token) {
        currentSecretId = token.getSecretId();
        updater.accept(token.getSecretId());
    }

    /**
     * Schedules one relogin and, from its result, the next one. Every delay is measured against the expiration of the
     * token the pod holds right now rather than of the first one: the way of obtaining the token can change while the
     * pod lives, and the two auth methods carry different {@code MaxTokenTTL}. A failed relogin therefore shortens the
     * next delay instead of repeating the previous one, which would put the retry well past the expiration on a
     * short-lived token. A token without an expiration ends the schedule.
     *
     * <p>Repeated failures raise the lower bound on that delay: it doubles from {@link #MIN_DELAY_SECONDS} to
     * {@link #MAX_RETRY_DELAY_SECONDS}, and a successful relogin returns it to the minimum.
     *
     * <p>The task catches {@link Throwable} rather than {@link Exception}, against the rule of the module that lets an
     * {@link Error} through. Here nobody would see it: the executor keeps it in a {@link java.util.concurrent.Future}
     * no one reads, the task never runs again, and the pod silently keeps a token that eventually expires.
     */
    private void scheduleRelogin(Consumer<String> updater, OffsetDateTime expirationTime) {
        retryDelaySeconds = MIN_DELAY_SECONDS;
        scheduleReloginIn(updater, expirationTime, reloginDelaySeconds(expirationTime));
    }

    private void scheduleReloginIn(Consumer<String> updater, OffsetDateTime expirationTime, long delaySeconds) {
        executor.schedule(() -> {
            log.debug("Get new consul token with {} retry attempts", tries);
            try {
                Token newToken = withRetry(tokenProvider::getToken, tries);
                publish(newToken);
                if (newToken.getExpirationTime() == null) {
                    log.debug("Consul token has no expiration time, stop refreshing");
                    return;
                }
                scheduleRelogin(updater, newToken.getExpirationTime());
            } catch (Throwable e) {
                long nextDelaySeconds = Math.max(reloginDelaySeconds(expirationTime), retryDelaySeconds);
                retryDelaySeconds = Math.min(retryDelaySeconds * 2, MAX_RETRY_DELAY_SECONDS);
                log.error("Error occurred during getting new consul token. Will try in {} seconds.", nextDelaySeconds, e);
                scheduleReloginIn(updater, expirationTime, nextDelaySeconds);
            }
        }, delaySeconds, TimeUnit.SECONDS);
    }

    /**
     * Returns the delay before the next relogin as a share of the remaining lifetime. The same value serves as the
     * period of the schedule, so a constant offset from the expiration would degenerate on short-lived tokens. Past
     * the expiration the share turns negative, and the returned value is {@link #MIN_DELAY_SECONDS}.
     */
    private long reloginDelaySeconds(OffsetDateTime expirationTime) {
        long remaining = ChronoUnit.SECONDS.between(OffsetDateTime.now(clock), expirationTime);
        return Math.max((long) (remaining * DELAY_MULTIPLIER), MIN_DELAY_SECONDS);
    }

    private Token withRetry(CheckedSupplier<Token> c, int tries) {
        try {
            return Failsafe.with(getRetryPolicy(tries)).get(c);
        } catch (FailsafeException e) {
            throw new RuntimeException("can not update consul token: ", e.getCause());
        }
    }

    private RetryPolicy<Token> getRetryPolicy(int tries) {
        return LoginRetryPolicies.<Token>onTransportFailure(tries, retryPause)
                .onFailedAttempt(event -> log.debug("Failed attempt {} to get a consul token",
                        event.getAttemptCount(), event.getLastFailure()));
    }
}
