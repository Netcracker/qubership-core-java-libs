package com.netcracker.cloud.bluegreen.impl.service;

import com.netcracker.cloud.bluegreen.api.error.LockFailedException;
import com.netcracker.cloud.bluegreen.api.model.MicroserviceLockInfo;
import com.netcracker.cloud.bluegreen.api.service.GlobalMutexService;

import java.time.Duration;
import java.util.List;

@Deprecated(forRemoval = true)
public class InMemoryGlobalMutexService implements GlobalMutexService {

    @Override
    public boolean tryLock(Duration timeout, List<String> namespaces) throws LockFailedException {
        throw unsupported();
    }

    @Override
    public boolean forceLock(List<String> namespaces) throws LockFailedException {
        throw unsupported();
    }

    @Override
    public void unlock(List<String> namespaces) throws LockFailedException {
        throw unsupported();
    }

    @Override
    public boolean isLocked(List<String> namespaces) throws LockFailedException {
        throw unsupported();
    }

    @Override
    public List<MicroserviceLockInfo> getMicroserviceLocks(List<String> namespaces) throws LockFailedException {
        throw unsupported();
    }

    private UnsupportedOperationException unsupported() {
        return new UnsupportedOperationException("not implemented yet");
    }
}
