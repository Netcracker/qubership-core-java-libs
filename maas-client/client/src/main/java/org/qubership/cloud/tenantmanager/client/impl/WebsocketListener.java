package org.qubership.cloud.tenantmanager.client.impl;

import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

import java.util.function.Consumer;

@Slf4j
class WebsocketListener extends WebSocketListener {
	private final Runnable onOpen;
	private volatile Consumer<StompFrame> onMessage;
	private final Runnable onClose;
	private final Runnable onFailure;

	WebsocketListener(Runnable onOpen, Consumer<StompFrame> onMessage, Runnable onClose, Runnable onFailure) {
		this.onOpen = onOpen;
		this.onMessage = onMessage;
		this.onClose = onClose;
		this.onFailure = onFailure;
	}

	public void setOnMessage(Consumer<StompFrame> onMessage) {
		this.onMessage = onMessage;
	}

	@Override
	public void onOpen(WebSocket webSocket, Response response) {
		log.info("Socket opened...");
		onOpen.run();
	}

	@Override
	public void onMessage(WebSocket webSocket, String text) {
		log.debug("Received text message: {}", text);
		StompFrameContainer c = StompFrameContainer.deserialize(text);
		switch(c.getOperation()) {
			case "a":
				onMessage.accept(c.getFrame());
				break;
			case "o":
				// unknown operation, but it's okay
				break;
			case "h":
				log.debug("Heartbeat message received");
				break;
			case "c":
				log.info("Close command received");
				onClose.run();
				return;
			default:
				log.warn("Unknown operation: {}", c);
		}
	}

	@Override
	public void onFailure(WebSocket webSocket, Throwable t, Response response) {
		log.error("Error: " + response, t);
		onFailure.run();
	}

	@Override
	public void onClosing(WebSocket webSocket, int code, String reason) {
		log.info("Close socket by code: {}, reaason: {}", reason);
		onClose.run();
	}
}

