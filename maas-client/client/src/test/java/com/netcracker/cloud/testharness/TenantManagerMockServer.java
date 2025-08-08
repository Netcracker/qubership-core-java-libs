package org.qubership.cloud.testharness;

import io.undertow.Undertow;
import io.undertow.websockets.WebSocketConnectionCallback;
import io.undertow.websockets.core.AbstractReceiveListener;
import io.undertow.websockets.core.BufferedTextMessage;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.WebSockets;
import io.undertow.websockets.spi.WebSocketHttpExchange;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.net.ServerSocket;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static io.undertow.Handlers.path;
import static io.undertow.Handlers.websocket;

@Slf4j
public class TenantManagerMockServer implements WebsockServer {
    private static final String QUIT_MESSAGE = "quit." + UUID.randomUUID();
    private final Undertow server;

    private final int serverPort;
    private final BlockingQueue<String> messagesQueue = new LinkedBlockingQueue<>();
    private volatile Timer heartbeatTimer;

    public TenantManagerMockServer() {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            serverPort = serverSocket.getLocalPort();
        } catch (Exception e) {
            throw new RuntimeException("Can't find free port to start tenant-manager mock server", e);
        }

        server = Undertow.builder()
                .setIoThreads(10)
                .setWorkerThreads(10)
                .addHttpListener(serverPort, "localhost")
                .setHandler(path()
                        .addPrefixPath("/api/v4/tenant-manager/watch/", websocket(new WebSocketConnectionCallback() {
                            @Override
                            @SneakyThrows
                            public void onConnect(WebSocketHttpExchange exchange, WebSocketChannel channel) {
                                log.info("Client connected");
                                channel.getReceiveSetter().set(new AbstractReceiveListener() {
                                    @SneakyThrows
                                    protected void onFullTextMessage(WebSocketChannel channel, BufferedTextMessage message) {
                                        String command = message.getData();
                                        if (command.startsWith("[\"CONNECT")) {
                                            log.info("Receive connect command");
                                            String strangeResponse = "o";
                                            WebSockets.sendText(strangeResponse, channel, null);
                                            String connectAckMessage = "a[\"CONNECTED\\nserver:vertx-stomp/3.9.2\\nheart-beat:1000,1000\\nsession:fb407370-5ee5-42a3-a842-d0f26de0ba35\\nversion:1.2\\n\\n\\u0000\"]";
                                            WebSockets.sendText(connectAckMessage, channel, null);
                                        } else if (command.startsWith("[\"SUBSCRIBE")) {
                                            log.info("Receive subscribe command");
                                            log.info("Switched in event sender mode...");
                                            while (true) {
                                                String event = messagesQueue.poll(100, TimeUnit.MILLISECONDS);
                                                if (event == null) {
                                                    continue;
                                                }

                                                if (QUIT_MESSAGE.equals(event)) {
                                                    log.info("Quit events emu queue");
                                                    break;
                                                }
                                                log.info("Send event: {}", event);
                                                WebSockets.sendText(event, channel, null);
                                            }
                                        }
                                    }
                                });
                                channel.resumeReceives();
                            }
                        }))
                ).build();
    }

    public String addFirstActivatedTenant() {
        messagesQueue.add("a[\"MESSAGE\\nmessage-id:90c683e4-80a5-4b3f-be4d-2e8881162b2a\\nsubscription:9abb0688-6426-4c88-a535-64ba223e89d2\\n\\n" +
                "{\\\"type\\\":\\\"MODIFIED\\\",\\\"tenants\\\":[{\\\"externalId\\\":\\\"233a1c4c-dde1-4766-9ca5-c0b21400c2b7\\\",\\\"objectId\\\":\\\"57b5e723-3970-4cde-a5d1-cfee509eecd0\\\",\\\"namespace\\\":null,\\\"status\\\":\\\"ACTIVE\\\",\\\"name\\\":\\\"user-tenant2\\\",\\\"domainName\\\":null,\\\"workbook\\\":null,\\\"admin\\\":{\\\"login\\\":\\\"test@example.com\\\",\\\"password\\\":null,\\\"firstName\\\":\\\"Sergey\\\",\\\"lastName\\\":\\\"Lisovoy\\\"}}]}\\u0000\"]");
        return "233a1c4c-dde1-4766-9ca5-c0b21400c2b7";
    }

    public String addSecondActivatedTenant() {
        messagesQueue.add("a[\"MESSAGE\\nmessage-id:90c683e4-80a5-4b3f-be4d-2e8881162b2a\\nsubscription:9abb0688-6426-4c88-a535-64ba223e89d2\\n\\n" +
                "{\\\"type\\\":\\\"MODIFIED\\\",\\\"tenants\\\":[{\\\"externalId\\\":\\\"800eb78d-9878-4222-82e9-9bc1fde13196\\\",\\\"objectId\\\":\\\"4dd2d500-3ee3-40a8-bb79-5e535c533da1\\\",\\\"namespace\\\":null,\\\"status\\\":\\\"ACTIVE\\\",\\\"name\\\":\\\"user-tenant2\\\",\\\"domainName\\\":null,\\\"workbook\\\":null,\\\"admin\\\":{\\\"login\\\":\\\"test@example.com\\\",\\\"password\\\":null,\\\"firstName\\\":\\\"Sergey\\\",\\\"lastName\\\":\\\"Lisovoy\\\"}}]}\\u0000\"]");
        return "800eb78d-9878-4222-82e9-9bc1fde13196";
    }

    public void deleteFirstTenant() {
        messagesQueue.add("a[\"MESSAGE\\nmessage-id:f80afd14-d611-494e-bbc6-9429b63df12e\\nsubscription:9abb0688-6426-4c88-a535-64ba223e89d2\\n\\n" +
                "{\\\"type\\\":\\\"DELETED\\\",\\\"tenants\\\":[{\\\"externalId\\\":\\\"233a1c4c-dde1-4766-9ca5-c0b21400c2b7\\\",\\\"objectId\\\":\\\"57b5e723-3970-4cde-a5d1-cfee509eecd0\\\",\\\"namespace\\\":null,\\\"status\\\":\\\"DELETING\\\",\\\"name\\\":\\\"user-tenant2\\\",\\\"domainName\\\":null,\\\"workbook\\\":null,\\\"admin\\\":{\\\"login\\\":\\\"test@example.com\\\",\\\"password\\\":null,\\\"firstName\\\":\\\"Sergey\\\",\\\"lastName\\\":\\\"Lisovoy\\\"}}]}\\u0000\"]");
    }

    public void deactivateSecondTenant() {
        messagesQueue.add("a[\"MESSAGE\\nmessage-id:90c683e4-80a5-4b3f-be4d-2e8881162b2a\\nsubscription:9abb0688-6426-4c88-a535-64ba223e89d2\\n\\n" +
                "{\\\"type\\\":\\\"MODIFIED\\\",\\\"tenants\\\":[{\\\"externalId\\\":\\\"800eb78d-9878-4222-82e9-9bc1fde13196\\\",\\\"objectId\\\":\\\"4dd2d500-3ee3-40a8-bb79-5e535c533da1\\\",\\\"namespace\\\":null,\\\"status\\\":\\\"SUSPENDED\\\",\\\"name\\\":\\\"user-tenant2\\\",\\\"domainName\\\":null,\\\"workbook\\\":null,\\\"admin\\\":{\\\"login\\\":\\\"test@example.com\\\",\\\"password\\\":null,\\\"firstName\\\":\\\"Sergey\\\",\\\"lastName\\\":\\\"Lisovoy\\\"}}]}\\u0000\"]");
    }

    public void start() {
        messagesQueue.clear();
        messagesQueue.add("a[\"MESSAGE\\nsubscription:9abb0688-6426-4c88-a535-64ba223e89d2\\n\\n{\\\"type\\\":\\\"SUBSCRIBED\\\",\\\"tenants\\\":[{\\\"externalId\\\":\\\"9bcacf1e-20cb-4dd2-970e-404c354bcd8f\\\",\\\"objectId\\\":null,\\\"namespace\\\":null,\\\"status\\\":\\\"AWAITING_APPROVAL\\\",\\\"name\\\":\\\"user-tenant1\\\",\\\"domainName\\\":null,\\\"workbook\\\":null,\\\"admin\\\":{\\\"login\\\":\\\"test@example.com\\\",\\\"password\\\":null,\\\"firstName\\\":\\\"Sergey\\\",\\\"lastName\\\":\\\"Lisovoy\\\"}}]}\\u0000\"]");
        server.start();

        heartbeatTimer = new Timer(true);
        heartbeatTimer.scheduleAtFixedRate(new TimerTask() {
            @Override public void run() {
                messagesQueue.add("h");
            }
        }, 1000, 1000);
    }

    public void stop() {
        messagesQueue.add(QUIT_MESSAGE);
        heartbeatTimer.cancel();
        server.stop();
    }

    public String getUrl() {
        return "http://localhost:" + serverPort;
    }

}
