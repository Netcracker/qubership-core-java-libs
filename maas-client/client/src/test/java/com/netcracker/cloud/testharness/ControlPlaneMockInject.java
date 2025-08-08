package com.netcracker.cloud.testharness;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ControlPlaneMockInject {
}
