package com.netcracker.cloud.microserviceframework.resttemplate.sample;

import com.netcracker.cloud.dbaas.client.config.EnableServiceDbaasPostgresql;
import com.netcracker.cloud.microserviceframework.BaseApplicationOnRestTemplate;
import com.netcracker.cloud.routesregistration.common.annotation.Route;
import com.netcracker.cloud.routesregistration.common.gateway.route.RouteType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@SpringBootApplication
@EnableServiceDbaasPostgresql
public class TestApplication extends BaseApplicationOnRestTemplate {

    @Value("${test.key}")
    public String testValue;

    @Route(RouteType.PUBLIC)
    @RequestMapping("/test")
    public String query() {
        return testValue;
    }

}

