package com.netcracker.cloud.microserviceframework.application;

import com.netcracker.cloud.dbaas.client.config.EnableServiceDbaasPostgresql;
import com.netcracker.cloud.microserviceframework.BaseApplicationOnWebClient;
import com.netcracker.cloud.security.common.DummyM2MManagerConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.data.elasticsearch.autoconfigure.DataElasticsearchAutoConfiguration;
import org.springframework.cloud.config.client.ConfigClientAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@EnableServiceDbaasPostgresql
@EnableAutoConfiguration(exclude = {DataElasticsearchAutoConfiguration.class, ConfigClientAutoConfiguration.class})
@Import(DummyM2MManagerConfiguration.class)
public class TestApplicationOnWebClient extends BaseApplicationOnWebClient {
}
