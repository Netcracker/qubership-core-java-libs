package org.qubership.cloud.security.common.reactive;

import org.qubership.cloud.security.core.auth.Token;
import reactor.core.publisher.Mono;

public interface M2MManager {
    Mono<Token> getToken();
}
