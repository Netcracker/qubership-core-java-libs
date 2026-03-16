package com.netcracker.cloud.security.common.webclient;

import org.springframework.web.reactive.function.client.ExchangeFilterFunction;

public interface AuthorizationHeaderInterceptorFactory {
    ExchangeFilterFunction build(Mode mode);
}
