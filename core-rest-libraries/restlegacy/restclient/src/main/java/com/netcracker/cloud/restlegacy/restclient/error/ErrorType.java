package com.netcracker.cloud.restlegacy.restclient.error;


import lombok.ToString;
import org.springframework.http.HttpStatus;

import static com.netcracker.cloud.restlegacy.restclient.error.ErrorMessageCodes.*;

/**
 * @deprecated See javadoc on {@link ErrorException}
 */
@Deprecated
@ToString
public class ErrorType {
    public static final ErrorType INTERNAL_SERVER_ERROR = new ErrorType(HttpStatus.INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR_CODE);
    public static final ErrorType BAD_REQUEST = new ErrorType(HttpStatus.BAD_REQUEST, "error.bad_request");
    public static final ErrorType NOT_FOUND = new ErrorType(HttpStatus.NOT_FOUND, ENTITY_NOT_FOUND_ERROR_CODE);
    public static final ErrorType UNPROCESSABLE_ENTITY_COMMON = new ErrorType(HttpStatus.UNPROCESSABLE_ENTITY, "error.unprocessable_entity_common");
    public static final ErrorType UNPROCESSABLE_ENTITY_VALIDATION_FAILED = new ErrorType(HttpStatus.UNPROCESSABLE_ENTITY, "error.unprocessable_entity_validation_failed");
    public static final ErrorType CONFLICT = new ErrorType(HttpStatus.CONFLICT, DB_IN_CONFLICT_ERROR_CODE);

    private static final String MESSAGE_POSTFIX = ".code";

    private HttpStatus statusCode;
    private String errorCode;
    private String messageCode;

    public String getErrorCode() {
        return errorCode;
    }

    public HttpStatus getStatusCode() {
        return statusCode;
    }

    public String getMessageCode() {
        return messageCode;
    }

    protected ErrorType(HttpStatus statusCode, String messageCode) {
        this.statusCode = statusCode;
        this.messageCode = messageCode;
        this.errorCode = messageCode + MESSAGE_POSTFIX;
    }
}


