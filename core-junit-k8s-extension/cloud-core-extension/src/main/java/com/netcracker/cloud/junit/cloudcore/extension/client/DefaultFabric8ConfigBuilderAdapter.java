package com.netcracker.cloud.junit.cloudcore.extension.client;

import com.netcracker.cloud.junit.cloudcore.extension.annotations.Priority;
import io.fabric8.kubernetes.client.ConfigBuilder;

@Priority
public class DefaultFabric8ConfigBuilderAdapter implements Fabric8ConfigBuilderAdapter {

    public static final String REQ_RETRY_BACKOFF_LIMIT = "k8s.request.retry.backoff.limit";
    public static final String CONNECTION_TIMEOUT_MS = "k8s.connection.timeout.ms";
    public static final String REQUEST_TIMEOUT_MS = "k8s.request.timeout.ms";
    public static final String WATCH_RECONNECT_INTERVAL_MS = "k8s.watch.reconnect.interval.ms";
    public static final String WATCH_RECONNECT_LIMIT = "k8s.watch.reconnect.limit";
    public static final String WS_PING_INTERVAL_MS = "k8s.websocket.ping.interval.ms";

    public static final int DEF_REQ_RETRY_BACKOFF_LIMIT = 3;
    public static final int DEF_CONNECTION_TIMEOUT_MS = 10_000;
    public static final int DEF_REQUEST_TIMEOUT_MS = 60_000;
    public static final int DEF_WATCH_RECONNECT_INTERVAL_MS = 3_000;
    public static final int DEF_WATCH_RECONNECT_LIMIT = 5;
    public static final int DEF_WS_PING_INTERVAL_MS = 10_000;

    public ConfigBuilder adapt(ConfigBuilder configBuilder) {
        return configBuilder
                .withTrustCerts(true)
                .withDisableHostnameVerification(true)

                .withRequestRetryBackoffLimit(Integer.getInteger(REQ_RETRY_BACKOFF_LIMIT, DEF_REQ_RETRY_BACKOFF_LIMIT).intValue())
                .withConnectionTimeout(Integer.getInteger(CONNECTION_TIMEOUT_MS, DEF_CONNECTION_TIMEOUT_MS).intValue())
                .withRequestTimeout(Integer.getInteger(REQUEST_TIMEOUT_MS, DEF_REQUEST_TIMEOUT_MS).intValue())
                .withWatchReconnectInterval(Integer.getInteger(WATCH_RECONNECT_INTERVAL_MS, DEF_WATCH_RECONNECT_INTERVAL_MS).intValue())
                .withWatchReconnectLimit(Integer.getInteger(WATCH_RECONNECT_LIMIT, DEF_WATCH_RECONNECT_LIMIT).intValue())
                .withWebsocketPingInterval(Integer.getInteger(WS_PING_INTERVAL_MS, DEF_WS_PING_INTERVAL_MS).longValue());
    }
}
