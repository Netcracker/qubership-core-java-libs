package com.netcracker.routes.gateway.plugin;

import java.util.Set;

public record Rule(String match, String target, long timeout, Set<String> sources) {
}
