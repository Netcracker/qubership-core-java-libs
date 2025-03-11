package org.qubership.cloud.quarkus.routesregistration.deployment.config;

import jakarta.enterprise.context.ApplicationScoped;
import okhttp3.Request;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@ApplicationScoped
public class RequestRecorder {
    private final Set<Request> records = new HashSet<>();

    public void record(Request req) {
        this.records.add(req);
    }

    public void clear() {
        this.records.clear();
    }

    public Set<Request> records() {
        return Collections.unmodifiableSet(this.records);
    }
}
