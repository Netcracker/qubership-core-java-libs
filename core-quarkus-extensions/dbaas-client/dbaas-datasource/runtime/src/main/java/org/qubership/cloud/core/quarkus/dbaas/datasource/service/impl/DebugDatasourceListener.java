package org.qubership.cloud.core.quarkus.dbaas.datasource.service.impl;

import io.agroal.api.AgroalDataSourceListener;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.util.Arrays;

@Slf4j
public class DebugDatasourceListener implements AgroalDataSourceListener {
    @Override
    public void beforeConnectionCreation() {
        log.debug("beforeConnectionCreation");
    }

    @Override
    public void onConnectionCreation(Connection connection) {
        log.debug("onConnectionCreation {}", connection);
    }

    @Override
    public void onConnectionPooled(Connection connection) {
        log.debug("onConnectionPooled {}", connection);
    }

    @Override
    public void onConnectionAcquire(Connection connection) {
        log.debug("onConnectionAcquire {}", connection);
    }

    @Override
    public void beforeConnectionReturn(Connection connection) {
        log.debug("beforeConnectionReturn");
    }

    @Override
    public void onConnectionReturn(Connection connection) {
        log.debug("onConnectionReturn {}", connection);
    }

    @Override
    public void onConnectionLeak(Connection connection, Thread thread) {
        StringBuilder builder = new StringBuilder();
        Arrays.stream(thread.getStackTrace()).forEach(builder::append);
        log.debug("onConnectionLeak {}, thread: {}, stacktrace: {}", connection, thread, builder.toString());
    }

    @Override
    public void beforeConnectionValidation(Connection connection) {
        log.debug("beforeConnectionValidation {}", connection);
    }

    @Override
    public void onConnectionValid(Connection connection) {
        log.debug("onConnectionValid {}", connection);
    }

    @Override
    public void onConnectionInvalid(Connection connection) {
        log.debug("onConnectionInvalid {}", connection);
    }

    @Override
    public void beforeConnectionFlush(Connection connection) {
        log.debug("beforeConnectionFlush {}", connection);
    }

    @Override
    public void onConnectionFlush(Connection connection) {
        log.debug("onConnectionFlush {}", connection);
    }

    @Override
    public void beforeConnectionReap(Connection connection) {
        log.debug("beforeConnectionReap {}", connection);
    }

    @Override
    public void onConnectionReap(Connection connection) {
        log.debug("onConnectionReap {}", connection);
    }

    @Override
    public void beforeConnectionDestroy(Connection connection) {
        log.debug("beforeConnectionDestroy {}", connection);
    }

    @Override
    public void onConnectionDestroy(Connection connection) {
        log.debug("onConnectionDestroy {}", connection);
    }

    @Override
    public void onWarning(String message) {
        log.debug("onWarning {}", message);
    }

    @Override
    public void onWarning(Throwable throwable) {
        log.warn("onWarning", throwable);
    }

    @Override
    public void onInfo(String message) {
        log.debug("onInfo {}", message);
    }
}
