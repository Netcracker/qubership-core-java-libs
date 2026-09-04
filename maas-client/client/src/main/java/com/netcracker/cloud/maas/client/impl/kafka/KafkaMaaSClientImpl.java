package com.netcracker.cloud.maas.client.impl.kafka;

import static java.net.HttpURLConnection.HTTP_CREATED;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.netcracker.cloud.maas.client.api.Classifier;
import com.netcracker.cloud.maas.client.api.MaaSException;
import com.netcracker.cloud.maas.client.api.kafka.KafkaMaaSClient;
import com.netcracker.cloud.maas.client.api.kafka.SearchCriteria;
import com.netcracker.cloud.maas.client.api.kafka.TopicAddress;
import com.netcracker.cloud.maas.client.api.kafka.TopicCreateOptions;
import com.netcracker.cloud.maas.client.api.kafka.protocolextractors.OnTopicExists;
import com.netcracker.cloud.maas.client.impl.ApiUrlProvider;
import com.netcracker.cloud.maas.client.impl.Env;
import com.netcracker.cloud.maas.client.impl.Lazy;
import com.netcracker.cloud.maas.client.impl.dto.kafka.v1.TopicDeleteRequest;
import com.netcracker.cloud.maas.client.impl.dto.kafka.v1.TopicDeleteResponse;
import com.netcracker.cloud.maas.client.impl.dto.kafka.v1.TopicInfo;
import com.netcracker.cloud.maas.client.impl.dto.kafka.v1.TopicRequest;
import com.netcracker.cloud.maas.client.impl.dto.kafka.v1.TopicTemplate;
import com.netcracker.cloud.maas.client.impl.http.HttpClient;
import com.netcracker.cloud.maas.client.impl.http.HttpExecution;
import com.netcracker.cloud.tenantmanager.client.TenantManagerConnector;

