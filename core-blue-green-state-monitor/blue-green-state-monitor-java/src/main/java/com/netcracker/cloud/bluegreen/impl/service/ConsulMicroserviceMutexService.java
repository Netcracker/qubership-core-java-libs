package com.netcracker.cloud.bluegreen.impl.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.netcracker.cloud.bluegreen.api.error.LockFailedException;
import com.netcracker.cloud.bluegreen.api.model.LockAction;
import com.netcracker.cloud.bluegreen.api.service.MicroserviceMutexService;
import com.netcracker.cloud.bluegreen.impl.dto.consul.*;
import com.netcracker.cloud.bluegreen.impl.http.HttpClientAdapter;
import com.netcracker.cloud.bluegreen.impl.http.ObjectMapperPublisher;
import com.netcracker.cloud.bluegreen.impl.http.ResponseHandler;
import com.netcracker.cloud.bluegreen.impl.http.StringResponseBodyHandler;
import com.netcracker.cloud.bluegreen.impl.http.error.DefaultErrorCodeException;
import com.netcracker.cloud.bluegreen.impl.http.error.ErrorCodeException;
import com.netcracker.cloud.bluegreen.impl.http.error.InvocationException;
import com.netcracker.cloud.bluegreen.impl.util.ConsulUtil;
import com.netcracker.cloud.bluegreen.impl.util.EnvUtil;
import com.netcracker.cloud.bluegreen.impl.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Slf4j
@Deprecated(forRemoval = true)
public class ConsulMicroserviceMutexService implements MicroserviceMutexService, AutoCloseable {
    public static final String MS_MUTEX_CONSUL_PATH_TEMPLATE = "config/%s/%s/bluegreen/mutex/%s/%s"; //config/{namespace}/{microservice}/bluegreen/mutex/{name}/{pod}
    public static final Duration DEFAULT_TTL = Duration.ofMinutes(1);
    public static final Duration MIN_TTL = Duration.ofSeconds(10);
    public static final Duration MAX_TTL = Duration.ofHours(24); //todo should we narrow this down to more low value?
    public static final Duration ERROR_RETRY_DELAY = Duration.ofSeconds(1);
    public static final String CONSUL_KV_PATH = "/v1/kv/";

    final Map<String, SessionContext> sessionMap = new ConcurrentHashMap<>();
    final String consulUrl;
    final HttpClientAdapter client;
    final Duration sessionTTL;
    final String namespace;
    final String microserviceName;
    final String podName;

    public ConsulMicroserviceMutexService(Supplier<String> consulTokenSupplier) {
        this(consulTokenSupplier, EnvUtil.getConsulUrl(), EnvUtil.getNamespace(), EnvUtil.getMicroserviceName(), EnvUtil.getPodName());
    }

    public ConsulMicroserviceMutexService(Supplier<String> consulTokenSupplier, String consulUrl, String namespace, String microserviceName, String podName) {
        this(new HttpClientAdapter(consulTokenSupplier), consulUrl, namespace, microserviceName, podName, DEFAULT_TTL);
    }

    public ConsulMicroserviceMutexService(Supplier<String> consulTokenSupplier, String consulUrl, String namespace, String microserviceName, String podName, Duration sessionTTL) {
        this(new HttpClientAdapter(consulTokenSupplier), consulUrl, namespace, microserviceName, podName, sessionTTL);
    }

    public ConsulMicroserviceMutexService(HttpClientAdapter client, String consulUrl, String namespace, String microserviceName, String podName, Duration sessionTTL) {
        LockUtils.checkNotEmpty(Map.of(
                "client", client,
                "consulUrl", consulUrl,
                "namespace", namespace,
                "microserviceName", microserviceName,
                "podName", podName,
                "sessionTTL", sessionTTL));
        if (sessionTTL.compareTo(MIN_TTL) < 0 || sessionTTL.compareTo(MAX_TTL) > 0) {
            throw new IllegalArgumentException("Invalid ttl " + sessionTTL + ". Cannot be less than " + MIN_TTL + " or more than " + MAX_TTL);
        }
        this.consulUrl = consulUrl;
        this.namespace = namespace;
        this.microserviceName = microserviceName;
        this.podName = podName;
        this.client = client;
        this.sessionTTL = sessionTTL;
    }

