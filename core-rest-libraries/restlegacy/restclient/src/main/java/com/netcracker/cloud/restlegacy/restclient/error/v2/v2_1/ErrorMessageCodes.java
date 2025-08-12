package com.netcracker.cloud.restlegacy.restclient.error.v2.v2_1;

public final class ErrorMessageCodes {
    public static final String INTERNAL_SERVER_ERROR_CODE = "Internal server error";
    public static final String ENTITY_NOT_FOUND_ID_PARAM = "id";
    public static final String ENTITY_NOT_FOUND_TYPE_PARAM = "type";
    public static final String ENTITY_NOT_FOUND_ERROR_CODE = "Entity with id {{" + ENTITY_NOT_FOUND_ID_PARAM + "}} and type {{" + ENTITY_NOT_FOUND_TYPE_PARAM + "}} is not found";
    public static final String ACCESS_DENIED_ERROR_CODE = "No rights to execute operation";
    public static final String OPTIMISTIC_LOCKING_ERROR_CODE = "Lock on object while executing operation";
    public static final String OPERATION_VALIDATION_ERROR_CODE = "Operation cannot be performed because of validation error";
}
