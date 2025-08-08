package com.netcracker.cloud.framework.contexts.clientip;

import com.netcracker.cloud.context.propagation.core.contextdata.IncomingContextData;
import com.netcracker.cloud.context.propagation.core.contextdata.OutgoingContextData;
import com.netcracker.cloud.context.propagation.core.contexts.SerializableContext;
import com.netcracker.cloud.context.propagation.core.contexts.SerializableDataContext;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;

public class ClientIPContextObject implements SerializableContext, SerializableDataContext {
    public static final String X_FORWARDED_FOR = "X-Forwarded-For";
    public static final String X_NC_CLIENT_IP = "X-Nc-Client-Ip";
    private final String clientIp;

    public ClientIPContextObject(@NotNull String clientIp) {
        this.clientIp = clientIp;
    }

    public ClientIPContextObject(@Nullable IncomingContextData contextData) {
        if (contextData != null && contextData.get(X_FORWARDED_FOR) != null) {
            String xForwardedFor = (String) contextData.get(X_FORWARDED_FOR);
            this.clientIp = xForwardedFor.split(",")[0];
        } else if (contextData != null && contextData.get(X_NC_CLIENT_IP) != null) {
            this.clientIp = (String) contextData.get(X_NC_CLIENT_IP);
        } else {
            this.clientIp = "";
        }
    }

    @Override
    public void serialize(OutgoingContextData outgoingContextData) {
        if (StringUtils.isNotBlank(clientIp)) {
            outgoingContextData.set(X_NC_CLIENT_IP, clientIp);
        }
    }

    @Override
    public Map<String, Object> getSerializableContextData() {
        if (StringUtils.isNotBlank(clientIp)) {
            return Map.of(X_NC_CLIENT_IP, clientIp);
        }
        return Collections.emptyMap();
    }

    public String getClientIp() {
        return clientIp;
    }
}
