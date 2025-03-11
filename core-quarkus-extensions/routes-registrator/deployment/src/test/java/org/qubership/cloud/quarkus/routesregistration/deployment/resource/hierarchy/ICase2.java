package org.qubership.cloud.quarkus.routesregistration.deployment.resource.hierarchy;

import jakarta.ws.rs.Path;

@Path("/x-icase2")
public interface ICase2 extends ICase3 {
    @Path("/icase2/method1")
    void method1();

    @Path("/icase2/method2")
    void method2();

    @Path("/icase2/method3")
    void method3();

    @Path("/icase2/method5")
    void method5();
}