    @Override
    public boolean tryLock(Duration timeout, String name, String reason) throws LockFailedException {
        LockUtils.checkNotEmpty(Map.of("timeout", timeout, "name", name, "reason", reason));
        LockUtils.checkLockTimeout(timeout);
        return this.tryLock(timeout, microserviceName, name, podName, reason);
    }

    private boolean tryLock(Duration timeout, String microserviceName, String name, String podName, String reason) {
        Instant start = Instant.now();
        String sessionId = null;
        do {
            if (sessionId != null) {
                // remove prev session because it may soon to be released
                try {
                    String uri = consulUrl + "/v1/session/destroy/" + sessionId;
                    this.client.invoke(req -> req.uri(URI.create(uri))
                            .header("Content-Type", "application/json")
                            .PUT(new ObjectMapperPublisher("")), String.class).sendAndGet();
                    log.debug("Deleted previous session with id = {}", sessionId);
                } catch (Exception ignored) {
                    log.warn("Failed to delete session with id = {}", sessionId);
                }
            }
            try {
                sessionId = this.client.invoke(req -> req.uri(URI.create(consulUrl + "/v1/session/create"))
                        .header("Content-Type", "application/json")
                        .PUT(new ObjectMapperPublisher(CreateSession.builder()
                                .behavior(CreateSession.Behavior.delete).ttl(ConsulUtil.toConsulTTL(sessionTTL))
                                .build())), Session.class).sendAndGet().getId();
                log.debug("Created consul session '{}' for microservice '{}' namespace '{}' name '{}' pod '{}'",
                        sessionId, microserviceName, namespace, name, podName);
                log.info("Trying to set lock for microservice '{}' in namespace '{}' name '{}' pod '{}'",
                        microserviceName, namespace, name, podName);
                ResponseHandler<TxKVOperationResultList> error409handler = new ResponseHandler<>(TxKVOperationResultList.class);
                try {
                    List<TxKVOperationWrapper> transaction = List.of(
                            // OpIndex=0
                            new TxKVOperationWrapper(TxKVOperation.builder()
                                    .verb(TxnKVVerb.CHECK_NOT_EXISTS)
                                    .key(String.format(ConsulGlobalMutexService.GLOBAL_MUTEX_CONSUL_PATH_TEMPLATE, namespace))
                                    .build()),
                            // OpIndex=1
                            new TxKVOperationWrapper(TxKVOperation.builder()
                                    .verb(TxnKVVerb.SET)
                                    .key(String.format(ConsulGlobalMutexService.MODIFY_MUTEX_CONSUL_PATH_TEMPLATE, namespace))
                                    .value(JsonUtil.toJson(new ModifyLockData(LockAction.CREATE, namespace, microserviceName)))
                                    .build()),
                            // OpIndex=2
                            // delete lock, in case it exists (for reentrant functionality)
                            new TxKVOperationWrapper(TxKVOperation.builder()
                                    .verb(TxnKVVerb.DELETE)
                                    .key(String.format(MS_MUTEX_CONSUL_PATH_TEMPLATE, namespace, microserviceName, name, podName))
                                    .value(JsonUtil.toJson(new MicroserviceLockData(OffsetDateTime.now(), reason)))
                                    .session(sessionId)
                                    .build()),
                            // OpIndex=3
                            new TxKVOperationWrapper(TxKVOperation.builder()
                                    .verb(TxnKVVerb.LOCK)
                                    .key(String.format(MS_MUTEX_CONSUL_PATH_TEMPLATE, namespace, microserviceName, name, podName))
                                    .value(JsonUtil.toJson(new MicroserviceLockData(OffsetDateTime.now(), reason)))
                                    .session(sessionId)
                                    .build())
                    );
                    if (log.isDebugEnabled()) {
                        log.debug("Trying to set lock for microservice '{}' in namespace '{}' name '{}' pod '{}' with transaction = \n{}",
                                microserviceName, namespace, name, podName,
                                transaction.stream().map(TxKVOperationWrapper::toString).collect(Collectors.joining("\n")));
                    }
                    this.client.invoke(req -> req.uri(URI.create(consulUrl + "/v1/txn"))
                                    .header("Content-Type", "application/json")
                                    .PUT(new ObjectMapperPublisher(transaction)), TxKVOperationResultList.class)
                            .onError(error409handler, 409)
                            .sendAndGet();
                    log.info("Successfully set lock for microservice '{}' in namespace '{}' name '{}' pod '{}'",
                            microserviceName, namespace, name, podName);
                    SessionContext previousSessionContext = sessionMap.put(buildLockKey(microserviceName, name),
                            new SessionContext(client, consulUrl, sessionId, namespace, microserviceName, name, podName, sessionTTL));
                    if (previousSessionContext != null) {
                        // reentrant case, need to cancel context of the previous session
                        previousSessionContext.cancel();
                    }
                    return true;
                } catch (ErrorCodeException e) {
                    if (e.getHttpCode() == 409) {
                        TxKVOperationResultList resultList = error409handler.getBody();
                        if (resultList.getErrors().stream().anyMatch(err -> err.getOpIndex() == 0)) {
                            // start long polling to watch for changes in Global lock
                            String lastModifyIndex = "0";
                            do {
                                // get index of or watch (long polling) current global lock
                                URI uri;
                                if (lastModifyIndex.equals("0")) {
                                    uri = URI.create(consulUrl + CONSUL_KV_PATH + String.format(ConsulGlobalMutexService.GLOBAL_MUTEX_CONSUL_PATH_TEMPLATE, namespace));
                                } else {
                                    String pollingWaitTime = ConsulUtil.toConsulTTLAsSeconds(LockUtils.getLongPollingWaitForTimeoutSeconds(timeout, start));
                                    uri = URI.create(consulUrl + CONSUL_KV_PATH + String.format(ConsulGlobalMutexService.GLOBAL_MUTEX_CONSUL_PATH_TEMPLATE, namespace) +
                                            String.format("?index=%s&wait=%s", lastModifyIndex, pollingWaitTime));
                                }
                                try {
                                    log.debug("Sending request to Consul: {}", uri);
                                    ResponseHandler<List<KVInfo>> responseHandler = this.client.invoke(req -> req.uri(uri)
                                            .header("Content-Type", "application/json")
                                            .GET(), new TypeReference<List<KVInfo>>() {
                                    }).onError(StringResponseBodyHandler.INSTANCE, 404).send();
                                    lastModifyIndex = LockUtils.getModifyIndex(responseHandler);
                                    log.debug("Received response from Consul: with modifyIndex={}. Global Lock is present", lastModifyIndex);
                                } catch (ErrorCodeException gme) {
                                    if (gme.getHttpCode() == 404) {
                                        log.debug("Global lock was not found, try acquire ms lock");
                                        break;
                                    } else {
                                        throw gme;
                                    }
                                }
                            } while (Duration.between(start, Instant.now()).compareTo(timeout) < 0);
                        } else {
                            throw e;
                        }
                    } else {
                        throw e;
                    }
                }
            } catch (DefaultErrorCodeException | InvocationException e) {
                throw new LockFailedException(LockAction.CREATE, List.of(namespace), microserviceName, e);
            }
        } while (Duration.between(start, Instant.now()).compareTo(timeout) < 0);
        return false;
    }

