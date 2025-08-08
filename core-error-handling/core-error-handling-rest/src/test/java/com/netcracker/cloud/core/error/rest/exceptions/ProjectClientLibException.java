package org.qubership.cloud.core.error.rest.exceptions;

import org.qubership.cloud.core.error.runtime.ErrorCodeException;
import org.qubership.cloud.core.error.runtime.ErrorCodeHolder;

public class ProjectClientLibException extends ErrorCodeException {
    public ProjectClientLibException(String message, Throwable cause) {
        super(new ErrorCodeHolder("PROJECT-LIB-5", "simple message"), message, cause);
    }
}
