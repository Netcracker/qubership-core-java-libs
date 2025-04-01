package org.qubership.cloud.microserviceframework;

import org.qubership.cloud.restlegacy.restclient.configuration.annotation.EnableControllersAdvice;
import org.qubership.cloud.restlegacy.restclient.configuration.annotation.EnableMessagesResolving;
import org.qubership.cloud.routeregistration.resttemplate.EnableRouteRegistrationOnRestTemplate;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableControllersAdvice
@EnableMessagesResolving
@EnableRouteRegistrationOnRestTemplate
public class BaseApplicationOnRestTemplate extends BaseApplicationCommon {
}
