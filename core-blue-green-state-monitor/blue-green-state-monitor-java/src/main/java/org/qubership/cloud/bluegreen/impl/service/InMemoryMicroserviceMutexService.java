package org.qubership.cloud.bluegreen.impl.service;

import org.qubership.cloud.bluegreen.api.error.LockFailedException;
import org.qubership.cloud.bluegreen.api.service.MicroserviceMutexService;

import java.time.Duration;

@Deprecated(forRemoval = true)
public class InMemoryMicroserviceMutexService implements MicroserviceMutexService {

    @Override
    public boolean tryLock(Duration timeout, String name, String reason) throws LockFailedException {
        throw unsupported();
    }

    @Override
    public void unlock(String name) throws LockFailedException {
        throw unsupported();
    }

    @Override
    public boolean isLocked(String name) throws LockFailedException {
        throw unsupported();
    }

    private UnsupportedOperationException unsupported() {
        return new UnsupportedOperationException("not implemented yet");
    }
}
