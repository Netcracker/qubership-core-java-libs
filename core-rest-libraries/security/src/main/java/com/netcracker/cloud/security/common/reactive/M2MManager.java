package com.netcracker.cloud.security.common.reactive;

import com.netcracker.cloud.security.core.auth.Token;
import reactor.core.publisher.Mono;

public interface M2MManager {
    Mono<Token> getToken();
}
