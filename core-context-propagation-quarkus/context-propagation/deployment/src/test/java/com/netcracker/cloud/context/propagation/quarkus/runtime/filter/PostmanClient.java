package com.netcracker.cloud.context.propagation.quarkus.runtime.filter;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/headers")
@RegisterRestClient(configKey = "my-client")
public interface PostmanClient {
    @GET
    Response getHeaders();
}
