package com.netcracker.cloud.restlegacy.restclient.error.v2;


import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;

import java.lang.annotation.*;

import static com.netcracker.cloud.restlegacy.restclient.error.v2.Constants.*;

/**
 * This custom annotation ErrorHandlerVersion2Condition created to encapsulate {@link ConditionalOnExpression} annotation
 * with two values error.handler.version == v2 OR error.handler.version == v2.1
 * <p>
 * This annotation should be used on classes for versions of error model v2 or v2.1 :
 * <ul>
 * <li>      {@link DebugInfoFilter}</li>
 * <li>      {@link ExceptionHandlingV2MainConfiguration}</li>
 * <li>      {@link RestClientExceptionPropagatorV2}</li>
 * </ul>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Documented
@ConditionalOnExpression("'${" + ERROR_HANDLER_VERSION_CONDITION_PROPERTY + "}'=='" + VERSION_2 + "'" +
        " || '${" + ERROR_HANDLER_VERSION_CONDITION_PROPERTY + "}'=='" + VERSION_2_1 + "'")
@interface ErrorHandlerVersion2Condition {
}
