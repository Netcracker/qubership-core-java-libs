package org.qubership.cloud.consul.config.source.deployment;

public class SpringCloudWithConsulException extends IllegalStateException {

    public SpringCloudWithConsulException() {
        super("Cannot use Consul extension with Spring Cloud extension, use strictly one of following extensions:" +
                    "`springcloud-config-source`, `consul-config-source`");
    }
}
