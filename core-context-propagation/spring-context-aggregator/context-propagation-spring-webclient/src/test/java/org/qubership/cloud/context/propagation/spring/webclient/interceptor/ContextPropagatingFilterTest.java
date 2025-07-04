package org.qubership.cloud.context.propagation.spring.webclient.interceptor;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.qubership.cloud.context.propagation.core.ContextManager;
import org.qubership.cloud.framework.contexts.acceptlanguage.AcceptLanguageContextObject;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static jakarta.ws.rs.core.HttpHeaders.ACCEPT_LANGUAGE;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ContextPropagatingFilterTest {
    private static final String ACCEPT_LANGUAGE_VALUE = "en-En";
    private Scheduler nettyNio;

    @BeforeEach
    void init() {
        ContextManager.clearAll();
        nettyNio = Schedulers.newSingle("test-netty-thread", true);//emulates netty nio

        nettyNio.schedule(() -> {});//warmup. it should not be propagated by the Thread Inheritance
        CoreContextPropagator.installHook();
        ContextManager.set(ACCEPT_LANGUAGE, new AcceptLanguageContextObject(ACCEPT_LANGUAGE_VALUE));

    }

    @AfterEach
    public void tearDown() {
        nettyNio.dispose();
        ContextManager.clearAll();
    }

    @Test
    void testHeadersAreSet() {
        List<ClientRequest> requests = new CopyOnWriteArrayList<>();
        final WebClient client = WebClient.builder()
                .exchangeFunction(request -> {
                    requests.add(request);
                    return Mono.<ClientResponse>error(new RuntimeException("boom"))//got error from the server
                            .publishOn(nettyNio);
                })
                .filter(new SpringWebClientInterceptor())
                .build();

        client.get()
                .uri("/someUrl")
                .retrieve()
                .bodyToMono(String.class)
                //retry will be performed in a netty thread so there is no Thread context data.
                //so Second attempt will not send headers from the original context
                .retry(1)
                .onErrorReturn("")
                .block(Duration.ofSeconds(100));


        assertEquals(2, requests.size());
        assertEquals(ACCEPT_LANGUAGE_VALUE, requests.get(0).headers().toSingleValueMap().get(HttpHeaders.ACCEPT_LANGUAGE));
        assertEquals(ACCEPT_LANGUAGE_VALUE, requests.get(1).headers().toSingleValueMap().get(HttpHeaders.ACCEPT_LANGUAGE));
    }
}