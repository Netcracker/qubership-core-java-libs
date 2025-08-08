package com.netcracker.cloud.microserviceframework;

import com.netcracker.cloud.restlegacy.restclient.configuration.annotation.EnableControllersAdvice;
import com.netcracker.cloud.restlegacy.restclient.configuration.annotation.EnableMessagesResolving;
import com.netcracker.cloud.routeregistration.resttemplate.EnableRouteRegistrationOnRestTemplate;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableControllersAdvice
@EnableMessagesResolving
@EnableRouteRegistrationOnRestTemplate
public class BaseApplicationOnRestTemplate extends BaseApplicationCommon {
}
