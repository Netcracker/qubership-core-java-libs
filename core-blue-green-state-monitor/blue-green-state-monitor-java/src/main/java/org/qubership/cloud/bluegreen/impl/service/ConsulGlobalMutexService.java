package org.qubership.cloud.bluegreen.impl.service;

import com.fasterxml.jackson.core.type.TypeReference;
import org.qubership.cloud.bluegreen.api.error.LockFailedException;
import org.qubership.cloud.bluegreen.api.model.LockAction;
import org.qubership.cloud.bluegreen.api.model.MicroserviceLockInfo;
import org.qubership.cloud.bluegreen.api.service.GlobalMutexService;
import org.qubership.cloud.bluegreen.impl.dto.consul.*;
import org.qubership.cloud.bluegreen.impl.http.HttpClientAdapter;
import org.qubership.cloud.bluegreen.impl.http.ObjectMapperPublisher;
import org.qubership.cloud.bluegreen.impl.http.ResponseHandler;
import org.qubership.cloud.bluegreen.impl.http.StringResponseBodyHandler;
import org.qubership.cloud.bluegreen.impl.http.error.DefaultErrorCodeException;
import org.qubership.cloud.bluegreen.impl.http.error.ErrorCodeException;
import org.qubership.cloud.bluegreen.impl.http.error.InvocationException;
import org.qubership.cloud.bluegreen.impl.util.ConsulUtil;
import org.qubership.cloud.bluegreen.impl.util.EnvUtil;
import org.qubership.cloud.bluegreen.impl.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Deprecated(forRemoval = true)
public class ConsulGlobalMutexService implements GlobalMutexService {
    public static final String GLOBAL = "GLOBAL";
    public static final String ALL_MS_LOCKS = "ALL MS LOCKS";
    public static final String GLOBAL_MUTEX_CONSUL_PATH_TEMPLATE = "config/%s/application/bluegreen/global-mutex"; //config/{namespace}/application/bluegreen/global-mutex
    public static final String MODIFY_MUTEX_CONSUL_PATH_TEMPLATE = "data/%s/application/bluegreen/modify-mutex"; //data/{namespace}/application/bluegreen/modify-mutex
    public static final String NAMESPACE_CONFIG_PATH_TEMPLATE = "config/%s"; //config/{namespace}
    private static final String CONSUL_KV_PATH = "/v1/kv/";

    public static Pattern MS_MUTEX_PATH_PATTERN = Pattern.compile("config/(?<namespace>([^/]+))/(?<microservice>([^/]+))/bluegreen/mutex/(?<name>([^/]+))/(?<pod>([^/]+))");

    final HttpClientAdapter client;
    final String consulUrl;

    public ConsulGlobalMutexService(Supplier<String> consulTokenSupplier) {
        this(new HttpClientAdapter(consulTokenSupplier), EnvUtil.getConsulUrl());
    }

    public ConsulGlobalMutexService(Supplier<String> consulTokenSupplier, String consulUrl) {
        this(new HttpClientAdapter(consulTokenSupplier), consulUrl);
    }

    public ConsulGlobalMutexService(HttpClientAdapter client, String consulUrl) {
        LockUtils.checkNotEmpty(Map.of(
                "client", client,
                "consulUrl", consulUrl));
        this.client = client;
        this.consulUrl = consulUrl;
    }

    @Override
    public boolean tryLock(Duration timeout, List<String> namespaces) throws LockFailedException {
        return this.tryLock(timeout, namespaces, false);
    }

    @Override
    public boolean forceLock(List<String> namespaces) throws LockFailedException {
        return this.tryLock(Duration.ZERO, namespaces, true);
    }

