package com.netcracker.cloud.bluegreen.impl.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.netcracker.cloud.bluegreen.api.model.BlueGreenState;
import com.netcracker.cloud.bluegreen.api.model.NamespaceVersion;
import com.netcracker.cloud.bluegreen.api.model.State;
import com.netcracker.cloud.bluegreen.api.model.Version;
import com.netcracker.cloud.bluegreen.api.service.BlueGreenStatePublisher;
import com.netcracker.cloud.bluegreen.impl.dto.BGState;
import com.netcracker.cloud.bluegreen.impl.dto.NSVersion;
import com.netcracker.cloud.bluegreen.impl.dto.consul.KVInfo;
import com.netcracker.cloud.bluegreen.impl.http.HttpClientAdapter;
import com.netcracker.cloud.bluegreen.impl.http.ResponseHandler;
import com.netcracker.cloud.bluegreen.impl.http.StringResponseBodyHandler;
import com.netcracker.cloud.bluegreen.impl.http.error.DefaultErrorCodeException;
import com.netcracker.cloud.bluegreen.impl.http.error.ErrorCodeException;
import com.netcracker.cloud.bluegreen.impl.http.error.InvocationException;
import com.netcracker.cloud.bluegreen.impl.util.ConsulUtil;
import com.netcracker.cloud.bluegreen.impl.util.EnvUtil;
import com.netcracker.cloud.bluegreen.impl.util.JsonUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.event.Level;

import java.net.URI;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@Slf4j
public class ConsulBlueGreenStatePublisher implements BlueGreenStatePublisher, AutoCloseable {
    public static final String BG_STATE_CONSUL_PATH = "config/%s/application/bluegreen/bgstate"; // config/{namespace}/application/bluegreen/bgstate
    public static final Duration DEFAULT_POLLING_WAIT_TIME = Duration.ofMinutes(5);
    public static final OffsetDateTime UNKNOWN_DATETIME = OffsetDateTime.parse("1970-01-01T00:00:00Z");
    private static final String CONSUL_KV_PATH = "/v1/kv/";

    final WatcherTask watcherTask;
    final ScheduledExecutorService executorService;
    final AtomicBoolean cancelled;

    public ConsulBlueGreenStatePublisher(Supplier<String> consulTokenSupplier) {
        this(consulTokenSupplier, EnvUtil.getConsulUrl(), EnvUtil.getNamespace());
    }

    public ConsulBlueGreenStatePublisher(Supplier<String> consulTokenSupplier, String consulUrl, String namespace) {
        this(consulTokenSupplier, consulUrl, namespace, DEFAULT_POLLING_WAIT_TIME);
    }

    public ConsulBlueGreenStatePublisher(Supplier<String> consulTokenSupplier, String consulUrl, String namespace, Duration pollingInterval) {
        this(new HttpClientAdapter(consulTokenSupplier), consulUrl, namespace, pollingInterval);
    }

    public ConsulBlueGreenStatePublisher(HttpClientAdapter client, String consulUrl, String namespace, Duration pollingInterval) {
        this(client, consulUrl, namespace, pollingInterval, Duration.ofSeconds(30));
    }

    public ConsulBlueGreenStatePublisher(HttpClientAdapter client, String consulUrl, String namespace, Duration pollingInterval, Duration readyTimeout) {
        LockUtils.checkNotEmpty(Map.of(
                "client", client,
                "consulUrl", consulUrl,
                "namespace", namespace,
                "pollingInterval", pollingInterval));
        log.info("Starting ConsulBlueGreenStatePublisher in {}", String.format(BG_STATE_CONSUL_PATH, namespace));
        this.executorService = Executors.newScheduledThreadPool(1, r -> {
            Thread t = new Thread(r, "consul-bluegreen-state-publisher");
            t.setDaemon(true);
            return t;
        });
        this.cancelled = new AtomicBoolean(false);
        this.watcherTask = new WatcherTask(client, consulUrl, namespace, pollingInterval, executorService, cancelled);
        this.executorService.submit(watcherTask);
        if (!this.watcherTask.isReady(readyTimeout)) {
            this.close();
            throw new IllegalStateException("ConsulBlueGreenStatePublisher failed to get ready after " + readyTimeout,
                    this.watcherTask.lastError);
        }
        log.info("Started ConsulBlueGreenStatePublisher");
    }

