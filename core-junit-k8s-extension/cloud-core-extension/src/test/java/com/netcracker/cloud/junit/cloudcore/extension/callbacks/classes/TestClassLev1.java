package com.netcracker.cloud.junit.cloudcore.extension.callbacks.classes;

import org.qubership.cloud.junit.cloudcore.extension.annotations.PortForward;
import org.qubership.cloud.junit.cloudcore.extension.annotations.Value;

import java.net.URI;

public class TestClassLev1 {
    @PortForward(serviceName = @Value(value = "service-level-1"))
    URI uriLev1;

    public TestClassLev1(URI uriLev1) {
        this.uriLev1 = uriLev1;
    }

}
