package com.netcracker.cloud.framework.contexts.xrequestid;

import com.netcracker.cloud.context.propagation.core.contextdata.IncomingContextData;
import com.netcracker.cloud.context.propagation.core.contextdata.OutgoingContextData;
import com.netcracker.cloud.context.propagation.core.contexts.ResponsePropagatableContext;
import com.netcracker.cloud.context.propagation.core.contexts.SerializableContext;
import com.netcracker.cloud.context.propagation.core.contexts.SerializableDataContext;
import com.netcracker.cloud.framework.contexts.allowedheaders.HeaderPropagationConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import java.util.Collections;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.util.Map;

/**
 * Associates a given request id with the current execution thread. The purpose of the class is to provide a convenient
 * way to manage X_REQUEST_ID header.
 */
public class XRequestIdContextObject implements SerializableContext,
        ResponsePropagatableContext, SerializableDataContext {
    private static final SecureRandom random = new SecureRandom();
    public static final String X_REQUEST_ID = "X-Request-Id";

    private String requestId;
    private static final Logger log = LoggerFactory.getLogger(XRequestIdContextObject.class);

    public XRequestIdContextObject(@Nullable IncomingContextData contextData) {
        if (contextData != null && contextData.get(X_REQUEST_ID) != null) {
            this.requestId = (String) contextData.get(X_REQUEST_ID);
        } else {
            this.requestId = generateRequestId();
            log.debug("Generated new request-id: {}", requestId);
        }
    }

    @NotNull
    private String generateRequestId() {
        String requestId;
        requestId = System.currentTimeMillis() + "." + random.nextDouble();
        return requestId;
    }

    public XRequestIdContextObject(String requestId) {
        this.requestId = requestId;
    }

    @Override
    public void serialize(OutgoingContextData outgoingContextData) {
        if (!HeaderPropagationConfiguration.isBlacklisted(X_REQUEST_ID)) {
            outgoingContextData.set(X_REQUEST_ID, requestId);
        }
    }

    public String getRequestId() {
        return requestId;
    }

    @Override
    public void propagate(OutgoingContextData outgoingContextData) {
        if (!HeaderPropagationConfiguration.isBlacklisted(X_REQUEST_ID)) {
            outgoingContextData.set(X_REQUEST_ID, requestId);
        }
    }

    @Override
    public Map<String, Object> getSerializableContextData() {
        if (HeaderPropagationConfiguration.isBlacklisted(X_REQUEST_ID)) {
            return Collections.emptyMap();
        }
        return Map.of(X_REQUEST_ID, requestId);
    }
}