    @Override
    public void unlock(String name) {
        LockUtils.checkNotEmpty(Map.of("name", name));
        this.unlock(microserviceName, name, podName);
    }

    private void unlock(String microserviceName, String name, String podName) {
        log.info("Removing lock for microservice '{}' in namespace '{}' with name '{}' with pod '{}'", microserviceName, namespace, name, podName);
        SessionContext sessionContext = this.sessionMap.remove(buildLockKey(microserviceName, name));
        if (sessionContext == null) {
            throw new IllegalArgumentException(String.format("There is no lock session for microservice '%s' with name='%s' with pod='%s'",
                    microserviceName, name, podName));
        }
        sessionContext.cancel();
        String sessionId = sessionContext.sessionId;
        try {
            List<TxKVOperationWrapper> transaction = List.of(
                    new TxKVOperationWrapper(TxKVOperation.builder()
                            .verb(TxnKVVerb.SET)
                            .key(String.format(ConsulGlobalMutexService.MODIFY_MUTEX_CONSUL_PATH_TEMPLATE, namespace))
                            .value(JsonUtil.toJson(new ModifyLockData(LockAction.DELETE, namespace, microserviceName)))
                            .build()),
                    new TxKVOperationWrapper(TxKVOperation.builder()
                            .verb(TxnKVVerb.UNLOCK)
                            .key(String.format(MS_MUTEX_CONSUL_PATH_TEMPLATE, namespace, microserviceName, name, podName))
                            .value(JsonUtil.toJson(new MicroserviceLockData(OffsetDateTime.now(), "unlock")))
                            .session(sessionId)
                            .build()),
                    new TxKVOperationWrapper(TxKVOperation.builder()
                            .verb(TxnKVVerb.DELETE)
                            .key(String.format(MS_MUTEX_CONSUL_PATH_TEMPLATE, namespace, microserviceName, name, podName))
                            .build())
            );
            this.client.invoke(req -> req.uri(URI.create(consulUrl + "/v1/txn"))
                    .header("Content-Type", "application/json")
                    .PUT(new ObjectMapperPublisher(transaction)), Session.class).sendAndGet();
            log.info("Removed lock for microservice '{}' in namespace '{}' with name '{}' with pod '{}'", microserviceName, namespace, name, podName);
        } catch (DefaultErrorCodeException | InvocationException e) {
            throw new LockFailedException(LockAction.DELETE, List.of(namespace), microserviceName, e);
        }
    }

