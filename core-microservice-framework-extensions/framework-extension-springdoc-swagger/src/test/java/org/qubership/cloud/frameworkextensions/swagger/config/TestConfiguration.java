package org.qubership.cloud.frameworkextensions.swagger.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;


public class TestConfiguration {

    public static final String TEST_SCHEME = "testScheme";

    @Bean
    OpenAPI testOpenApi() {
        return new OpenAPI().info(new Info().title("Test API").version("2.0.0-SNAPSHOT")
                .description("Test API documentation."));
    }
}
