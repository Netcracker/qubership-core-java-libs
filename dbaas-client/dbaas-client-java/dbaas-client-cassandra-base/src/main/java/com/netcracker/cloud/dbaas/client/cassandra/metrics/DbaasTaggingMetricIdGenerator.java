package com.netcracker.cloud.dbaas.client.cassandra.metrics;

import com.datastax.oss.driver.api.core.config.DriverOption;
import com.datastax.oss.driver.api.core.context.DriverContext;
import com.datastax.oss.driver.api.core.metadata.Node;
import com.datastax.oss.driver.api.core.metrics.NodeMetric;
import com.datastax.oss.driver.api.core.metrics.SessionMetric;
import com.datastax.oss.driver.internal.core.metrics.DefaultMetricId;
import com.datastax.oss.driver.internal.core.metrics.MetricId;
import com.datastax.oss.driver.internal.core.metrics.TaggingMetricIdGenerator;
import com.datastax.oss.driver.shaded.guava.common.collect.ImmutableMap;
import edu.umd.cs.findbugs.annotations.NonNull;

import java.util.HashMap;
import java.util.Map;

public class DbaasTaggingMetricIdGenerator extends TaggingMetricIdGenerator {

    static final DriverOption DBAAS_METRICS_TAGS = () -> "dbaas.cassandra.metrics.tags";

    private Map<String, String> dbaasTags;

    public DbaasTaggingMetricIdGenerator(DriverContext context) {
        super(context);
        dbaasTags = context.getConfig().getDefaultProfile().getStringMap(DBAAS_METRICS_TAGS, new HashMap<>());
    }

    @NonNull
    @Override
    public MetricId sessionMetricId(@NonNull SessionMetric metric) {
        MetricId baseMetricId = super.sessionMetricId(metric);
        return new DefaultMetricId(baseMetricId.getName(), ImmutableMap.<String, String>builder().putAll(baseMetricId.getTags()).putAll(dbaasTags).build());
    }

    @NonNull
    @Override
    public MetricId nodeMetricId(@NonNull Node node, @NonNull NodeMetric metric) {
        MetricId baseMetricId = super.nodeMetricId(node, metric);
        return new DefaultMetricId(baseMetricId.getName(), ImmutableMap.<String, String>builder().putAll(baseMetricId.getTags()).putAll(dbaasTags).build());
    }
}
