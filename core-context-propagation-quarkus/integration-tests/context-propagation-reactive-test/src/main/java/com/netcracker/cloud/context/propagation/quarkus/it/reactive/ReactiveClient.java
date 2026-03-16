package com.netcracker.cloud.context.propagation.quarkus.it.reactive;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/headers")
@RegisterRestClient(configKey = "my-client")
public interface ReactiveClient {
    @GET
    Response getHeaders();

    @GET
    @Path("/reactive")
    Uni<Response> getReactiveHeaders();
}
