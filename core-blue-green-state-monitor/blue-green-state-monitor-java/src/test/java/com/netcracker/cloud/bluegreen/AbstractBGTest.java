package com.netcracker.cloud.bluegreen;

import com.netcracker.cloud.bluegreen.impl.http.HttpClientAdapter;
import lombok.SneakyThrows;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.consul.ConsulContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

/** Shared fixture: a Consul container per test method, plus small waiting helpers. */
@Testcontainers
abstract class AbstractBGTest {

    String ns1 = "ns-1";
    String ns2 = "ns-2";
    String ms = "ms-1";
    String pod1 = "pod-1";
    String pod2 = "pod-2";

    List<String> namespaces = List.of(ns1, ns2);

    Supplier<String> consulTokenSupplier = () -> "test";
    HttpClientAdapter client = new HttpClientAdapter(consulTokenSupplier);

    String consulUrl;

    private static final Duration NODE_REGISTRATION_TIMEOUT = Duration.ofSeconds(30);

    @Container
    ConsulContainer consulContainer = new ConsulContainer("hashicorp/consul:1.16");

    @BeforeEach
    void before() {
        consulUrl = String.format("http://%s:%d", consulContainer.getHost(), consulContainer.getMappedPort(8500));
        awaitNodeRegistered();
    }

    /**
     * The agent answers on its port before it has registered itself in the catalog, and a session
     * cannot be bound to a node that is not there yet: consul replies 500 "Missing node registration".
     */
    private void awaitNodeRegistered() {
        Awaitility.await("consul node registered in the catalog")
                .atMost(NODE_REGISTRATION_TIMEOUT)
                .pollInterval(Duration.ofMillis(100))
                .ignoreExceptions()
                .until(this::catalogHasNodes);
    }

    private boolean catalogHasNodes() {
        String nodes = client.invoke(req -> req.uri(URI.create(consulUrl + "/v1/catalog/nodes")).GET(),
                String.class).sendAndGet();
        return nodes != null && !nodes.isBlank() && !nodes.strip().equals("[]");
    }

    @SneakyThrows
    static void run(Task task) {
        task.run();
    }

    @SneakyThrows
    static <T> T run(Callable<T> task) {
        return task.call();
    }

    interface Task {
        void run() throws Exception;
    }

    interface AssertionErrorTask {
        void run() throws AssertionError;
    }

    static void retry(Duration timeout, AssertionErrorTask task) {
        Instant start = Instant.now();
        while (true) {
            try {
                task.run();
                return;
            } catch (AssertionError e) {
                if (Duration.between(start, Instant.now()).compareTo(timeout) > 0) {
                    throw e;
                }
            }
        }
    }


    static long getThreadsCount(String name) {
        return Thread.getAllStackTraces().keySet().stream().filter(t -> t.getName().contains(name)).count();
    }

}
