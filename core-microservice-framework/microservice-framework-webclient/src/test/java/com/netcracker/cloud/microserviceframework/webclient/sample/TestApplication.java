package com.netcracker.cloud.microserviceframework.webclient.sample;

import com.netcracker.cloud.dbaas.client.config.EnableServiceDbaasPostgresql;
import com.netcracker.cloud.microserviceframework.BaseApplicationOnWebClient;
import com.netcracker.cloud.routesregistration.common.annotation.Route;
import com.netcracker.cloud.routesregistration.common.gateway.route.RouteType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.data.elasticsearch.autoconfigure.DataElasticsearchAutoConfiguration;
import org.springframework.cloud.config.client.ConfigClientAutoConfiguration;
import org.springframework.context.annotation.Import;
import com.netcracker.cloud.security.common.DummyM2MManagerConfiguration;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@SpringBootApplication(exclude = {DataElasticsearchAutoConfiguration.class, ConfigClientAutoConfiguration.class})
@EnableServiceDbaasPostgresql
@Import(DummyM2MManagerConfiguration.class)
public class TestApplication extends BaseApplicationOnWebClient {

    @Value("${test.key}")
    public String testValue;

    @Route(RouteType.PUBLIC)
    @RequestMapping("/test")
    public String query() {
        return testValue;
    }
}