    @Override
    public void subscribe(Consumer<BlueGreenState> subscriber) {
        log.debug("Subscribing '{}'", subscriber);
        watcherTask.subscribers.add(subscriber);
        watcherTask.notifyCallback(subscriber, watcherTask.blueGreenState.get());
    }

    @Override
    public void unsubscribe(Consumer<BlueGreenState> subscriber) {
        if (watcherTask.subscribers.remove(subscriber)) {
            log.debug("Unsubscribed '{}'", subscriber);
        }
    }

    @Override
    public BlueGreenState getBlueGreenState() {
        BlueGreenState state = watcherTask.blueGreenState.get();
        log.debug("Current blueGreenState = {}", state);
        return state;
    }

    @Override
    public void close() {
        log.info("Closing {} - cancelling watch loop and clearing subscribers", this.getClass().getSimpleName());
        this.cancelled.set(true);
        this.executorService.shutdownNow();
        this.watcherTask.subscribers.clear();
    }

    private static class WatcherTask implements Runnable {
        final HttpClientAdapter client;
        final String consulUrl;
        final String pollingWaitTime;
        String lastModifyIndex;
        AtomicReference<BlueGreenState> blueGreenState = new AtomicReference<>();
        final Set<Consumer<BlueGreenState>> subscribers;
        final String namespace;
        final CountDownLatch ready = new CountDownLatch(1);
        final ScheduledExecutorService executorService;
        final AtomicBoolean cancelled;
        Exception lastError;

        private WatcherTask(HttpClientAdapter client, String consulUrl, String namespace, Duration pollingWaitTime, ScheduledExecutorService executorService, final AtomicBoolean cancelled) {
            this.client = client;
            this.consulUrl = consulUrl;
            this.pollingWaitTime = ConsulUtil.toConsulTTL(pollingWaitTime);
            this.subscribers = Collections.synchronizedSet(new HashSet<>());
            this.namespace = namespace;
            this.lastModifyIndex = "0";
            this.executorService = executorService;
            this.cancelled = cancelled;
        }

        @Override
        public void run() {
            if (cancelled.get()) {
                log.info("WatcherTask was cancelled. It means the publisher is being cleaned up");
                return;
            }
            Duration retryDelay = Duration.ZERO; // by default, we retry immediately
            URI uri;
            if (Objects.equals(lastModifyIndex, "0")) {
                uri = URI.create(consulUrl + CONSUL_KV_PATH + String.format(BG_STATE_CONSUL_PATH, namespace));
            } else {
                uri = URI.create(consulUrl + CONSUL_KV_PATH + String.format(BG_STATE_CONSUL_PATH, namespace) + String.format("?index=%s&wait=%s", lastModifyIndex, pollingWaitTime));
            }
            try {
                log.debug("Sending request to Consul: {}", uri);
                ResponseHandler<List<KVInfo>> response = this.client.invoke(req -> req.uri(uri).header("Content-Type", "application/json").GET(), new TypeReference<List<KVInfo>>() {
                }).onError(StringResponseBodyHandler.INSTANCE, 404).send();
                BgStateAndIndex bgStateAndIndex = parseResponse(response);
                lastError = null;
                lastModifyIndex = bgStateAndIndex.index();
                Optional<BlueGreenState> state = bgStateAndIndex.state();
                log.debug("Received response from Consul: with modifyIndex={} and blueGreenState={}", lastModifyIndex, state);
                state.ifPresent(blueGreenState -> {
                    if (!Objects.equals(blueGreenState, this.blueGreenState.get())) {
                        this.blueGreenState.set(blueGreenState);
                        notifyCallbacks(blueGreenState);
                    }
                });
            } catch (ErrorCodeException e) {
                if (e.getHttpCode() == 404) {
                    lastModifyIndex = ConsulUtil.getModifyIndex(e.responseHandler);
                    blueGreenState.set(getDefaultBGState(namespace));
                    log.atLevel(ready.getCount() != 0 ? Level.INFO : Level.DEBUG)
                            .log("There is no BG state value in Consul (modifyIndex={}). Setting blueGreenState to the default value={}",
                                    lastModifyIndex, blueGreenState.get());
                } else {
                    lastError = e;
                    retryDelay = Duration.ofSeconds(5);
                    log.error("Error happened on long polling request to '{}', retrying after {}. Reason: {}", uri, retryDelay, e.getMessage());
                }
            } catch (DefaultErrorCodeException | InvocationException e) {
                if (e.getCause() instanceof InterruptedException) {
                    log.info("The thread was interrupted. It means the publisher is being cleaned up");
                    return;
                }
                lastError = e;
                retryDelay = Duration.ofSeconds(5);
                log.error("Error happened on long polling request to '{}', retrying after {}. Reason: {}", uri, retryDelay, e.getMessage());
            }
            if (this.blueGreenState.get() != null) {
                ready.countDown();
            }
            // schedule next long polling request
            log.debug("Scheduling {} with retryDelay= {}", this.getClass().getSimpleName(), retryDelay);
            this.executorService.schedule(this, retryDelay.toMillis(), TimeUnit.MILLISECONDS);
        }

