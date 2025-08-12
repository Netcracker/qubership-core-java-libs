package com.netcracker.cloud.tenantmanager.client.impl;

import com.netcracker.cloud.maas.client.impl.Env;
import com.netcracker.cloud.maas.client.impl.http.HttpClient;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;
import okhttp3.WebSocket;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.netcracker.cloud.tenantmanager.client.impl.StompCommands.*;

// TODO there might be error in stomp negotiation sequence. So this code will wait connect/subscription ack indefinite time
@Slf4j
public class WebsocketAdapter implements AutoCloseable {
	private final BlockingDeque<StompFrame> responseQueue = new LinkedBlockingDeque<>();

	private final HttpClient httpClient;
	private final String tenantManagerUrl;

	private volatile WebSocket sock;
	private volatile WebsocketListener sockListener;
	private volatile boolean stopping = false;
	private volatile Optional<Timer> reconnectTimer = Optional.empty();

	WebsocketAdapter(String tenantManagerUrl, HttpClient httpClient) {
		this.tenantManagerUrl = tenantManagerUrl;
		this.httpClient = httpClient;
		establishConnection();
	}

	private void establishConnection() {
		String url = Env.url2ws(this.tenantManagerUrl) + "/api/v4/tenant-manager/watch/"
				+ new Random().nextInt(999)
				+ "/" + randomString(16)
				+ "/websocket";

		Request req = new Request.Builder()
				.url(url)
				.header("Host", Env.tenantManagerHost())
				.header("Schema", this.tenantManagerUrl)
				.build();

		log.info("Open connection to tenant-manager: " + req);

		sockListener = new WebsocketListener(
				this::connect,
				null,
				this::reconnect,
				this::scheduleReconnect
			);
		sock = httpClient.getClient().newWebSocket(req, sockListener);
	}

	private void scheduleReconnect() {
		if (stopping) return;

		log.error("Socket connection was failed. Start reconnection attempt after timeout: {}", Env.tenantManagerReconnectTimeout());
		release();

		Timer t = new Timer();
		t.schedule(new TimerTask() {
					@Override public void run() {
						establishConnection();
					}
				}, Env.tenantManagerReconnectTimeout());

		reconnectTimer = Optional.of(t);
	}

	private void reconnect() {
		if (stopping) return;

		log.warn("Socket was closed. Start reconnect sequence immediately...");
		release();
		establishConnection();
	}

	private void connect() {
		log.info("Socket connection established. Send CONNECT stomp command");
		StompFrame connectCommand = new StompFrame(CONNECT,
				StompFrame.headers("accept-version", "1.2,1.1,1.0",	"heart-beat", "10000,10000")
		);

		sockListener.setOnMessage(this::subscribe);
		sock.send(connectCommand.toString());
	}

	private void subscribe(StompFrame e) {
		if (e.getCommand() == CONNECTED) {
			log.info("Connection established. Session id: {}", e.getHeader("session"));
		} else {
			throw new RuntimeException("Unexpected error connecting to tenant-manager: " + e);
		}
		String subscriptionId = UUID.randomUUID().toString();
		log.info("Subscribe on tenant change events channel (subscriptionId: {})...", subscriptionId);
		StompFrame subscribeCmd = new StompFrame(SUBSCRIBE,
				StompFrame.headers("id", subscriptionId, "destination", "/channels/tenants")
		);

		sockListener.setOnMessage(responseQueue::add);
		sock.send(subscribeCmd.serialize());
	}

	@SneakyThrows
	public StompFrame awaitEvent(Duration timeout) {
		return responseQueue.poll(timeout.toMillis(), TimeUnit.MILLISECONDS);
	}

	private static String randomString(int len) {
		final String allowedChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
		Random rnd = new Random();
		return IntStream.range(0, len)
				.map(i -> rnd.nextInt(allowedChars.length() - 1))
				.mapToObj(i -> String.valueOf(allowedChars.charAt(i)))
				.collect(Collectors.joining());
	}

	private void release() {
		log.debug("Release websocket resources...");
		if (sock != null) {
			sock.cancel();
		}
		reconnectTimer.ifPresent(t -> t.cancel());
		responseQueue.clear();
	}

	@Override
	public void close() {
		stopping = true;
		release();
	}
}
