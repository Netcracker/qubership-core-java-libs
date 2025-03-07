package org.qubership.cloud.frameworkextensions.swagger.config;


import io.swagger.v3.core.filter.SpecFilter;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.*;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;

import java.util.HashMap;
import java.util.Map;

/**
 * Class {@code SwaggerAuthConfiguration} contains common
 * configurations, which are needed for authorization
 * in Swagger UI.
 */
@Slf4j
@Configuration
public class SwaggerAuthConfiguration {
    /**
     * Name of the M2M Swagger Security Scheme. Can be used in
     * {@link io.swagger.v3.oas.annotations.security.SecurityRequirement} annotation inside the
     * {@link io.swagger.v3.oas.annotations.Operation} annotation to specify
     * that operation uses M2M role.
     */
    public static final String M2M_SECURITY_DEF = "m2mOauth2";

    /**
     * Name of the CLOUD-ADMIN Swagger Security Scheme. Can be used in
     * {@link io.swagger.v3.oas.annotations.security.SecurityRequirement} annotation inside the
     * {@link io.swagger.v3.oas.annotations.Operation} annotation to specify
     * that operation uses CLOUD-ADMIN role.
     */
    public static final String CLOUD_ADMIN_SECURITY_DEF = "cloudAdminOauth2";

    /**
     * Name of the API Key Swagger Security Scheme. Can be used in
     * {@link io.swagger.v3.oas.annotations.security.SecurityRequirement} annotation inside the
     * {@link io.swagger.v3.oas.annotations.Operation} annotation to pass
     * any token through input field in Swagger UI.
     */
    public static final String API_KEY_SECURITY_DEF = "apiKey";

    public static final String M2M_SECURITY_DESCR = "M2M OAuth. You should open a route on internal-gateway service to use it in Swagger UI.";
    public static final String CLOUD_ADMIN_SECURITY_DESCR = "cloud-admin Oauth (IMPORTANT! change client_id to 'frontend' and leave client_secret blank !)";
    public static final String API_KEY_SECURITY_HEADER = "Authorization";
    public static final String API_KEY_SECURITY_DESCR = "Alows to pass any token in Authorization header. Pass the value as: Bearer $token";
    private static final String REALM = "cloud-common";
    private static final String IDP_TOKEN_URL = "/api/v1/identity-provider/auth/realms/" + REALM + "/protocol/openid-connect/token";


    @Bean
    String m2mTokenUrl(@Value("${CLOUD_NAMESPACE:}") String cloudNamespace,
                       @Value("${CLOUD_PUBLIC_HOST:}") String cloudPublicHost) {
        return "https://internal-gateway-" + cloudNamespace + "." + cloudPublicHost + IDP_TOKEN_URL;
    }

    @Bean
    String tokenUrl(@Value("${apigateway.external.public.url}") String publicGatewayUrl) {
        return publicGatewayUrl + IDP_TOKEN_URL;
    }

    @Bean
    public SecurityScheme m2mOauth2(SwaggerSecurityConfigurator swaggerSecurityConfigurator,
                                    ConfigurableEnvironment environment,
                                    String m2mTokenUrl) {
        swaggerSecurityConfigurator.postProcessEnvironment(environment, null);
        OAuthFlow clientCredentialsFlow = new OAuthFlow().tokenUrl(m2mTokenUrl);
        clientCredentialsFlow.setScopes(new Scopes());
        return new SecurityScheme()
                .description(M2M_SECURITY_DESCR)
                .type(SecurityScheme.Type.OAUTH2)
                .flows(new OAuthFlows()
                        .clientCredentials(clientCredentialsFlow));
    }

    @Bean
    public SecurityScheme cloudAdminOauth2(String tokenUrl) {
        OAuthFlow passwordFlow = new OAuthFlow().tokenUrl(tokenUrl);
        passwordFlow.setScopes(new Scopes());
        return new SecurityScheme()
                .description(CLOUD_ADMIN_SECURITY_DESCR)
                .type(SecurityScheme.Type.OAUTH2)
                .flows(new OAuthFlows()
                        .password(passwordFlow));
    }

    @Bean
    public SecurityScheme apiKey() {
        return new SecurityScheme()
                .name(API_KEY_SECURITY_HEADER)
                .description(API_KEY_SECURITY_DESCR)
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.HEADER);
    }

    @Bean
    public Map<String, SecurityScheme> commonSecuritySchemesMap(SecurityScheme m2mOauth2, SecurityScheme cloudAdminOauth2, SecurityScheme apiKey) {
        Map<String, SecurityScheme> commonSecuritySchemesMap = new HashMap<>();
        commonSecuritySchemesMap.put(M2M_SECURITY_DEF, m2mOauth2);
        commonSecuritySchemesMap.put(CLOUD_ADMIN_SECURITY_DEF, cloudAdminOauth2);
        commonSecuritySchemesMap.put(apiKey.getName(), apiKey);
        return commonSecuritySchemesMap;
    }

    @Bean
    public SwaggerSecurityConfigurator swaggerSecurityConfigurator() {
        return new SwaggerSecurityConfigurator();
    }

    @Bean
    public SecurityCustomiser securityCustomiser(Map<String, SecurityScheme> commonSecuritySchemesMap, @Autowired (required = false) OpenAPI ncOpenApi) {
        SecurityCustomiser securityCustomiser = null;
        if(null != ncOpenApi) {
            securityCustomiser = new SecurityCustomiser(commonSecuritySchemesMap);
            securityCustomiser.customise(ncOpenApi);
        }
        return securityCustomiser;
    }

    public class SecurityCustomiser extends SpecFilter implements OpenApiCustomizer {

        Map<String, SecurityScheme> commonSecuritySchemesMap;

        public SecurityCustomiser(Map<String, SecurityScheme> commonSecuritySchemesMap) {
            this.commonSecuritySchemesMap = commonSecuritySchemesMap;
        }

        @Override
        public void customise(OpenAPI openApi) {
            Components components = openApi.getComponents();
            if (components == null) {
                components = new Components().securitySchemes(this.commonSecuritySchemesMap);
            } else {
                if (components.getSecuritySchemes() == null) {
                    components.securitySchemes(this.commonSecuritySchemesMap);
                } else {
                    components.getSecuritySchemes().putAll(this.commonSecuritySchemesMap);
                }
            }
            openApi.addSecurityItem(new SecurityRequirement().addList(M2M_SECURITY_DEF)
                            .addList(CLOUD_ADMIN_SECURITY_DEF).addList(API_KEY_SECURITY_DEF))
                    .components(components);
        }

    }

}
