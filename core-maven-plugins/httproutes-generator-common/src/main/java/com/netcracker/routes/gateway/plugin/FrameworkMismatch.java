package com.netcracker.routes.gateway.plugin;

public final class FrameworkMismatch {

    public static final String QUARKUS_PLUGIN_ARTIFACT = "httproutes-generator-maven-plugin";
    public static final String SPRING_PLUGIN_ARTIFACT = "httproutes-generator-spring-maven-plugin";

    public static final String QUARKUS_NOT_FOUND_MESSAGE =
            "Quarkus was not found. Please use " + SPRING_PLUGIN_ARTIFACT + " for Spring service.";

    public static final String SPRING_NOT_FOUND_MESSAGE =
            "Spring was not found. Please use " + QUARKUS_PLUGIN_ARTIFACT + " for Quarkus service.";

    public static final String SPRING_REST_CONTROLLER = "org.springframework.web.bind.annotation.RestController";
    public static final String SPRING_CONTROLLER = "org.springframework.stereotype.Controller";
    public static final String JAKARTA_WS_RS_PATH = "jakarta.ws.rs.Path";
    public static final String JAVAX_WS_RS_PATH = "javax.ws.rs.Path";

    private FrameworkMismatch() {
    }
}