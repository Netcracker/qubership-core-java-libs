package com.netcracker.cloud.restlegacy.restclient.error.v2.v2_1;

import lombok.Getter;

/**
 * System will show to user message generated with the help of MessageCode and NamedMessageParameter.
 * MessageCode will be used to get placeholder, and NamedMessageParameter will be used as args for the placeholder.
 */
@Getter
public class GenericDisplayedException extends RuntimeException {

    private final String messageCode;

    private final NamedMessageParameter[] namedMessageParameters;

    public GenericDisplayedException(final String messageCode, final NamedMessageParameter... namedMessageParameters) {
        this.messageCode = messageCode;
        this.namedMessageParameters = namedMessageParameters;
    }

    public GenericDisplayedException(final String messageCode, final Throwable cause, final NamedMessageParameter... namedMessageParameters) {
        super(cause);
        this.messageCode = messageCode;
        this.namedMessageParameters = namedMessageParameters;
    }

}
