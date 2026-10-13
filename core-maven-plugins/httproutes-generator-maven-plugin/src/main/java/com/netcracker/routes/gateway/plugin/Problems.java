package com.netcracker.routes.gateway.plugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Errors and warnings of route migration. Any error fails the build before the output file is written.
 */
public record Problems(List<String> errors, List<String> warnings) {

    public Problems() {
        this(new ArrayList<>(), new ArrayList<>());
    }

    void error(String message) {
        errors.add(message);
    }

    void warn(String message) {
        warnings.add(message);
    }
}
