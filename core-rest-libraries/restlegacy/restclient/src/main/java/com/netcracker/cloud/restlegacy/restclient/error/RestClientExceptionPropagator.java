package com.netcracker.cloud.restlegacy.restclient.error;

public interface RestClientExceptionPropagator {
    void propagate(Throwable throwable, String failedUrl);
}
