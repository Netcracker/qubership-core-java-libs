package com.netcracker.cloud.restclient.exception;

import org.qubership.cloud.core.error.runtime.ErrorCodeException;
import org.qubership.cloud.core.error.runtime.ErrorCodeHolder;

public class MicroserviceRestClientException extends ErrorCodeException {
    public MicroserviceRestClientException(String msg) {
        this(msg, null);
    }

    public MicroserviceRestClientException(String msg, Throwable cause) {
        super(new ErrorCodeHolder("CORE-LIB-MRA-0001", "Exception occurred performing REST call"), msg, cause);
    }
}
