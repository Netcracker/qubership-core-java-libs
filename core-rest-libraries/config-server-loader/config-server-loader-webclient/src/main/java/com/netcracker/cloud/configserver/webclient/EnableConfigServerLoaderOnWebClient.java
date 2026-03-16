package com.netcracker.cloud.configserver.webclient;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @deprecated
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Deprecated(since="4.x") // This annotation will be deleted in 4.x release. We moved on a new initialization process, so you may just delete it.
public @interface EnableConfigServerLoaderOnWebClient {
}
