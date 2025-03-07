package org.qubership.cloud.restlegacy.restclient.error;

public interface RestClientExceptionPropagator {
    void propagate(Exception exception, String failedUrl);
}
