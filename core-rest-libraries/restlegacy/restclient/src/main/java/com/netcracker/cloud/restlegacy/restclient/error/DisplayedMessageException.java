package com.netcracker.cloud.restlegacy.restclient.error;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * System will show message to User from {@link Throwable#getLocalizedMessage()} if this exception is marked by this annotation.
 * You can use {@link GenericDisplayedException} or create your custom exception class.
 * Only for error model v1 or v2. Not for v2.1
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface DisplayedMessageException {
}