import dev.failsafe.Failsafe;
import dev.failsafe.FailsafeException;
import dev.failsafe.RetryPolicy;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class KafkaMaaSClientImpl implements KafkaMaaSClient {
    private final HttpClient httpClient;
    private final Lazy<TenantManagerConnector> tenantManagerConnector;
    private final ApiUrlProvider apiProvider;

    /**
     * How long maas-service is asked to hold a watch poll open. Stays below the read timeout,
     * otherwise a quiet poll dies locally instead of returning an empty 200.
     */
    private final Duration watchTimeout = watchTimeout(Env.httpTimeout());

    /** Largest gap left between the watch window and the read timeout. */
    private static final long MAX_WATCH_MARGIN_SECONDS = 5;

    /**
     * The margin is clamped, so a small read timeout narrows the window instead of inverting it.
     * The window travels in whole seconds, so {@code maas.http.timeout} below 2s is unsupported.
     */
    static Duration watchTimeout(Duration httpTimeout) {
        long timeoutSeconds = httpTimeout.getSeconds();
        long marginSeconds = Math.min(MAX_WATCH_MARGIN_SECONDS, timeoutSeconds / 2);
        return Duration.ofSeconds(Math.max(1, timeoutSeconds - marginSeconds));
    }

    private static final Duration WATCH_RETRY_INTERVAL = Duration.ofSeconds(1);
    private static final Duration WATCH_MAX_RETRY_INTERVAL = Duration.ofSeconds(30);
    private static final double WATCH_BACKOFF_MULTIPLIER = 2.0;

    /** Keeps instances that lost the same agent from returning to it at the same moment. */
    private static final double WATCH_BACKOFF_JITTER = 0.2;
    // there is no need in highly concurrent map/lists implementation, we will wait for network responses most of the time
    private final Map<Classifier, List<Consumer<TopicAddress>>> topicCreateListeners = Collections.synchronizedMap(new HashMap<>());
    private volatile boolean closed = false;

    /**
     * Set when the watch thread exits without {@link #close()}. Nothing restarts it, so a later
     * {@link #watchTopicCreate} must refuse rather than register a callback that cannot fire.
     */
    private volatile boolean watchThreadDead = false;

    /**
     * Monitor for parking the watch thread. Not the thread itself: {@link Thread#join()} waits on
     * that monitor too, and would steal the notification meant for the loop.
     */
    private final Object watchLock = new Object();
    private final Lazy<Thread> watchThread = new Lazy<>(() -> {
        Thread exec = new Thread(this::watchTenantCreateTopics, "watchTopicCreate");
        exec.setDaemon(true);
        exec.start();
        return exec;
    });

    public KafkaMaaSClientImpl(HttpClient httpClient, Supplier<TenantManagerConnector> tmConn, ApiUrlProvider apiProvider) {
        this.httpClient = httpClient;
        this.tenantManagerConnector = new Lazy<>(tmConn);
        this.apiProvider = apiProvider;
    }

    @Override
    public TopicAddress getOrCreateTopic(Classifier classifier, TopicCreateOptions options) {
        if (options.getMinNumPartitions() != 0) {
            apiProvider.getServerApiVersion().requiresApiVersion(2, 8);
        }
        if (options.isVersioned()) {
            apiProvider.getServerApiVersion().requiresApiVersion(2, 14);
        }

        String url = apiProvider.getKafkaTopicUrl(options.getOnTopicExists());

        log.info("Get or create topic by classifier=`{}' and options=`{}'", classifier, options);
        HttpExecution request = httpClient.request(url)
                .post(TopicRequest.builder(classifier).build().options(options))
                .expect(HTTP_OK, HTTP_CREATED);
        if (options.getOnTopicExists() == OnTopicExists.FAIL) {
            request.noRetry();
        }
        return request
                .sendAndReceive(TopicInfo.class)
                .map(TopicAddressImpl::new)
                .get();
    }

    @Override
    public Optional<TopicAddress> getTopic(Classifier classifier) {
        return Optional.ofNullable(searchTopic(classifier));
    }

    @Override
    public boolean deleteTopic(Classifier classifier) {
        TopicDeleteResponse resp = httpClient.request(apiProvider.getKafkaTopicUrl(null))
                .delete(new TopicDeleteRequest(classifier))
                .expect(HTTP_OK)
                .noRetry()
                .sendAndReceive(TopicDeleteResponse.class)
                .orElse(null);

        if (resp == null) {
            return false; // empty body
        }
        if (!resp.getFailedToDelete().isEmpty()) {
            throw new MaaSException("Error delete topic by classifier: %s. Error: %s", classifier, resp.getFailedToDelete().get(0).getMessage());
        }

        return resp.getDeletedSuccessfully().size() == 1;
    }

    @Override
    public void watchTenantTopics(String name, Consumer<List<TopicAddress>> callback) {
        tenantManagerConnector.get().subscribe(tenantList -> {
            log.info("Tenant list changed. Select topics by externalId");
            List<TopicAddress> topics = tenantList.stream()
                    .map(tenant -> getTopic(new Classifier(name).tenantId(tenant.getExternalId())))
                    .flatMap(Optional::stream)
                    .toList();
            callback.accept(topics);
        });
    }

    private void watchTenantCreateTopics() {
        try {
            while (!closed) {
                if (!pollWhileThereIsSomethingToWatch()) {
                    return;
                }
                if (closed || !parkUntilThereIsSomethingToWatch()) {
                    return;
                }
            }
        } finally {
            if (!closed) {
                // exiting without close(): let watchTopicCreate refuse further registrations
                watchThreadDead = true;
            }
        }
    }

    /**
     * Polls the watch endpoint until nothing is being watched any more.
     *
     * @return false if the thread must stop
     */
    private boolean pollWhileThereIsSomethingToWatch() {
        while (!closed && !topicCreateListeners.isEmpty()) {
            String url = apiProvider.getKafkaTopicWatchCreateUrl(watchTimeout);
            List<TopicInfo> found;
            try {
                found = Failsafe.with(watchRetryPolicy(url)).get(() -> poll(url));
            } catch (Exception e) {
                Throwable cause = e instanceof FailsafeException failsafe ? failsafe.getCause() : e;
                if (cause instanceof InterruptedException) {
                    // Failsafe clears the flag when it catches this, so the check below cannot see it
                    Thread.currentThread().interrupt();
                }
                // `closed` is checked too: an interrupt can be swallowed further down
                if (closed) {
                    return false; // shutting down, not a failure worth reporting
                }
                if (Thread.currentThread().isInterrupted()) {
                    log.error("Watch thread interrupted without close(). Topic create callbacks for {} "
                            + "will no longer fire; recreate the client to resume watching",
                            watchedNames(), cause);
                    return false;
                }
                // the policy only gives up on interrupt, so this should be unreachable
                log.error("Watch poll for {} stopped unexpectedly. Topic create callbacks for {} "
                        + "will no longer fire; recreate the client to resume watching",
                        url, watchedNames(), cause);
                return false;
            }
            deliver(found);
        }
        return true;
    }

    /**
     * Backoff between failed watch polls. Unbounded in attempts, because the subscription lives as
     * long as the client; a fresh policy per poll resets the sequence after every success, and the
     * interrupt from {@link #close()} ends the wait.
     */
    private RetryPolicy<List<TopicInfo>> watchRetryPolicy(String url) {
        return RetryPolicy.<List<TopicInfo>>builder()
                .handle(Exception.class)
                .withBackoff(WATCH_RETRY_INTERVAL, WATCH_MAX_RETRY_INTERVAL, WATCH_BACKOFF_MULTIPLIER)
                .withJitter(WATCH_BACKOFF_JITTER)
                .withMaxAttempts(-1)
                .onRetry(event -> log.warn("Error execute request to {}. Attempt {} failed with {}, will back off before retrying",
                        url, event.getAttemptCount(), event.getLastException()))
                .build();
    }

    private static final TypeReference<List<TopicInfo>> TOPIC_LIST = new TypeReference<>() {
    };

    private Set<Classifier> watchedClassifiers() {
        synchronized (topicCreateListeners) {
            return new HashSet<>(topicCreateListeners.keySet());
        }
    }

    /** Names only: a classifier is an open map and may carry whatever the caller put in it. */
    private List<String> watchedNames() {
        return watchedClassifiers().stream().map(Classifier::getName).toList();
    }

    /** One long poll for topics created since the previous call. */
    private List<TopicInfo> poll(String url) {
        return httpClient.request(url)
                .post(watchedClassifiers())
                .expect(200)
                .noRetry()
                .sendAndReceive(TOPIC_LIST)
                .orElse(Collections.emptyList());
    }

    /** Hands each created topic to the callbacks registered for it, removing them as it goes. */
    private void deliver(List<TopicInfo> found) {
        for (TopicInfo addr : found) {
            List<Consumer<TopicAddress>> callbacks = topicCreateListeners.remove(addr.getClassifier());
            if (callbacks == null) {
                // this is unexpected situation in theory, but with this, code will be a little safer
                continue;
            }
            for (Consumer<TopicAddress> callback : callbacks) {
                try {
                    log.info("Topic create event for {} received, execute callback {}", addr.getClassifier(), callback);
                    callback.accept(new TopicAddressImpl(addr));
                } catch (Exception e) {
                    log.error("Error execute callback {}", callback, e);
                }
            }
        }
    }

    /**
     * Parks the thread while no topic is being watched.
     *
     * @return false if the thread was interrupted and must stop
     */
    private boolean parkUntilThereIsSomethingToWatch() {
        try {
            log.info("Nothing to watch, sleep thread.");
            synchronized (watchLock) {
                while (!closed && topicCreateListeners.isEmpty()) {
                    watchLock.wait();
                }
            }
            log.info("Woke up!");
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    @Override
    public void watchTopicCreate(String name, Consumer<TopicAddress> callback) {
        if (closed) {
            throw new IllegalStateException("Client is closed, cannot watch topic: " + name);
        }
        if (watchThreadDead) {
            throw new IllegalStateException("Watch thread has stopped unexpectedly, cannot watch topic: "
                    + name + "; recreate the client to resume watching");
        }
        apiProvider.getServerApiVersion().requiresApiVersion(2, 8);

        log.info("Add watch for topic by: {}, callback: {}", name, callback);
        topicCreateListeners.computeIfAbsent(new Classifier(name), k -> Collections.synchronizedList(new ArrayList<>())).add(callback);
        watchThread.get(); // start the thread if this is the first watch
        synchronized (watchLock) {
            watchLock.notifyAll();
        }
    }

    @Override
    public TopicAddress getOrCreateLazyTopic(String name) {
        log.info("Get lazy topic by: {}", name);
        return getOrCreateLazyTopic(new Classifier(name));
    }

    public TopicAddress getOrCreateLazyTopic(String name, String tenantId) {
        log.info("Get lazy tenant topic by: {}", name);
        return getOrCreateLazyTopic(new Classifier(name).tenantId(tenantId));
    }

    private TopicAddressImpl getOrCreateLazyTopic(Classifier classifier) {
        log.info("Request lazy topic by classifier=`{}'", classifier);
        return httpClient.request(apiProvider.getKafkaLazyTopicUrl())
                .post(classifier)
                .expect(HTTP_OK, HTTP_CREATED)
                .sendAndReceive(TopicInfo.class)
                .map(TopicAddressImpl::new)
                .orElse(null);
    }

    private TopicAddressImpl searchTopic(Classifier classifier) {
        log.info("Search topic by classifier: {}", classifier);
        return httpClient.request(apiProvider.getKafkaTopicGetByClassifierUrl())
                .post(classifier)
                .expect(HTTP_OK)
                .supressError(HTTP_NOT_FOUND, body -> log.info("Topic not found by {}", classifier))
                .sendAndReceive(TopicInfo.class)
                .map(TopicAddressImpl::new)
                .orElse(null);
    }

    public TopicTemplate deleteTopicTemplate(String name) {
        log.info("Delete topic template by name: {}", name);
        return httpClient.request(apiProvider.getKafkaTopicTemplateUrl())
                .delete(TopicTemplate.builder().name(name).build())
                .expect(HTTP_OK)
                .sendAndReceive(TopicTemplate.class)
                .orElse(null);
    }

    @Override
    public List<TopicAddress> search(SearchCriteria criteria) {
        log.info("Search for topics by criteria: {}", criteria);
        TypeReference<List<TopicInfo>> typeRef = new TypeReference<>(){};
        return httpClient.request(apiProvider.getKafkaTopicSearchUrl())
                .post(criteria)
                .expect(HTTP_OK)
                .sendAndReceive(typeRef)
                .orElseGet(Collections::emptyList)
                .stream()
                .map(TopicAddressImpl::new)
                .collect(Collectors.toList());
    }

    @Override
    public void close() {
        closed = true;
        synchronized (watchLock) {
            watchLock.notifyAll(); // release the watch thread if it is parked
        }
        if (watchThread.isInitialized()) {
            watchThread.get().interrupt(); // also cuts a backoff sleep short
            try {
                watchThread.get().join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        if (tenantManagerConnector.isInitialized()) {
            try {
                tenantManagerConnector.get().close();
            } catch (Exception e) {
                log.error("Error closing tenant manager connector", e);
            }
        }
    }
}
