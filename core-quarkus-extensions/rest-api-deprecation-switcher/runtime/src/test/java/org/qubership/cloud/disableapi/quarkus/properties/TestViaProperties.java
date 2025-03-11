package org.qubership.cloud.disableapi.quarkus.properties;

import org.qubership.cloud.disableapi.quarkus.AbstractTest;
import org.qubership.cloud.disableapi.quarkus.properties.profiles.PropertiesProfile;
import io.quarkus.arc.profile.IfBuildProfile;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.Method;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.MediaType.TEXT_HTML;

@QuarkusTest
@TestProfile(PropertiesProfile.class)
class TestViaProperties extends AbstractTest {

    @Test
    void v1Test() {
        test("/api/v1/test", Method.GET, 404, CoreMatchers.containsString(MANDATORY_RESPONSE_STRING));
        test("/api/v1/test", Method.POST, 200, CoreMatchers.containsString("ok"));
        test("/api/v1/test/inner", Method.GET, 404, CoreMatchers.containsString(MANDATORY_RESPONSE_STRING));
        test("/api/v1/test/inner/wildcard/param1", Method.GET, 404, CoreMatchers.containsString(MANDATORY_RESPONSE_STRING));
        test("/api/v1/test/inner/extension/example.html", Method.GET, 404, CoreMatchers.containsString(MANDATORY_RESPONSE_STRING));
    }

    @Test
    void v2Test() {
        test("/api/v2/test", Method.GET, 404, CoreMatchers.containsString(MANDATORY_RESPONSE_STRING));
        test("/api/v2/test", Method.POST, 404, CoreMatchers.containsString(MANDATORY_RESPONSE_STRING));
        test("/api/v2/test/inner", Method.GET, 404, CoreMatchers.containsString(MANDATORY_RESPONSE_STRING));
        test("/api/v2/test/inner/wildcard/param1", Method.GET, 404, CoreMatchers.containsString(MANDATORY_RESPONSE_STRING));
        test("/api/v2/test/inner/extension/example.html", Method.GET, 404, CoreMatchers.containsString(MANDATORY_RESPONSE_STRING));
    }

    @Test
    void v3Test() {
        test("/api/v3/test", Method.GET, 200, CoreMatchers.containsString("ok"));
        test("/api/v3/test", Method.POST, 200, CoreMatchers.containsString("ok"));
        test("/api/v3/test/inner", Method.GET, 200, CoreMatchers.containsString("ok"));
        test("/api/v3/test/inner/wildcard/param1", Method.GET, 200, CoreMatchers.containsString("ok"));
        test("/api/v3/test/inner/extension/example.html", Method.GET, 200, CoreMatchers.containsString("ok"));
    }

    @Path("/api/v1/test")
    @IfBuildProfile("properties-profile")
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
    @IfBuildProfile("properties-profile")
    public static class ControllerV2 {

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

    @Path("/api/v3/test")
    @IfBuildProfile("properties-profile")
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
