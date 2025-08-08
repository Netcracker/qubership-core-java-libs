package org.qubership.cloud.dbaas.client.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.listener.RetryListenerSupport;

@Slf4j
public class DefaultRetryListener extends RetryListenerSupport {

    @Override
    public <T, E extends Throwable> void onError(RetryContext context,
                                                 RetryCallback<T, E> callback, Throwable throwable) {
        log.warn("Error is thrown during retry operation, retry attempt {}, got {}: {}",
                context.getRetryCount(), throwable.getClass(), throwable.getMessage());
    }

}
