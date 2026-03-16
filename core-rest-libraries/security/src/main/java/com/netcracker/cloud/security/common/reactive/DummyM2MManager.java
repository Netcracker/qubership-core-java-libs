package com.netcracker.cloud.security.common.reactive;

import com.netcracker.cloud.security.core.auth.Token;
import reactor.core.publisher.Mono;

public class DummyM2MManager implements M2MManager {

    @Override
    public Mono<Token> getToken() {
        return Mono.just(Token.DUMMY_TOKEN);

    }
}
