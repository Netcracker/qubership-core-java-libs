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
    // there is no need in highly concurrent map/lists implementation, we will wait for network responses most of the time
    private final Map<Classifier, List<Consumer<TopicAddress>>> topicCreateListeners = Collections.synchronizedMap(new HashMap<>());
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

        if (resp != null && !resp.getFailedToDelete().isEmpty()) {
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
                    .collect(Collectors.toList());
            callback.accept(topics);
        });
    }

    private void watchTenantCreateTopics() {
        TypeReference<List<TopicInfo>> typeRef = new TypeReference<>() {
        };
        while (true) {
            while (!topicCreateListeners.isEmpty()) {
                String url = apiProvider.getKafkaTopicWatchCreateUrl(watchTimeout);
                List<TopicInfo> found = Collections.emptyList();
                try {
                    found = httpClient.request(url)
                            .post(topicCreateListeners.keySet())
                            .expect(200)
                            .sendAndReceive(typeRef)
                            .orElse(Collections.emptyList());
                } catch (Exception e) {
                    log.error("Error execute request to {}", url, e);
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

            try {
                log.info("Nothing to watch, sleep thread.");
                synchronized (watchThread.get()) {
                    watchThread.get().wait();
                }
                log.info("Woke up!");
            } catch (InterruptedException e) {
                return; // exit loop
            }
        }
    }

    @Override
    public void watchTopicCreate(String name, Consumer<TopicAddress> callback) {
        apiProvider.getServerApiVersion().requiresApiVersion(2, 8);

        log.info("Add watch for topic by: {}, callback: {}", name, callback);
        topicCreateListeners.computeIfAbsent(new Classifier(name), k -> Collections.synchronizedList(new ArrayList<>())).add(callback);
        synchronized (watchThread.get()) {
            watchThread.get().notify();
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
}
