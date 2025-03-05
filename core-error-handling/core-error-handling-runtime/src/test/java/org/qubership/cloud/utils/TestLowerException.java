package org.qubership.cloud.utils;

import org.qubership.cloud.core.error.runtime.ErrorCodeException;
import org.qubership.cloud.core.error.runtime.ErrorCodeHolder;

public class TestLowerException extends ErrorCodeException {
    public static final String CODE = "TEST-LOWER-1000";
    public TestLowerException() {
        super(new ErrorCodeHolder(CODE, "test %s"), "lower");
    }
}