    @Override
    public boolean isLocked(String name) {
        LockUtils.checkNotEmpty(Map.of("name", name));
        return this.isLocked(microserviceName, name, podName);
    }

    private boolean isLocked(String microserviceName, String name, String podName) {
        boolean locked;
        try {
            log.debug("Checking if lock for microservice '{}' in namespace '{}' with name '{}' with pod '{}' exists",
                    microserviceName, namespace, name, podName);
            locked = !this.client.invoke(req ->
                            req.uri(URI.create(consulUrl + CONSUL_KV_PATH + String.format(MS_MUTEX_CONSUL_PATH_TEMPLATE, namespace, microserviceName, name, podName)))
                                    .header("Content-Type", "application/json")
                                    .GET(), new TypeReference<List<KVInfo>>() {
                    })
                    .onError(StringResponseBodyHandler.INSTANCE, 404)
                    .sendAndGet().isEmpty();
        } catch (ErrorCodeException e) {
            if (e.getHttpCode() == 404) {
                locked = false;
            } else {
                throw new LockFailedException(LockAction.READ, List.of(namespace), microserviceName, e);
            }
        }
        log.debug("Lock for microservice '{}' in namespace '{}' with name '{}' with pod '{}' {}",
                microserviceName, namespace, name, podName, locked ? "exists" : "does not exist");
        return locked;
    }

    @Override
    public void close() {
        log.info("Closing {}", this.getClass().getSimpleName());
        new HashSet<>(this.sessionMap.keySet()).stream().map(this.sessionMap::remove).filter(Objects::nonNull).forEach(ctx -> {
            log.debug("Closing session context with id={}, ms={}, namespace={}, name='{}'",
                    ctx.sessionId, ctx.microservice, ctx.namespace, ctx.name);
            ctx.cancel();
        });
    }

    private static String buildLockKey(String microserviceName, String name) {
        return String.format("%s+%s", microserviceName, name);
    }

    @Slf4j
    static class SessionContext {
        final HttpClientAdapter client;
        final String consulUrl;
        final String sessionId;
        final String namespace;
        final String microservice;
        final String name;
        final String pod;
        final ScheduledExecutorService executorService;
        final AtomicBoolean cancelled;

