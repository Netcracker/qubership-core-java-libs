package com.netcracker.routes.gateway.plugin.inheritance.quarkusapi;

import com.netcracker.cloud.routesregistration.common.annotation.Route;
import com.netcracker.cloud.routesregistration.common.gateway.route.RouteType;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

/**
 * Resources that inherit {@code @Path} and {@code @GET}, and don't inherit {@code @Route}, like in the Quarkus extension.
 */
public final class QuarkusApiResources {

    private QuarkusApiResources() {
    }

    @Path("/api/v1/res")
    public interface ResourceApi {
        @GET
        @Path("/items")
        void items();
    }

    public static class Resource implements ResourceApi {
        @Override
        @Route(RouteType.PUBLIC)
        public void items() {
        }
    }

    @Path("/api/v1/base")
    @Route(RouteType.PUBLIC)
    public abstract static class BaseResource {
        @GET
        @Path("/a")
        @Route
        public void a() {
        }
    }

    @Path("/api/v1/sub")
    public static class SubResource extends BaseResource {
        @GET
        @Path("/b")
        public void b() {
        }
    }
}
