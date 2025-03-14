package org.qubership.cloud.frameworkextensions.swagger.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.HashMap;
import java.util.Map;

import static org.qubership.cloud.frameworkextensions.swagger.config.SwaggerAuthConfiguration.*;
import static org.qubership.cloud.frameworkextensions.swagger.config.SwaggerSecurityConfigurator.SPRINGDOC_SWAGGER_UI_OAUTH_CLIENT_ID;
import static org.qubership.cloud.frameworkextensions.swagger.config.SwaggerSecurityConfigurator.SPRINGDOC_SWAGGER_UI_OAUTH_CLIENT_SECRET;
import static org.qubership.cloud.frameworkextensions.swagger.config.TestConfiguration.TEST_SCHEME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {SwaggerAuthConfiguration.class, TestConfiguration.class})
@SpringBootTest(properties = {
        "CLOUD_NAMESPACE=test-namespace",
        "CLOUD_PUBLIC_HOST=test.qubership.org"
})
class SwaggerSecurityConfiguratorTest {
    public static final String IDP_TOKEN_URL = "/api/v1/identity-provider/auth/realms/cloud-common/protocol/openid-connect/token";
    private static final String CLIENT_ID = "testuser";
    private static final String CLIENT_SECRET = "testpassword";

    @Autowired
    SwaggerSecurityConfigurator configurator;
    @Autowired
    private ConfigurableEnvironment environment;
    @Autowired
    private String m2mTokenUrl;
    @Autowired
    private String tokenUrl;
    @Autowired
    private Map<String, SecurityScheme> commonSecuritySchemesMap;
    @Autowired
    SwaggerAuthConfiguration.SecurityCustomiser securityCustomiser;
    @Autowired
    OpenAPI testOpenApi;


    @Test
    void testSecurityConfigurationBuild() {
        configurator.postProcessEnvironment(environment, null);
        assertEquals(CLIENT_ID, environment.getProperty(SPRINGDOC_SWAGGER_UI_OAUTH_CLIENT_ID));
        assertEquals(CLIENT_SECRET, environment.getProperty(SPRINGDOC_SWAGGER_UI_OAUTH_CLIENT_SECRET));
    }

    @Test
    void testSwaggerAuthConfigurationConstants() {
        assertEquals("m2mOauth2", M2M_SECURITY_DEF);
        assertEquals("cloudAdminOauth2", CLOUD_ADMIN_SECURITY_DEF);
        assertEquals("apiKey", API_KEY_SECURITY_DEF);
        assertEquals("https://internal-gateway-test-namespace.test.qubership.org" + IDP_TOKEN_URL,
                m2mTokenUrl);
        assertEquals("https://public-gateway-url" + IDP_TOKEN_URL, tokenUrl);
    }

    @Test
    void testSecuritySchemesMap() {
        assertEquals(3, commonSecuritySchemesMap.size());
        assertNotNull(commonSecuritySchemesMap.get(M2M_SECURITY_DEF));
        assertNotNull(commonSecuritySchemesMap.get(CLOUD_ADMIN_SECURITY_DEF));
        assertNotNull(commonSecuritySchemesMap.get(API_KEY_SECURITY_DEF));
    }


    @Test
    void testCustomizeWithCommonSecuritySchemes() {
        securityCustomiser.customise(testOpenApi);
        Map<String, SecurityScheme> allSecuritySchemes = testOpenApi.getComponents().getSecuritySchemes();
        assertEquals(3, allSecuritySchemes.size());
        assertNotNull(allSecuritySchemes.get(M2M_SECURITY_DEF));
        assertNotNull(allSecuritySchemes.get(CLOUD_ADMIN_SECURITY_DEF));
        assertNotNull(allSecuritySchemes.get(API_KEY_SECURITY_DEF));
    }

    @Test
    void testCustomizeWithCustomComponent() {
        Components components = new Components();
        testOpenApi.setComponents(components);
        securityCustomiser.customise(testOpenApi);
        assertEquals(components, testOpenApi.getComponents());
        Map<String, SecurityScheme> allSecuritySchemes = testOpenApi.getComponents().getSecuritySchemes();
        assertEquals(3, allSecuritySchemes.size());
        assertNotNull(allSecuritySchemes.get(M2M_SECURITY_DEF));
        assertNotNull(allSecuritySchemes.get(CLOUD_ADMIN_SECURITY_DEF));
        assertNotNull(allSecuritySchemes.get(API_KEY_SECURITY_DEF));
    }

    @Test
    void testCustomizeWithCustomComponentAndSecurityScheme() {
        SecurityScheme securityScheme = new SecurityScheme().name("test security scheme")
                .description("test security scheme description").type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.HEADER);
        Components components = new Components();
        testOpenApi.setComponents(components.securitySchemes(new HashMap<>() {{
            put(TEST_SCHEME, securityScheme);
        }}));
        testOpenApi.addSecurityItem(new SecurityRequirement().addList(TEST_SCHEME));

        securityCustomiser.customise(testOpenApi);
        assertEquals(components, testOpenApi.getComponents());
        Map<String, SecurityScheme> allSecuritySchemes = testOpenApi.getComponents().getSecuritySchemes();
        assertEquals(4, allSecuritySchemes.size());
        assertNotNull(allSecuritySchemes.get(M2M_SECURITY_DEF));
        assertNotNull(allSecuritySchemes.get(CLOUD_ADMIN_SECURITY_DEF));
        assertNotNull(allSecuritySchemes.get(API_KEY_SECURITY_DEF));
        assertNotNull(allSecuritySchemes.get(TEST_SCHEME));
    }

}