    @Override
    public void unlock(List<String> namespaces) throws LockFailedException {
        LockUtils.checkNamespaces(namespaces);
        log.debug("Trying to remove Global lock in namespaces '{}'", namespaces);
        try {
            List<TxKVOperationWrapper> transaction = namespaces.stream().flatMap(namespace -> Stream.of(
                            new TxKVOperationWrapper(TxKVOperation.builder()
                                    .verb(TxnKVVerb.SET)
                                    .key(String.format(MODIFY_MUTEX_CONSUL_PATH_TEMPLATE, namespace))
                                    .value(JsonUtil.toJson(new ModifyLockData(LockAction.DELETE, namespace, GLOBAL)))
                                    .build()),
                            new TxKVOperationWrapper(TxKVOperation.builder()
                                    .verb(TxnKVVerb.DELETE)
                                    .key(String.format(GLOBAL_MUTEX_CONSUL_PATH_TEMPLATE, namespace))
                                    .build())
                    )
            ).toList();
            this.client.invoke(req -> req.uri(URI.create(consulUrl + "/v1/txn"))
                    .header("Content-Type", "application/json")
                    .PUT(new ObjectMapperPublisher(transaction)), Session.class).sendAndGet();
        } catch (DefaultErrorCodeException | InvocationException e) {
            throw new LockFailedException(LockAction.DELETE, namespaces, GLOBAL, e);
        }
    }

    @Override
    public boolean isLocked(List<String> namespaces) throws LockFailedException {
        LockUtils.checkNamespaces(namespaces);
        log.debug("Checking if Global lock is set for namespaces: {}", namespaces);
        boolean locked = namespaces.stream().allMatch(namespace -> {
            try {
                return !this.client.invoke(req ->
                                req.uri(URI.create(consulUrl + CONSUL_KV_PATH + String.format(GLOBAL_MUTEX_CONSUL_PATH_TEMPLATE, namespace)))
                                        .header("Content-Type", "application/json")
                                        .GET(), new TypeReference<List<KVInfo>>() {
                        })
                        .onError(StringResponseBodyHandler.INSTANCE, 404)
                        .sendAndGet().isEmpty();
            } catch (ErrorCodeException e) {
                if (e.getHttpCode() == 404) {
                    log.debug("Global lock is not set for namespaces: {}", namespaces);
                    return false;
                } else {
                    throw new LockFailedException(LockAction.READ, List.of(namespace), GLOBAL, e);
                }
            } catch (DefaultErrorCodeException | InvocationException e) {
                throw new LockFailedException(LockAction.READ, List.of(namespace), GLOBAL, e);
            }
        });
        log.debug("Global lock is {} for namespaces: {}", locked ? "set" : "not set", namespaces);
        return locked;
    }

    @Override
    public List<MicroserviceLockInfo> getMicroserviceLocks(List<String> namespaces) throws LockFailedException {
        LockUtils.checkNamespaces(namespaces);
        return namespaces.stream().flatMap(namespace -> getMsLocksList(namespace).list().stream()).toList();
    }

    private boolean tryLock(Duration timeout, List<String> namespaces, boolean force) throws LockFailedException {
        LockUtils.checkLockTimeout(timeout);
        LockUtils.checkNamespaces(namespaces);
        log.info("Trying to {} Global lock in namespaces '{}'", force ? "force set" : "set", namespaces);
        Instant start = Instant.now();
        do {
            List<TxKVOperationWrapper> tx;
            if (force) {
                tx = namespaces.stream().flatMap(namespace ->
                        Stream.of(new TxKVOperationWrapper(TxKVOperation.builder()
                                        .verb(TxnKVVerb.SET)
                                        .key(String.format(GLOBAL_MUTEX_CONSUL_PATH_TEMPLATE, namespace))
                                        .value("true")
                                        .build()),
                                new TxKVOperationWrapper(TxKVOperation.builder()
                                        .verb(TxnKVVerb.SET)
                                        .key(String.format(MODIFY_MUTEX_CONSUL_PATH_TEMPLATE, namespace))
                                        .value(JsonUtil.toJson(new ModifyLockData(LockAction.CREATE, namespace, GLOBAL)))
                                        .build())
                        )).toList();
            } else {
                // start long polling to watch for changes in MS locks
                List<ModifyMutexIndex> modifyMutexList = longPollOnMsLocks(timeout, namespaces, start);
                if (modifyMutexList.size() != namespaces.size() || !modifyMutexList.stream().allMatch(ModifyMutexIndex::noMSLocks)) {
                    continue;
                }
                // set global lock in all BlueGreen namespaces in the single transaction
                tx = modifyMutexList.stream().flatMap(modifyMutexIndex ->
                        Stream.of(new TxKVOperationWrapper(TxKVOperation.builder()
                                        .verb(TxnKVVerb.CHECK_INDEX)
                                        .key(String.format(MODIFY_MUTEX_CONSUL_PATH_TEMPLATE, modifyMutexIndex.namespace))
                                        .index(modifyMutexIndex.modifyIndex)
                                        .build()),
                                new TxKVOperationWrapper(TxKVOperation.builder()
                                        .verb(TxnKVVerb.SET)
                                        .key(String.format(GLOBAL_MUTEX_CONSUL_PATH_TEMPLATE, modifyMutexIndex.namespace))
                                        .value("true")
                                        .build()))
                ).toList();
            }
            if (log.isDebugEnabled()) {
                log.debug("Trying to set Global lock in namespaces '{}' with transaction = \n{}", namespaces,
                        tx.stream().map(TxKVOperationWrapper::toString).collect(Collectors.joining("\n")));
            }
            ResponseHandler<String> error409handler = new ResponseHandler<>(String.class);
            try {
                this.client.invoke(req -> req.uri(URI.create(consulUrl + "/v1/txn"))
                                .header("Content-Type", "application/json")
                                .PUT(new ObjectMapperPublisher(tx)), TxKVOperationResultList.class)
                        .onError(error409handler, 409)
                        .sendAndGet();
                log.info("Successfully set Global lock in namespaces '{}'", namespaces);
                return true;
            } catch (ErrorCodeException e) {
                if (e.getHttpCode() == 409) {
                    log.debug("Attempt to set Global lock in namespaces '{}' has failed. Reason: \n{}", namespaces, error409handler.getBody());
                } else {
                    throw new LockFailedException(LockAction.CREATE, namespaces, GLOBAL, e);
                }
            } catch (DefaultErrorCodeException | InvocationException e) {
                throw new LockFailedException(LockAction.CREATE, namespaces, GLOBAL, e);
            }
        } while (Duration.between(start, Instant.now()).compareTo(timeout) < 0);
        return false;
    }

