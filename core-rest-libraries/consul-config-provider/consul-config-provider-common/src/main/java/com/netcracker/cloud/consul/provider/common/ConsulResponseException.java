package com.netcracker.cloud.consul.provider.common;

import java.io.IOException;

/**
 * A non-2xx answer from Consul, carrying the response code the caller has to classify. {@code 403} from reading a
 * token means Consul no longer resolves it, which no retry changes; any other code may be a transient failure.
 *
 * <p>Only reads of a token the pod already holds throw this. A login answers {@code 403} for an unrelated reason —
 * the auth method refused the bearer — and reporting it in the same type would let a refused login force the next
 * relogin.
 */
public class ConsulResponseException extends IOException {

    private final int code;

    public ConsulResponseException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
