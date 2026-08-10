package com.netcracker.cloud.maas.client.impl.kafka;

import static java.net.HttpURLConnection.HTTP_CREATED;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import com.netcracker.cloud.maas.client.impl.ApiUrlProvider;
import com.netcracker.cloud.maas.client.impl.Lazy;
import com.netcracker.cloud.maas.client.impl.dto.kafka.v1.TopicDeleteRequest;
import com.netcracker.cloud.maas.client.impl.dto.kafka.v1.TopicDeleteResponse;
import com.netcracker.cloud.maas.client.impl.dto.kafka.v1.TopicInfo;
import com.netcracker.cloud.maas.client.impl.dto.kafka.v1.TopicRequest;
import com.netcracker.cloud.maas.client.impl.dto.kafka.v1.TopicTemplate;
import com.netcracker.cloud.maas.client.impl.http.HttpClient;
import com.netcracker.cloud.tenantmanager.client.TenantManagerConnector;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class KafkaMaaSClientImpl implements KafkaMaaSClient {
    private final HttpClient httpClient;
    private final Lazy<TenantManagerConnector> tenantManagerConnector;
    private final ApiUrlProvider apiProvider;

    private final Duration watchTimeout = Duration.ofSeconds(60);
    private static final Duration WATCH_RETRY_INTERVAL = Duration.ofSeconds(1);
    private static final Duration WATCH_MAX_RETRY_INTERVAL = Duration.ofSeconds(30);
    // there is no need in highly concurrent map/lists implementation, we will wait for network responses most of the time
    private final Map<Classifier, List<Consumer<TopicAddress>>> topicCreateListeners = Collections.synchronizedMap(new HashMap<>());
    private volatile boolean closed = false;
    /**
     * Monitor for parking the watch thread while there is nothing to watch.
     * <p>
     * Not the thread itself: {@link Thread#join()} waits on that same monitor and the JVM
     * notifies it when the thread ends, so a notification meant for the watch loop can be
     * consumed by a {@code join()} in {@link #close()} and the loop never wakes up.
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
        return httpClient.request(url)
                .post(TopicRequest.builder(classifier).build().options(options))
                .expect(HTTP_OK, HTTP_CREATED)
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
                .sendAndReceive(TopicDeleteResponse.class)
                .orElse(null);

        if (resp == null) {
            // empty body: nothing was reported as deleted
            return false;
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
        int failures = 0;
        TypeReference<List<TopicInfo>> typeRef = new TypeReference<>() {
        };
        while (!closed) {
            while (!closed && !topicCreateListeners.isEmpty()) {
                String url = apiProvider.getKafkaTopicWatchCreateUrl(watchTimeout);
                List<TopicInfo> found = Collections.emptyList();
                try {
                    found = httpClient.request(url)
                            .post(topicCreateListeners.keySet())
                            .expect(200)
                            .noRetry()
                            .sendAndReceive(typeRef)
                            .orElse(Collections.emptyList());
                    failures = 0;
                } catch (Exception e) {
                    // `closed` is checked too: an interrupt can be swallowed further down
                    if (closed) {
                        return; // shutting down, not a failure worth reporting
                    }
                    if (Thread.currentThread().isInterrupted()) {
                        log.warn("Watch thread interrupted without close(), stopping to watch {}", url, e);
                        return;
                    }
                    failures++;
                    log.warn("Error execute request to {}. Attempt {}, will back off before retrying", url, failures, e);
                    if (!sleepWatchBackoff(failures)) {
                        return; // interrupted while backing off
                    }
                    continue; // `found` is still empty, nothing to deliver
                }

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

            if (closed) {
                return;
            }

            try {
                log.info("Nothing to watch, sleep thread.");
                synchronized (watchLock) {
                    // guarded wait: a bare wait() would also return on a spurious wakeup
                    while (!closed && topicCreateListeners.isEmpty()) {
                        watchLock.wait();
                    }
                }
                log.info("Woke up!");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return; // exit loop
            }
        }
    }

    /**
     * Linear, capped backoff between failed watch polls, reset on every success.
     *
     * @return false if the thread was interrupted while waiting, meaning the caller should stop
     */
    private static boolean sleepWatchBackoff(int failures) {
        long delayMillis = Math.min(
                failures * WATCH_RETRY_INTERVAL.toMillis(),
                WATCH_MAX_RETRY_INTERVAL.toMillis());
        try {
            Thread.sleep(delayMillis);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    @Override
    public void watchTopicCreate(String name, Consumer<TopicAddress> callback) {
        if (closed) {
            // the watch thread has already exited and nothing restarts it, so the callback
            // would never fire
            throw new IllegalStateException("Client is closed, cannot watch topic: " + name);
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
                .get()
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
            watchThread.get().interrupt();
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
