package org.qubership.cloud.junit.cloudcore.extension.callbacks.classes;

import org.qubership.cloud.junit.cloudcore.extension.annotations.PortForward;
import org.qubership.cloud.junit.cloudcore.extension.annotations.Value;

import java.net.URI;

public class TestClassLev2 extends TestClassLev1 {
    @PortForward(serviceName = @Value(value = "service-level-2"))
    URI uriLev2;

    public TestClassLev2(URI uriLev1, URI uriLev2) {
        super(uriLev1);
        this.uriLev2 = uriLev2;
    }

    public static class TestInnerClass {
        @PortForward(serviceName = @Value(value = "service-level-2-inner"))
        URI uri;

        public TestInnerClass(URI uri) {
            this.uri = uri;
        }
    }
}
