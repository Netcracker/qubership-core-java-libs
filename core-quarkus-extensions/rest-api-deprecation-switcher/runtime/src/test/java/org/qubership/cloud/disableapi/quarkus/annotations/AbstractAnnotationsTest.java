package org.qubership.cloud.disableapi.quarkus.annotations;

import org.qubership.cloud.disableapi.quarkus.AbstractTest;
import io.quarkus.arc.profile.UnlessBuildProfile;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.MediaType.TEXT_HTML;

public abstract class AbstractAnnotationsTest extends AbstractTest {

    @Path("/api/v1/test")
    @Deprecated
    @UnlessBuildProfile("properties-profile")
    public static class ControllerV1 {

        @GET
        @Produces(APPLICATION_JSON)
        public Response apiGet() {
            return Response.ok("ok").build();
        }

        @POST
        @Produces(APPLICATION_JSON)
        public Response apiPost() {
            return Response.ok("ok").build();
        }

        @GET
        @Path("/inner")
        @Produces(APPLICATION_JSON)
        public Response apiInnerGet() {
            return Response.ok("ok").build();
        }

        @GET
        @Path("/inner/wildcard/{param}")
        @Produces(APPLICATION_JSON)
        public Response apiInnerWithParamGet(@PathParam("param") String param) {
            return Response.ok("ok").build();
        }

        @GET
        @Path("/inner/extension/{name}.html")
        @Produces(TEXT_HTML)
        public Response apiInnerWithExtensionGet(@PathParam("name") String name) {
            return Response.ok("ok").build();
        }
    }

    @Path("/api/v2/test")
    @UnlessBuildProfile("properties-profile")
    public static class ControllerV2 {

        @GET
        @Deprecated
        public Response apiGet() {
            return Response.ok("ok").build();
        }

        @POST
        @Deprecated
        public Response apiPost() {
            return Response.ok("ok").build();
        }

        @GET
        @Path("/inner")
        public Response apiInnerGet() {
            return Response.ok("ok").build();
        }

        @GET
        @Path("/inner/wildcard/{param}")
        @Produces(APPLICATION_JSON)
        public Response apiInnerWithParamGet(@PathParam("param") String param) {
            return Response.ok("ok").build();
        }

        @GET
        @Path("/inner/extension/{name}.html")
        @Produces(TEXT_HTML)
        public Response apiInnerWithExtensionGet(@PathParam("name") String name) {
            return Response.ok("ok").build();
        }
    }

    @Path("/api/v3/test")
    @UnlessBuildProfile("properties-profile")
    public static class ControllerV3 {

        @GET
        public Response apiGet() {
            return Response.ok("ok").build();
        }

        @POST
        public Response apiPost() {
            return Response.ok("ok").build();
        }

        @GET
        @Path("/inner")
        public Response apiInnerGet() {
            return Response.ok("ok").build();
        }

        @GET
        @Path("/inner/wildcard/{param}")
        @Produces(APPLICATION_JSON)
        public Response apiInnerWithParamGet(@PathParam("param") String param) {
            return Response.ok("ok").build();
        }

        @GET
        @Path("/inner/extension/{name}.html")
        @Produces(TEXT_HTML)
        public Response apiInnerWithExtensionGet(@PathParam("name") String name) {
            return Response.ok("ok").build();
        }
    }
}