    private String updateAndGetIndexForModifyMutexKV(String namespace) {
        try {
            List<TxKVOperationWrapper> modifyMutexTx = List.of(
                    new TxKVOperationWrapper(TxKVOperation.builder()
                            .verb(TxnKVVerb.SET)
                            .key(String.format(MODIFY_MUTEX_CONSUL_PATH_TEMPLATE, namespace))
                            .value(JsonUtil.toJson(new ModifyLockData(LockAction.CREATE, namespace, GLOBAL)))
                            .build()));
            TxKVOperationResultList modifyMutexResult = this.client.invoke(req -> req.uri(URI.create(consulUrl + "/v1/txn"))
                            .header("Content-Type", "application/json")
                            .PUT(new ObjectMapperPublisher(modifyMutexTx)), TxKVOperationResultList.class)
                    .sendAndGet();
            return Optional.ofNullable(modifyMutexResult).map(TxKVOperationResultList::getResults).map(r -> r.isEmpty() ? null : r.get(0))
                    .map(TxKVOperationResultWrapper::getKv).map(TxKVOperationResult::getModifyIndex)
                    .map(String::valueOf)
                    .orElseThrow(() -> new IllegalStateException("Invalid modifyMutexResult received: " + modifyMutexResult));
        } catch (DefaultErrorCodeException | InvocationException e) {
            throw new LockFailedException(LockAction.CREATE, List.of(namespace), "modify-mutex", e);
        }
    }

    private String getIndexForModifyMutexKV(String namespace) {
        try {
            ResponseHandler<String> modifyMutexGetResponse = this.client.invoke(req ->
                    req.uri(URI.create(consulUrl + CONSUL_KV_PATH + String.format(MODIFY_MUTEX_CONSUL_PATH_TEMPLATE, namespace)))
                            .header("Content-Type", "application/json")
                            .GET(), String.class).send();
            return LockUtils.getModifyIndex(modifyMutexGetResponse);
        } catch (DefaultErrorCodeException e) {
            if (e.getHttpCode() == 404) {
                return LockUtils.getModifyIndex(e.getDefaultErrorConsumer());
            } else {
                throw new LockFailedException(LockAction.READ, List.of(namespace), "modify-mutex", e);
            }
        } catch (InvocationException e) {
            throw new LockFailedException(LockAction.READ, List.of(namespace), "modify-mutex", e);
        }
    }

