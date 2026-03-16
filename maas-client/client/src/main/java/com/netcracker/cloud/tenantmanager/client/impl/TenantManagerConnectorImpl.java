package com.netcracker.cloud.tenantmanager.client.impl;

import com.netcracker.cloud.maas.client.impl.Env;
import com.netcracker.cloud.maas.client.impl.http.HttpClient;
import com.netcracker.cloud.tenantmanager.client.Tenant;
import com.netcracker.cloud.tenantmanager.client.TenantManagerConnector;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static com.netcracker.cloud.tenantmanager.client.Tenant.STATE_ACTIVE;
import static com.netcracker.cloud.tenantmanager.client.impl.StompCommands.MESSAGE;

// TODO externalize to separate library?
// TODO dummy sync implementation: no retries, no timeout management and so on ...
@Slf4j
public class TenantManagerConnectorImpl implements TenantManagerConnector {
	private final List<Consumer<List<Tenant>>> listeners = new CopyOnWriteArrayList<>();
	private final AtomicReference<List<Tenant>> tenants = new AtomicReference<>(new ArrayList<>());

	private final String url;
	private final HttpClient httpClient;

	private final AtomicBoolean stopping = new AtomicBoolean(false);
	private final Thread processingThread = new Thread(() -> stateLoop(), "tm-websock-events-proc");


	public TenantManagerConnectorImpl(HttpClient httpClient) {
		this(Env.tenantManagerUrl(), httpClient);
	}

	public TenantManagerConnectorImpl(String url, HttpClient httpClient) {
		this.url = url;
		this.httpClient = httpClient;

		processingThread.setDaemon(true);
		processingThread.start();
	}

	@Override
	public List<Tenant> getTenantList() {
		return tenants.get();
	}

	@Override
	public void subscribe(Consumer<List<Tenant>> callback) {
		listeners.add(callback);
		// there is possible phantom subsequent call from event thread, then here
		callback.accept(tenants.get());
	}

	public boolean unsubscribe(Consumer<List<Tenant>> callback) {
		return listeners.remove(callback);
	}

	// main state machine loop
	private void stateLoop() {
		WebsocketAdapter websocketAdapter = null;
		while(!stopping.get()) {
			if (websocketAdapter == null) {
				log.info("Create tenant-manager websocket manger...");
				websocketAdapter = new WebsocketAdapter(url, httpClient);
			}

			StompFrame frame = websocketAdapter.awaitEvent(Duration.ofMillis(100));
			if (frame == null) {
				continue; // no messages
			}
			List<Tenant> current = tenants.get();
			List<Tenant> changed = mergeUpdate(current, frame);
			if (!current.equals(changed)) {
				log.debug("Tenant list changed.");
				tenants.set(changed);
				executeCallbacks(changed);
			} else {
				log.debug("Tenant list stay intact");
			}
		}

		if (websocketAdapter != null) {
			websocketAdapter.close();
		}

		log.debug("Exit event processing loop");
	}

	private void executeCallbacks(List<Tenant> changed) {
		// execute callbacks
		listeners.stream().forEach(c -> {
			log.info("Call event listener: {}", c);
			try {
				c.accept(changed);
			} catch (Exception e) {
				log.error("Error executing callback: {}. Error: {}", c, e);
			}
		});
	}

	private List<Tenant> mergeUpdate(List<Tenant> current, StompFrame frame) {
		log.debug("Merge message: {}", frame);
		if (frame.getCommand() != MESSAGE) {
			throw new IllegalStateException("Unexpected frame from tenant-manager: " + frame);
		}

		// copy current tenant list and perform changes on it
		List<Tenant> changed = new ArrayList<>(current);
		TenantWatchEvent event = TenantWatchEvent.deserialize(frame.getBody());
		// filter only tenants in ACTIVE state
		switch(event.getType()) {
			case SUBSCRIBED:
				log.info("Subscription successful. Tenant list received: {}", event.getTenants());
				// setup initial tenants list
				event.getTenants()
						.stream()
						.filter(t -> STATE_ACTIVE.equals(t.getStatus()))
						.forEach(t -> changed.add(t));
				break;
			case CREATED:
				// ignore this events. we are interested only in ACTIVE tenants
				break;
			case MODIFIED:
				log.info("Modify tenants: {}", event.getTenants());
				// remove tenants that change its state to non-ACTIVE
				event.getTenants()
						.stream()
						.filter(t -> !STATE_ACTIVE.equals(t.getStatus()))
						.map(t -> t.withStatus(STATE_ACTIVE))
						.forEach(t -> changed.remove(t));

				// add tenants, that change it's state to ACTIVE
				event.getTenants()
							.stream()
							.filter(t -> STATE_ACTIVE.equals(t.getStatus()))
							.forEach(t -> changed.add(t));
				break;
			case DELETED:
				log.info("Delete tenants: {}", event.getTenants());
				event.getTenants()
						.stream()
						.filter(t -> !STATE_ACTIVE.equals(t.getStatus()))
						.map(t -> t.withStatus(STATE_ACTIVE))
						.forEach(t -> changed.remove(t));
				break;
			default:
				throw new IllegalArgumentException("Unsupported event type: " + event.getType());
		}
		return changed;
	}

	@SneakyThrows
	public void close() {
		log.info("Stop tenant-manager connector");
		stopping.set(true);
		processingThread.join();
	}
}
