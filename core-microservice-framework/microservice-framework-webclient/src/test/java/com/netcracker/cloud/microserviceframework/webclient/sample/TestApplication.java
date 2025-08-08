package com.netcracker.cloud.microserviceframework.webclient.sample;

import org.qubership.cloud.dbaas.client.config.EnableServiceDbaasPostgresql;
import org.qubership.cloud.microserviceframework.BaseApplicationOnWebClient;
import org.qubership.cloud.routesregistration.common.annotation.Route;
import org.qubership.cloud.routesregistration.common.gateway.route.RouteType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@SpringBootApplication
@EnableServiceDbaasPostgresql
public class TestApplication extends BaseApplicationOnWebClient {

    @Value("${test.key}")
    public String testValue;

    @Route(RouteType.PUBLIC)
    @RequestMapping("/test")
    public String query() {
        return testValue;
    }
}

