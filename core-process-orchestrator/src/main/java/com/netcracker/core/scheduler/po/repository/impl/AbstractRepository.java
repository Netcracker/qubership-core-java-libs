package com.netcracker.core.scheduler.po.repository.impl;

import com.github.kagkarlsson.jdbc.JdbcRunner;

import javax.sql.DataSource;

public class AbstractRepository {

    protected JdbcRunner jdbcRunner;
    protected final String tableName;

    public AbstractRepository(DataSource dataSource, String tableName) {
        this.jdbcRunner = new JdbcRunner(dataSource, true);
        this.tableName = tableName;
    }



}
