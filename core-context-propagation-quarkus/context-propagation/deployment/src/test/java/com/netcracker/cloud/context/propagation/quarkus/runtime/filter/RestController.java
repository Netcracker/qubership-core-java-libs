package com.netcracker.cloud.context.propagation.quarkus.runtime.filter;

import org.qubership.cloud.headerstracking.filters.context.AcceptLanguageContext;
import org.qubership.cloud.headerstracking.filters.context.RequestIdContext;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.HashMap;
import java.util.Map;

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class RestController {
    public static final String TEST_ENDPOINT = "/test";

    @GET
    @Path(TEST_ENDPOINT)
    public Response testEndpoint(@Context HttpHeaders headers) {
        String xRequestId = RequestIdContext.get();
        String acceptLanguage = AcceptLanguageContext.get();
        Map<String, String> map = new HashMap<>();
        map.put("X-Request-Id", xRequestId);
        map.put("Accept-Language", acceptLanguage);
        return Response.ok(map).build();
    }

    @GET
    @Path("/headers")
    public Response getHeaders(@Context HttpHeaders headers) {
        return Response.ok(headers.getRequestHeaders()).build();
    }
}
