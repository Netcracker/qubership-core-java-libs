package com.netcracker.cloud.utils;

import com.netcracker.cloud.core.error.runtime.ErrorCodeException;
import com.netcracker.cloud.core.error.runtime.ErrorCodeHolder;

public class TestUpperException extends ErrorCodeException {
    public static final String CODE = "TEST-UPPER-1000";
    public TestUpperException(Throwable cause) {
        super(new ErrorCodeHolder(CODE, "test %s"), "upper", cause);
    }
}
