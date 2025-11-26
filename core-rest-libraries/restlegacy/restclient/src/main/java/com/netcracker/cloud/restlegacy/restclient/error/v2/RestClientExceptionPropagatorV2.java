package com.netcracker.cloud.restlegacy.restclient.error.v2;

import com.netcracker.cloud.restlegacy.restclient.error.RestClientExceptionPropagator;
import org.springframework.web.client.RestClientException;

@ErrorHandlerVersion2Condition
class RestClientExceptionPropagatorV2 implements RestClientExceptionPropagator {

    @Override
    public void propagate(Throwable throwable, String failedUrl) {
        propagateIfPossible(throwable, RestClientException.class);
        throw new RestClientException("Exception while communication with service " + failedUrl, throwable);
    }

    private <X extends Throwable> void propagateIfPossible (Throwable throwable, Class<X> clazz) throws X {
        if (clazz.isInstance(throwable)) {
            throw clazz.cast(throwable);
        }
        if (throwable instanceof RuntimeException runtimeException) {
            throw runtimeException;
        }
        if (throwable instanceof Error error) {
            throw error;
        }
    }
}
