package org.qubership.cloud.restlegacy.restclient.error;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * @deprecated use {@link GenericDisplayedException}  or custom exception annotated by {@link DisplayedMessageException} in order to show message to User.
 * If you don not want to show message throw any standard java or custom exception not annotated by {@link DisplayedMessageException}.
 * These exceptions will shown to User with generic message, something like 'System cannot perform operation...'
 */
@Deprecated
@EqualsAndHashCode(callSuper = false)
@ToString
public class ErrorException extends Exception {

    @Getter
    private final ErrorType errorType;
    @Getter
    private final Object[] vars;

    public ErrorException(Throwable cause, ErrorType errorType, Object... vars) {
        super(cause);
        this.errorType = errorType;
        this.vars = vars;
    }
}
