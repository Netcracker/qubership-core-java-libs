package com.netcracker.cloud.core.error.rest.tmf;

import com.netcracker.cloud.core.error.runtime.ErrorCode;
import com.netcracker.cloud.core.error.runtime.ErrorCodeHolder;

public class ErrorCodes {
    public static final ErrorCode DBAAS_VALIDATION_4001 = new ErrorCodeHolder("DBAAS-4001", "field cannot be empty");
    public static final ErrorCode DBAAS_VALIDATION_4002 = new ErrorCodeHolder("DBAAS-4002", "field is too long");
}
