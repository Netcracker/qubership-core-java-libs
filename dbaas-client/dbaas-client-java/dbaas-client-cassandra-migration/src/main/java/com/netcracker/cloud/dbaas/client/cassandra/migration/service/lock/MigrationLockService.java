package com.netcracker.cloud.dbaas.client.cassandra.migration.service.lock;

import com.datastax.oss.driver.api.core.ConsistencyLevel;
import org.qubership.cloud.dbaas.client.cassandra.migration.exception.MigrationLockException;
import org.qubership.cloud.dbaas.client.cassandra.migration.exception.SchemaMigrationException;
import org.qubership.cloud.dbaas.client.cassandra.migration.model.settings.SchemaMigrationSettings;
import org.qubership.cloud.dbaas.client.cassandra.migration.session.SchemaMigrationSession;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.ClockProvider;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.SimpleLock;
import net.javacrumbs.shedlock.provider.cassandra.CassandraLockProvider;

import org.apache.commons.lang3.concurrent.BasicThreadFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.qubership.cloud.dbaas.client.cassandra.migration.SchemaMigrationCommonConstants.MIGRATION_LOG_PREFIX;

@Slf4j
public class MigrationLockService implements AutoCloseable {
    private final static Duration LOCK_AT_LEAST_FOR_DEFAULT_DURATION = Duration.ZERO;

    private final Long lockExtensionPeriod;
    private final Long lockExtensionFailDelayRetry;
    private final Long lockAwaitForUncompletedDelay;

    private final LockConfiguration lockConfiguration;
    private final CassandraLockProvider cassandraLockProvider;
    private final ScheduledExecutorService keepAliveExecutor;

    // Accessed only inside executor thread
    private Instant lockedUntil;
    private ScheduledFuture<?> future;

    private volatile SimpleLock cassandraLock;

    public MigrationLockService(
            SchemaMigrationSession session,
            SchemaMigrationSettings schemaMigrationSettings
    ) {
        this.lockExtensionPeriod = schemaMigrationSettings.lock().extensionPeriod();
        this.lockExtensionFailDelayRetry = schemaMigrationSettings.lock().extensionFailRetryDelay();
        this.lockAwaitForUncompletedDelay = schemaMigrationSettings.lock().retryDelay();
        this.cassandraLockProvider = new CassandraLockProvider(
                CassandraLockProvider.Configuration.builder()
                        .withCqlSession(session.getSession())
                        .withTableName(schemaMigrationSettings.lock().tableName())
                        .withConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM)
                        .build()
        );
        this.lockConfiguration = new LockConfiguration(
                Instant.now(), schemaMigrationSettings.lock().tableName(),
                Duration.ofMillis(schemaMigrationSettings.lock().lockLifetime()), LOCK_AT_LEAST_FOR_DEFAULT_DURATION
        );
        this.keepAliveExecutor = Executors.newSingleThreadScheduledExecutor(new BasicThreadFactory.Builder()
                .namingPattern("CSM-keepalive-%d")
                .build()
        );
    }

    public synchronized void lockOrWaitFor() {
        log.info(MIGRATION_LOG_PREFIX + "Start waiting for schema migration lock.");

        if (cassandraLock != null) {
            log.warn(MIGRATION_LOG_PREFIX + "Migration tries to lock, but migration is already locked.");
            return;
        }

        long t1 = System.currentTimeMillis();
        Optional<SimpleLock> lockTmp;
        try {
            while (
                    (lockTmp = cassandraLockProvider.lock(lockConfiguration)).isEmpty()
            ) {
                log.info(MIGRATION_LOG_PREFIX + "Migration already locked, waiting for schema migration uncompleted lock.");
                Thread.sleep(lockAwaitForUncompletedDelay);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SchemaMigrationException("Interrupted while waiting for schema migration lock.", e);
        }
        log.info(MIGRATION_LOG_PREFIX + "Stop waiting for schema migration lock after {} ms.", System.currentTimeMillis() - t1);

        this.cassandraLock = lockTmp.get();
        this.lockedUntil = calculateLockedUntilFromNow();
        this.future = keepAliveExecutor.scheduleAtFixedRate(
                this::extendForNextPeriod, lockExtensionPeriod,
                lockExtensionPeriod, TimeUnit.MILLISECONDS
        );

        log.info(MIGRATION_LOG_PREFIX + "Schema migration lock acquired until {}", lockedUntil);
    }

    private void extendForNextPeriod() {
        try {
            boolean extendedLock = tryExtendLock();
            if (!extendedLock) {
                tryExtendLockWithRetryDelay();
            }
        } catch (Throwable e) {
            log.error(MIGRATION_LOG_PREFIX + "Attempts to extend lock migration were failed, lock will be released.", e);
            this.cassandraLock = null;
            this.future.cancel(false);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private synchronized boolean tryExtendLock() throws MigrationLockException {
        log.info(MIGRATION_LOG_PREFIX + "Attempt to extend lock {}.", lockConfiguration.getName());

        if (cassandraLock == null) {
            throw new MigrationLockException("Attempt to extend lock failed, nothing to extend.");
        }

        if (ClockProvider.now().plusSeconds(5).isAfter(lockedUntil)) {
            throw new MigrationLockException("Attempt to extend lock failed, locked until time until is expired.");
        }

        try {
            Instant lockedUntilTmp = calculateLockedUntilFromNow();
            Optional<SimpleLock> lock = cassandraLock.extend(lockConfiguration.getLockAtMostFor(), LOCK_AT_LEAST_FOR_DEFAULT_DURATION);
            if (lock.isPresent()) {
                log.info(MIGRATION_LOG_PREFIX + "Attempt to extend lock {} succeeded, extended until {}",
                        lockConfiguration.getName(), lockedUntilTmp
                );
                this.cassandraLock = lock.get();
                this.lockedUntil = lockedUntilTmp;
                return true;
            } else {
                log.warn(MIGRATION_LOG_PREFIX + "Attempt to extend lock {} failed, lock provider returns empty result.",
                        lockConfiguration.getName()
                );
                return false;
            }
        } catch (Exception e) {
            log.warn(
                    MIGRATION_LOG_PREFIX + "Attempt to extend lock {} failed, lock provider throws exception",
                    lockConfiguration.getName(), e
            );
            return false;
        }
    }

    /**
     * return non-null True or throws exception
     */
    private void tryExtendLockWithRetryDelay() throws MigrationLockException, InterruptedException {
        while (!tryExtendLock()) {
            Thread.sleep(lockExtensionFailDelayRetry);
        }
    }

    public boolean isLockActive() {
        return cassandraLock != null;
    }

    private Instant calculateLockedUntilFromNow() {
        return ClockProvider.now().plus(lockConfiguration.getLockAtMostFor());
    }

    public synchronized void unlock() {
        log.info(MIGRATION_LOG_PREFIX + "Start unlocking schema migration lock.");
        if (cassandraLock != null) {
            cassandraLock.unlock();
            cassandraLock = null;
        }
        if (this.future != null) {
            this.future.cancel(false);
        }
        log.info(MIGRATION_LOG_PREFIX + "Stop unlocking schema migration lock.");
    }

    @Override
    public void close() {
        log.info(MIGRATION_LOG_PREFIX + "Start closing MigrationLockService");
        this.keepAliveExecutor.shutdownNow();
        log.info(MIGRATION_LOG_PREFIX + "Stop closing MigrationLockService");
    }
}
