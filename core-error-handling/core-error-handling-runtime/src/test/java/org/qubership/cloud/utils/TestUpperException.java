package org.qubership.cloud.utils;

import org.qubership.cloud.core.error.runtime.ErrorCodeException;
import org.qubership.cloud.core.error.runtime.ErrorCodeHolder;

public class TestUpperException extends ErrorCodeException {
    public static final String CODE = "TEST-UPPER-1000";
    public TestUpperException(Throwable cause) {
        super(new ErrorCodeHolder(CODE, "test %s"), "upper", cause);
    }
}
