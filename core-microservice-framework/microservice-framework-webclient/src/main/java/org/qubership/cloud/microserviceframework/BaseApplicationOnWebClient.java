package org.qubership.cloud.microserviceframework;

import org.qubership.cloud.routeregistration.webclient.EnableRouteRegistrationOnWebClient;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableRouteRegistrationOnWebClient
public class BaseApplicationOnWebClient extends BaseApplicationCommon {
}
