package org.qubership.cloud.quarkus.routesregistration.deployment.resource.hierarchy;

import jakarta.ws.rs.Path;

@Path("/x-icase3")
public interface ICase3 {
    @Path("/icase3/method3")
    void method3();

    @Path("/icase3/method4")
    void method4();

    @Path("/icase3/method4String")
    void method4(String s);

    @Path("/icase3/method4Integer")
    void method4(Integer i);
}
