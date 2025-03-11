package org.qubership.cloud.quarkus.routesregistration.deployment.resource.hierarchy;

import jakarta.ws.rs.Path;

@Path("/x-icase1")
public interface ICase1 {
    @Path("/icase1/method1")
    void method1();

    @Path("/icase1/method2")
    void method2();
}
