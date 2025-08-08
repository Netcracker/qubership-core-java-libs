package com.netcracker.cloud.junit.cloudcore.extension.annotations;

import com.netcracker.cloud.junit.cloudcore.extension.callbacks.CloudCoreJunitCallback;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(CloudCoreJunitCallback.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public @interface EnableExtension {
}
