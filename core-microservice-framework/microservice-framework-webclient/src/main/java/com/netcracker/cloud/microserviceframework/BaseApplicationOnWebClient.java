package com.netcracker.cloud.microserviceframework;

import com.netcracker.cloud.routeregistration.webclient.EnableRouteRegistrationOnWebClient;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableRouteRegistrationOnWebClient
public class BaseApplicationOnWebClient extends BaseApplicationCommon {
}
