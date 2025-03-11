package org.qubership.cloud.quarkus.stomp.ws.runtime.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.stomp.Command;
import io.vertx.ext.stomp.Frame;
import io.vertx.ext.stomp.impl.Queue;
import jakarta.inject.Inject;
import jakarta.websocket.ClientEndpoint;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.DeploymentException;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

@QuarkusTest
class StompJsServerTest {

    static LinkedBlockingDeque<String> MESSAGES = new LinkedBlockingDeque<>();
    static LinkedBlockingDeque<String> ERRORS = new LinkedBlockingDeque<>();

    @Inject
    TestQueueDestinationProviderImpl testQueueDestinationProvider;

    @Inject
    @RestClient
    StompJsInfoService stompJsInfoService;

    String expectedMessage = "unique-message-with-uuid " + UUID.randomUUID();
    String webSocketURL = "ws://localhost:9015/stomp";

    String EXPECTED_INFO_RESPONSE = "{\"websocket\":true}";
    String CONNECTION_REQUEST = "[\"CONNECT\\naccept-version:1.0,1.1,2.0\\nhost:local-host\\n\\n\\u0000\"]";
    String SUBSCRIBE_REQUEST = "[\"SUBSCRIBE\\nid:0\\ndestination:/channels\\n\\n\\u0000\"]";
    String HEARTBEAT_RESPONSE = "h";
    String CONNECTION_IS_OPENED_RESPONSE = "o";

    @Test
    void test_StompJsServer_SuccessConnectionAndSubscribing() throws InterruptedException, DeploymentException, IOException, URISyntaxException {

        try (Session session = ContainerProvider.getWebSocketContainer().connectToServer(Client.class, new URI(webSocketURL))) {

            String value = MESSAGES.poll(10, TimeUnit.SECONDS);
            Assertions.assertEquals(CONNECTION_IS_OPENED_RESPONSE, value);

            Response res = stompJsInfoService.getStompJsInfo();
            Assertions.assertEquals(200, res.getStatus());
            Assertions.assertEquals(EXPECTED_INFO_RESPONSE, res.readEntity(String.class));

            session.getAsyncRemote().sendText(CONNECTION_REQUEST);
            value = MESSAGES.poll(60, TimeUnit.SECONDS);
            Assertions.assertTrue(value != null && value.contains("CONNECTED"));

            session.getAsyncRemote().sendText(SUBSCRIBE_REQUEST);
            value = MESSAGES.poll(60, TimeUnit.SECONDS);
            Assertions.assertEquals(HEARTBEAT_RESPONSE, value);

            Assertions.assertTimeout(Duration.ofSeconds(60), () -> {
                while (!testQueueDestinationProvider.isDestinationSetUp()) {
                    // wait 60 seconds for destination to stand up
                }
            });

            Queue destination = (Queue) testQueueDestinationProvider.getDestination();
            Assertions.assertNotNull(destination);

            Buffer buff = Buffer.buffer(new ObjectMapper().writeValueAsBytes(expectedMessage));
            destination.dispatch(null, new Frame(Command.MESSAGE, new HashMap<>(), buff));

            value = MESSAGES.poll(60, TimeUnit.SECONDS);
            Assertions.assertTrue(value != null && value.contains(expectedMessage));
        }
    }

    @ClientEndpoint
    public static class Client {
        @OnOpen
        public void open(Session session) {
        }

        @OnMessage
        void message(String msg) {
            MESSAGES.add(msg);
        }

        @OnError
        void error(Throwable err) {
            ERRORS.add(err.toString());
        }
    }


    @RegisterRestClient(baseUri = "http://localhost:9015")
    public interface StompJsInfoService {
        @GET
        @Path("/stomp/info")
        Response getStompJsInfo();
    }
}
