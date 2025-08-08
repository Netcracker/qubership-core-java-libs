package com.netcracker.cloud.dbaas.client.service.flyway;

import lombok.Getter;

import javax.sql.DataSource;

@Getter
public class FlywayContext {

    private final DataSource dataSource;

    public FlywayContext(DataSource dataSource) {
        this.dataSource = dataSource;
    }
}
