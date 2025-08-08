package org.qubership.cloud.core.error.rest.exceptions;

import org.qubership.cloud.core.error.runtime.ErrorCodeException;
import org.qubership.cloud.core.error.runtime.ErrorCodeHolder;

public class DbaasClientLibException extends ErrorCodeException {
    public DbaasClientLibException(String message, Throwable cause) {
        super(new ErrorCodeHolder("DBAAS-CLIENT-LIB-4", "simple message"), message, cause);
    }
}