        public SessionContext(HttpClientAdapter client, String consulUrl, String sessionId,
                              String namespace, String microservice, String name, String pod, Duration initialTTL) {
            this.client = client;
            this.consulUrl = consulUrl;
            this.sessionId = sessionId;
            this.namespace = namespace;
            this.microservice = microservice;
            this.name = name;
            this.pod = pod;
            this.cancelled = new AtomicBoolean(false);
            this.executorService = Executors.newScheduledThreadPool(1, r -> new Thread(r, String.format("ms-lock-renewer-%s", name)));
            Runnable renewSession = () -> {
                try {
                    Duration ttl = this.renewSession();
                    if (!this.cancelled.get()) {
                        Duration renewDelay = getRenewDelay(ttl);
                        log.debug("Received TTL = {}. Scheduling next renew session call after {}", ttl, renewDelay);
                        this.executorService.schedule((Runnable) this, renewDelay.toMillis(), TimeUnit.MILLISECONDS);
                    }
                } catch (SessionNotFoundException e) {
                    // seems like we failed to renew the session it was deleted by Consul
                    log.error("Session '%s' for mutex '%s/%s' was not found in Consul on attempt to renew it. Terminating the renew loop.", e);
                } catch (InterruptedException e) {
                    log.info("The thread was interrupted. It means {} is being closed", this.getClass().getSimpleName());
                }
            };
            this.executorService.submit(renewSession);
            log.info("Started renew lock session thread (sessionId = '{}') for microservice '{}' pod '{}' in namespace '{}' with initial TTL = {}",
                    sessionId, microservice, pod, namespace, initialTTL);
        }

        private Duration getRenewDelay(Duration ttl) {
            return ttl.compareTo(Duration.ofSeconds(20)) <= 0 ? Duration.ofSeconds(5) : ttl.minus(Duration.ofSeconds(10));
        }

        private void cancel() {
            log.debug("Cancelling renew lock session {}", this.sessionId);
            this.cancelled.set(true);
            this.executorService.shutdownNow();
        }

        private Duration renewSession() throws SessionNotFoundException, InterruptedException {
            try {
                URI uri = URI.create(consulUrl + "/v1/session/renew/" + sessionId);
                log.debug("Sending session renew request to '{}'", uri);
                List<RenewSessionInfo> renewSessionInfoList = this.client.invoke(req ->
                                req.uri(uri)
                                        .header("Content-Type", "application/json")
                                        .PUT(new ObjectMapperPublisher(Map.of())), new TypeReference<List<RenewSessionInfo>>() {
                        })
                        .onError(StringResponseBodyHandler.INSTANCE, 404).sendAndGet();
                Duration newTTL = Optional.of(renewSessionInfoList).map(l -> l.isEmpty() ? null : l.get(0))
                        .map(RenewSessionInfo::getTtl)
                        .map(ConsulUtil::fromConsulTTL)
                        .orElseThrow(() -> new IllegalStateException(String.format("Invalid session renew response: %s",
                                renewSessionInfoList.stream().map(RenewSessionInfo::toString).collect(Collectors.joining(",")))));
                log.debug("Successfully renewed session '{}'. TTL from response = {}", sessionId, newTTL);
                return newTTL;
            } catch (ErrorCodeException e) {
                if (e.getHttpCode() == 404) {
                    throw new SessionNotFoundException(e);
                } else {
                    // retry as soon as possible
                    log.warn("Received error response for renew session ('{}'). Retrying. Reason: \n{}", sessionId, e.getMessage());
                    return ERROR_RETRY_DELAY;
                }
            } catch (DefaultErrorCodeException | InvocationException e) {
                if (e.getCause() instanceof InterruptedException) {
                    throw (InterruptedException) e.getCause();
                }
                // retry as soon as possible
                log.warn("Received error response for renew session ('{}'). Retrying. Reason: \n{}", sessionId, e.getMessage());
                return ERROR_RETRY_DELAY;
            }
        }

        static class SessionNotFoundException extends RuntimeException {
            public SessionNotFoundException(Throwable cause) {
                super(cause);
            }
        }
    }
}
