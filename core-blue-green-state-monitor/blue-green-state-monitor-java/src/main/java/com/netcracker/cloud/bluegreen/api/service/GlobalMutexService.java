package com.netcracker.cloud.bluegreen.api.service;

import com.netcracker.cloud.bluegreen.api.error.LockFailedException;
import com.netcracker.cloud.bluegreen.api.model.MicroserviceLockInfo;

import java.time.Duration;
import java.util.List;

/**
 * <p>
 * Global lock is stored for each BlueGreen namespace in Consul Key/Value storage at path:
 * 'config/{namespace}/application/bluegreen/global-mutex'
 * </p>
 * <p>
 * Active Global lock prohibits creation of any microservice locks. For details about microservice locks see {@link MicroserviceMutexService}
 * </p>
 * <p>
 * Global lock will fail to be set if there is at least 1 microservice lock in any of BlueGreen namespaces.
 * </p>
 * <p>
 * <b>Warning! Global lock must be used only by BG Operator</b>
 * </p>
 */
@Deprecated(forRemoval = true)
public interface GlobalMutexService {

    /**
     * <p>
     * Try to acquire Global lock within specified timeout.
     * </p>
     * <p>
     * Global lock will be acquired if there are no active Microservice locks.
     * </p>
     * <p>
     * Global lock is a reentrant lock. If there is already active Global lock, subsequent tryLock invocations will return true
     * </p>
     *
     * @param namespaces List of BlueGreen namespaces
     * @param timeout    duration to try to acquire Global lock. Cannot be negative
     * @return true if lock was created, false otherwise
     * @throws LockFailedException in case unexpected exception happened during communication with Consul
     */
    boolean tryLock(Duration timeout, List<String> namespaces) throws LockFailedException;

    /**
     * <p>
     * Force acquire Global lock even if there are active microservice locks.
     * </p>
     * <p>
     * Global lock is a reentrant lock. If there is already active Global lock, subsequent forceLock invocations will return true
     * </p>
     *
     * @param namespaces List of BlueGreen namespaces
     * @return true if lock was created, false otherwise
     * @throws LockFailedException in case unexpected exception happened during communication with Consul
     */
    boolean forceLock(List<String> namespaces) throws LockFailedException;

    /**
     * Remove Global lock if set. Operation is idempotent.
     *
     * @param namespaces List of BlueGreen namespaces
     * @throws LockFailedException in case unexpected exception happened during communication with Consul
     */
    void unlock(List<String> namespaces) throws LockFailedException;

    /**
     * @param namespaces List of BlueGreen namespaces
     * @return true if there is active Global lock, false otherwise.
     * @throws LockFailedException in case unexpected exception happened during communication with Consul
     */
    boolean isLocked(List<String> namespaces) throws LockFailedException;

    /**
     * @param namespaces List of BlueGreen namespaces
     * @return list of all active Microservice locks in each BlueGreen namespace.
     * @throws LockFailedException in case unexpected exception happened during communication with Consul
     */
    List<MicroserviceLockInfo> getMicroserviceLocks(List<String> namespaces) throws LockFailedException;

}
