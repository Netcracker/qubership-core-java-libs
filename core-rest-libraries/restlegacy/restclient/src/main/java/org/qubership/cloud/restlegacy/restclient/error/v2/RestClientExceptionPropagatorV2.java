package org.qubership.cloud.restlegacy.restclient.error.v2;

import org.qubership.cloud.restlegacy.restclient.error.RestClientExceptionPropagator;
import org.springframework.web.client.RestClientException;

@ErrorHandlerVersion2Condition
class RestClientExceptionPropagatorV2 implements RestClientExceptionPropagator {

    @Override
    public void propagate(Exception exception, String failedUrl) {
        propagateIfPossible(exception, RestClientException.class);
        throw new RestClientException("Exception while communication with service " + failedUrl, exception);
    }

    private <X extends Throwable> void propagateIfPossible (Throwable throwable, Class<X> clazz) throws X {
        if (clazz.isInstance(throwable)) {
            throw clazz.cast(throwable);
        }
        if (throwable instanceof RuntimeException) {
            throw (RuntimeException) throwable;
        }
        if (throwable instanceof Error) {
            throw (Error) throwable;
        }
    }
}
