package com.netcracker.cloud.context.propagation.core;

import java.lang.annotation.*;

/**
 *  The annotation allows registering context providers in contextManager automatically.
 *  The annotation must be on context provider.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RegisterProvider {
}