        @SneakyThrows
        private boolean isReady(Duration timeout) {
            return ready.await(timeout.toSeconds(), TimeUnit.SECONDS);
        }

        private BlueGreenState getDefaultBGState(String namespace) {
            return new BlueGreenState(new NamespaceVersion(namespace, State.ACTIVE, new Version(1)), UNKNOWN_DATETIME);
        }

        private void notifyCallbacks(BlueGreenState state) {
            Set<Consumer<BlueGreenState>> subscribers = new HashSet<>(this.subscribers);
            log.info("Notifying {} subscriber(s) about new BlueGreenState: {}", subscribers.size(), state);
            subscribers.forEach(callback -> notifyCallback(callback, state));
        }

        private void notifyCallback(Consumer<BlueGreenState> subscriber, BlueGreenState state) {
            try {
                log.debug("Notifying subscriber '{}' about new BlueGreenState: {}", subscriber, state);
                subscriber.accept(state);
            } catch (Exception e) {
                log.error("Subscriber '{}' failed to process BlueGreenState: {}", subscriber, state, e);
            }
        }

        private BgStateAndIndex parseResponse(ResponseHandler<List<KVInfo>> response) {
            String modifyIndex = ConsulUtil.getModifyIndex(response);
            Optional<BlueGreenState> blueGreenState = Optional.ofNullable(response.getBody())
                    .map(l -> l.isEmpty() ? null : l.get(0))
                    .map(KVInfo::getValue)
                    .map(bgState -> JsonUtil.fromJson(bgState, BGState.class))
                    .map(bgState -> {
                        NSVersion originNSVersion = bgState.getOriginNamespace();
                        NSVersion peerNSVersion = bgState.getPeerNamespace();
                        String originNamespace = originNSVersion.getName();
                        String peerNamespace = Optional.ofNullable(peerNSVersion).map(NSVersion::getName).orElse(null);
                        NSVersion current;
                        Optional<NSVersion> sibling;
                        if (Objects.equals(namespace, originNamespace)) {
                            current = originNSVersion;
                            sibling = Optional.ofNullable(peerNSVersion);
                        } else if (Objects.equals(namespace, peerNamespace)) {
                            current = peerNSVersion;
                            sibling = Optional.of(originNSVersion);
                        } else {
                            throw new IllegalStateException(
                                    String.format("Invalid BlueGreen state response or namespace parameter. " +
                                                    "'namespace' param = '%s' does not match neither originNamespace '%s' nor peerNamespace '%s'",
                                            namespace, originNamespace, peerNamespace));
                        }
                        Function<NSVersion, NamespaceVersion> convertNamespaceFunc = nsVersion ->
                                new NamespaceVersion(nsVersion.getName(), State.valueOf(nsVersion.getState().toUpperCase()), new Version(nsVersion.getVersion()));
                        NamespaceVersion currentNamespaceVersion = convertNamespaceFunc.apply(current);
                        Optional<NamespaceVersion> siblingNamespaceVersion = sibling.map(convertNamespaceFunc);
                        return new BlueGreenState(currentNamespaceVersion, siblingNamespaceVersion, bgState.getUpdateTime());
                    });
            return new BgStateAndIndex(modifyIndex, blueGreenState);
        }
    }

    record BgStateAndIndex(String index, Optional<BlueGreenState> state) {
    }
}
