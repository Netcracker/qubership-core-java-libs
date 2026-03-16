package com.netcracker.cloud.framework.contexts.allowedheaders;

import com.netcracker.cloud.context.propagation.core.RegisterProvider;
import com.netcracker.cloud.context.propagation.core.contextdata.IncomingContextData;
import com.netcracker.cloud.context.propagation.core.supports.providers.AbstractContextProviderOnInheritableThreadLocal;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

@RegisterProvider
public class AllowedHeadersProvider extends AbstractContextProviderOnInheritableThreadLocal<AllowedHeadersContextObject> {
    public static final String ALLOWED_HEADER = "allowed_header";
    public static final String HEADERS_PROPERTY = "headers.allowed";

    public static final String HEADERS_ENV = "headers_allowed";
    private List<String> headers;

    private synchronized void setUpHeadersFromProperties() {
        if (headers == null) { // double-check after monitor is obtained
            String allowedHeaders = "";
            if (System.getProperty(HEADERS_PROPERTY) != null) {
                allowedHeaders = System.getProperty(HEADERS_PROPERTY);
            } else if (System.getenv(HEADERS_ENV) != null) {
                allowedHeaders = System.getenv(HEADERS_ENV);
            }
            this.headers = Arrays.asList(allowedHeaders.split(","));
        }
    }

    @Override
    public final String contextName() {
        return ALLOWED_HEADER;
    }

    @Override
    public AllowedHeadersContextObject provide(@Nullable IncomingContextData contextData) {
        if (this.headers == null) setUpHeadersFromProperties();
        return new AllowedHeadersContextObject(contextData, this.headers);
    }
}
