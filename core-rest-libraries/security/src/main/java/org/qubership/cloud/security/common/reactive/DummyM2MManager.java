package org.qubership.cloud.security.common.reactive;

import org.qubership.cloud.security.core.auth.Token;
import reactor.core.publisher.Mono;

public class DummyM2MManager implements M2MManager {

    @Override
    public Mono<Token> getToken() {
        return Mono.just(Token.DUMMY_TOKEN);

    }
}
