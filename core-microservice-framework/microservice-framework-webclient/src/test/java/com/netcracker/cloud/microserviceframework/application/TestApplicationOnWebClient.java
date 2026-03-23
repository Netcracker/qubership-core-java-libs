package com.netcracker.cloud.microserviceframework.application;

import com.netcracker.cloud.dbaas.client.config.EnableServiceDbaasPostgresql;
import com.netcracker.cloud.microserviceframework.BaseApplicationOnWebClient;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.data.elasticsearch.autoconfigure.DataElasticsearchAutoConfiguration;
import org.springframework.cloud.config.client.ConfigClientAutoConfiguration;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableServiceDbaasPostgresql
@EnableAutoConfiguration(exclude = {DataElasticsearchAutoConfiguration.class, ConfigClientAutoConfiguration.class})
public class TestApplicationOnWebClient extends BaseApplicationOnWebClient {
}
