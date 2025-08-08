package com.netcracker.cloud.dbaas.client.cassandra.entity.metrics;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public abstract class CommonMetricsConfiguration {
    private List<String> enabled = new ArrayList<>();
}