    private List<ModifyMutexIndex> longPollOnMsLocks(Duration timeout, List<String> namespaces, Instant start) {
        return namespaces.stream().map(namespace -> {
            do {
                String indexForModifyMutexBefore = updateAndGetIndexForModifyMutexKV(namespace);
                MsLocksList msLocksList = getMsLocksList(namespace);
                if (!msLocksList.list.isEmpty()) {
                    // there are MS locks, start long polling until next config/{namespace} change
                    String pollingWaitTime = ConsulUtil.toConsulTTLAsSeconds(LockUtils.getLongPollingWaitForTimeoutSeconds(timeout, start));
                    URI uri = URI.create(consulUrl + CONSUL_KV_PATH + String.format(NAMESPACE_CONFIG_PATH_TEMPLATE, namespace) +
                            String.format("?recurse=true&index=%s&wait=%s", msLocksList.modifyIndex, pollingWaitTime));
                    try {
                        log.debug("Sending request to Consul: {}", uri);
                        ResponseHandler<List<KVInfo>> responseHandler = this.client.invoke(req -> req.uri(uri)
                                .header("Content-Type", "application/json")
                                .GET(), new TypeReference<List<KVInfo>>() {
                        }).onError(StringResponseBodyHandler.INSTANCE, 404).send();
                        String modifyIndex = LockUtils.getModifyIndex(responseHandler);
                        log.debug(String.format(NAMESPACE_CONFIG_PATH_TEMPLATE, namespace) + " modifyIndex changed to: " + modifyIndex);
                    } catch (ErrorCodeException gme) {
                        if (gme.getHttpCode() != 404) {
                            throw new LockFailedException(LockAction.CREATE, List.of(namespace), GLOBAL, gme);
                        }
                    } catch (DefaultErrorCodeException | InvocationException gme) {
                        throw new LockFailedException(LockAction.CREATE, List.of(namespace), GLOBAL, gme);
                    }
                } else {
                    String indexForModifyMutexAfter = getIndexForModifyMutexKV(namespace);
                    if (Objects.equals(indexForModifyMutexBefore, indexForModifyMutexAfter)) {
                        return new ModifyMutexIndex(namespace, Long.valueOf(indexForModifyMutexAfter), true);
                    }
                }
            } while (Duration.between(start, Instant.now()).compareTo(timeout) < 0);
            return new ModifyMutexIndex(namespace, Long.valueOf(getIndexForModifyMutexKV(namespace)), false);
        }).toList();
    }

    private MsLocksList getMsLocksList(String namespace) throws LockFailedException {
        String modifyIndex;
        List<MicroserviceLockInfo> list;
        try {
            ResponseHandler<List<KVInfo>> responseHandler = this.client.invoke(req ->
                            req.uri(URI.create(consulUrl + String.format(CONSUL_KV_PATH + NAMESPACE_CONFIG_PATH_TEMPLATE + "?recurse=true", namespace)))
                                    .header("Content-Type", "application/json")
                                    .GET(), new TypeReference<List<KVInfo>>() {
                    })
                    .onError(StringResponseBodyHandler.INSTANCE, 404)
                    .send();
            modifyIndex = LockUtils.getModifyIndex(responseHandler);
            list = responseHandler.getBody().stream().map(kv -> {
                        Matcher matcher = MS_MUTEX_PATH_PATTERN.matcher(kv.getKey());
                        if (matcher.matches()) {
                            String ns = matcher.group("namespace");
                            String ms = matcher.group("microservice");
                            String pod = matcher.group("pod");
                            String name = matcher.group("name");
                            MicroserviceLockData lockData = JsonUtil.fromJson(kv.getValue(), MicroserviceLockData.class);
                            return new MicroserviceLockInfo(ns, ms, pod, name, lockData.getReason(), lockData.getTimestamp());
                        } else {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .toList();
        } catch (ErrorCodeException e) {
            if (e.getHttpCode() == 404) {
                modifyIndex = LockUtils.getModifyIndex(e.responseHandler);
                list = List.of();
            } else {
                throw new LockFailedException(LockAction.READ, List.of(namespace), ALL_MS_LOCKS, e);
            }
        } catch (DefaultErrorCodeException | InvocationException e) {
            throw new LockFailedException(LockAction.READ, List.of(namespace), ALL_MS_LOCKS, e);
        }
        return new MsLocksList(modifyIndex, list);
    }

    record ModifyMutexIndex(String namespace, Long modifyIndex, boolean noMSLocks) {
    }

    record MsLocksList(String modifyIndex, List<MicroserviceLockInfo> list) {
    }

}
