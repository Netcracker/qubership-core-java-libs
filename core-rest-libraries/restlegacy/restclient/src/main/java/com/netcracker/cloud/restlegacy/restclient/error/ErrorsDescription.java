package com.netcracker.cloud.restlegacy.restclient.error;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.http.HttpStatus;

import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * @deprecated used only for legacy and legacy client errorHandler
 */
@Deprecated
@ToString
@AllArgsConstructor
public class ErrorsDescription {
    @Getter
    private final UUID errorId;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss a z")
    @Getter
    private final Date date;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Getter
    private final String service;
    @Getter
    private final HttpStatus status;
    @Getter
    private final List<ErrorDescription> errors;
    private final String exception;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Getter
    private final String originalMessage;
    private final String stackTrace;
    @Getter
    @Setter
    private String errorMessage;
    @Setter
    private boolean proxy;

    public boolean isProxy() {
        return proxy;
    }

    public ErrorsDescription(UUID errorId, Date date, String service, HttpStatus status, List<ErrorDescription> errors, String exception, String originalMessage, String stackTrace) {
        this.errorId = errorId;
        this.date = date;
        this.service = service;
        this.status = status;
        this.errors = errors;
        this.exception = exception;
        this.originalMessage = originalMessage;
        this.stackTrace = stackTrace;
        this.proxy = false;
    }

    public ErrorsDescription(Date date, String service, HttpStatus status, List<ErrorDescription> errors, String exception, String originalMessage, String stackTrace) {
        this(UUID.randomUUID(), date, service, status, errors, exception, originalMessage, stackTrace);
    }

    public ErrorsDescription(Date date, String service, HttpStatus status, List<ErrorDescription> errors, String exception, String originalMessage, String stackTrace, String errorMessage) {
        this(date, service, status, errors, exception, originalMessage, stackTrace);
        this.errorMessage = errorMessage;
    }

    /**
     * @deprecated used only for legacy {com.netcracker.cloud.microserviceframework.controller.ControllersAdvice} and legacy client errorHandler
     */
    @Deprecated
    @ToString
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ErrorDescription {
        @Getter
        private final String errorCode;
        @Getter
        private final String errorMessage;
        @Getter
        private String fieldName;
        @Getter
        private String[] parameters;

        public ErrorDescription(String errorCode, String errorMessage) {
            this.errorCode = errorCode;
            this.errorMessage = errorMessage;
        }

        public ErrorDescription(String errorCode, String errorMessage, String fieldName) {
            this(errorCode, errorMessage);
            this.fieldName = fieldName;
            this.parameters = ArrayUtils.EMPTY_STRING_ARRAY;
        }

        public ErrorDescription(String errorCode, String errorMessage, String[] parameters) {
            this(errorCode, errorMessage);
            this.parameters = (parameters == null) ? ArrayUtils.EMPTY_STRING_ARRAY : parameters;
        }
    }
}
