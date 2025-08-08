package com.netcracker.cloud.dbaas.client.cassandra.service;

import com.datastax.oss.driver.api.core.CqlSessionBuilder;
import com.datastax.oss.driver.api.core.config.ProgrammaticDriverConfigLoaderBuilder;

public interface DbaasCqlSessionBuilderCustomizer {

    void customize(CqlSessionBuilder cqlSessionBuilder);

    void customize(ProgrammaticDriverConfigLoaderBuilder configLoader);

}
