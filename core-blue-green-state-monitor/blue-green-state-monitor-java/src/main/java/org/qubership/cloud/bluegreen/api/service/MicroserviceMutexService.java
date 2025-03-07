package org.qubership.cloud.bluegreen.api.service;

import org.qubership.cloud.bluegreen.api.error.LockFailedException;

import java.time.Duration;

/**
 * <p>
 * Microservice lock is stored for particular BlueGreen namespace in Consul Key/Value storage at path:
 * 'config/{namespace}/{microservice}/bluegreen/mutex'
 * </p>
 * <p>
 * Active Global lock prohibits creation of Microservice lock. For details about Global lock see {@link GlobalMutexService}
 * </p>
 * <p>
 * This service works with particular namespace and microservice. See implementation for details how they are provided.
 * </p>
 */
@Deprecated(forRemoval = true)
public interface MicroserviceMutexService {

    /**
     * Try to acquire Microservice lock within specified timeout with particular reason.
     * <p>
     * Microservice lock will be acquired if there is no Global lock.
     * </p>
     * <p>
     * Microservice lock is a reentrant lock. If there is already active Microservice lock, subsequent tryLock invocations will return true.
     * </p>
     * <p>
     * Successful Microservice lock creation is followed by creation of daemon thread which is responsible to renew the Consul session bound to the just created lock.
     * Microservice lock will be auto-released in case this thread dies with JVM or fails to renew consul session within specified TTL
     * </p>
     *
     * @param timeout duration to try to acquire Microservice lock. Cannot be negative
     * @param reason human-readable description that contains information why the lock was set
     * @return true if lock was created, false otherwise
     * @throws LockFailedException in case unexpected exception happened during communication with Consul
     */
    boolean tryLock(Duration timeout, String name, String reason) throws LockFailedException;

    /**
     * Remove Microservice lock if set. Operation is idempotent.
     *
     * @throws LockFailedException in case unexpected exception happened during communication with Consul
     */
    void unlock(String name) throws LockFailedException;

    /**
     * @return true if there is active Microservice lock, false otherwise.
     * @throws LockFailedException in case unexpected exception happened during communication with Consul
     */
    boolean isLocked(String name) throws LockFailedException;

}
