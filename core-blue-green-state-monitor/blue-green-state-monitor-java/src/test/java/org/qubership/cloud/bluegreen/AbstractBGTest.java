package org.qubership.cloud.bluegreen;

import org.qubership.cloud.bluegreen.impl.http.HttpClientAdapter;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.consul.ConsulContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

@Testcontainers
class AbstractBGTest {

    String ns1 = "ns-1";
    String ns2 = "ns-2";
    String ms = "ms-1";
    String pod1 = "pod-1";
    String pod2 = "pod-2";

    List<String> namespaces = List.of(ns1, ns2);

    Supplier<String> consulTokenSupplier = () -> "test";
    HttpClientAdapter client = new HttpClientAdapter(consulTokenSupplier);

    String consulUrl;

    @Container
    ConsulContainer consulContainer = new ConsulContainer("hashicorp/consul:1.16");

    @BeforeEach
    void before() {
        consulUrl = String.format("http://%s:%d", consulContainer.getHost(), consulContainer.getMappedPort(8500));
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
