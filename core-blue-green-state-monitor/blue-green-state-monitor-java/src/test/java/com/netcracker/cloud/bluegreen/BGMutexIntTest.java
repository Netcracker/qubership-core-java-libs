package com.netcracker.cloud.bluegreen;

import com.netcracker.cloud.bluegreen.api.model.MicroserviceLockInfo;
import com.netcracker.cloud.bluegreen.api.service.GlobalMutexService;
import com.netcracker.cloud.bluegreen.api.service.MicroserviceMutexService;
import com.netcracker.cloud.bluegreen.impl.http.ObjectMapperPublisher;
import com.netcracker.cloud.bluegreen.impl.service.ConsulGlobalMutexService;
import com.netcracker.cloud.bluegreen.impl.service.ConsulMicroserviceMutexService;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import static com.netcracker.cloud.bluegreen.impl.service.ConsulMicroserviceMutexService.MAX_TTL;
import static com.netcracker.cloud.bluegreen.impl.service.ConsulMicroserviceMutexService.MIN_TTL;


class BGMutexIntTest extends AbstractBGTest {

    @AfterEach
    void validateThereAreNoHangingLocks() {
        retry(Duration.ofSeconds(10), () -> Assertions.assertEquals(0, getMsLockRenewerThreadsCount(),
                "there are active 'ms-lock-renewer' threads but expecting none")
        );
    }

    @Test
    void testMsLockCreateFailsIfGlobalLockExists() {
        new ConsulGlobalMutexService(client, consulUrl).tryLock(Duration.ofSeconds(1), namespaces);
        try (ConsulMicroserviceMutexService microserviceMutexServiceNs1 =
                     new ConsulMicroserviceMutexService(client, consulUrl, ns1, ms, pod1, MIN_TTL);
             ConsulMicroserviceMutexService microserviceMutexServiceNs2 =
                     new ConsulMicroserviceMutexService(client, consulUrl, ns2, ms, pod1, MIN_TTL)) {
            Assertions.assertFalse(microserviceMutexServiceNs1.tryLock(Duration.ZERO, "lock", "reason-1"));
            Assertions.assertFalse(microserviceMutexServiceNs2.tryLock(Duration.ZERO, "lock", "reason-2"));
        }
    }

    @Test
    void testGlobalLockCreateFailsIfMsLockExists() {
        GlobalMutexService globalMutexService = new ConsulGlobalMutexService(client, consulUrl);
        try (ConsulMicroserviceMutexService microserviceMutexServiceNs1 = new ConsulMicroserviceMutexService(client, consulUrl, ns1, ms, pod1, MIN_TTL);
             ConsulMicroserviceMutexService microserviceMutexServiceNs2 = new ConsulMicroserviceMutexService(client, consulUrl, ns2, ms, pod1, MIN_TTL)) {
            Assertions.assertTrue(microserviceMutexServiceNs1.tryLock(Duration.ZERO, "lock", "reason-1"));
            Assertions.assertFalse(globalMutexService.tryLock(Duration.ZERO, namespaces));
            microserviceMutexServiceNs1.unlock("lock");

            Assertions.assertTrue(globalMutexService.tryLock(Duration.ZERO, namespaces));
            globalMutexService.unlock(namespaces);

            Assertions.assertTrue(microserviceMutexServiceNs2.tryLock(Duration.ZERO, "lock", "reason-2"));
            Assertions.assertFalse(globalMutexService.tryLock(Duration.ZERO, namespaces));
            microserviceMutexServiceNs2.unlock("lock");

            Assertions.assertTrue(globalMutexService.tryLock(Duration.ZERO, namespaces));
            globalMutexService.unlock(namespaces);
        }
    }

    @Test
    void testGlobalLockIfMsConfigExists() {
        saveMsConfigInConsul(ns1, ms, "test");
        saveMsConfigInConsul(ns2, ms, "test");
        GlobalMutexService globalMutexService = new ConsulGlobalMutexService(client, consulUrl);
        Assertions.assertTrue(globalMutexService.tryLock(Duration.ZERO, namespaces));
        globalMutexService.unlock(namespaces);

        try (ConsulMicroserviceMutexService microserviceMutexServiceNs1 = new ConsulMicroserviceMutexService(client, consulUrl, ns1, ms, pod1, MIN_TTL);
             ConsulMicroserviceMutexService microserviceMutexServiceNs2 = new ConsulMicroserviceMutexService(client, consulUrl, ns2, ms, pod1, MIN_TTL)) {

            Assertions.assertTrue(microserviceMutexServiceNs1.tryLock(Duration.ZERO, "lock", "reason-1"));
            Assertions.assertFalse(globalMutexService.tryLock(Duration.ZERO, namespaces));
            microserviceMutexServiceNs1.unlock("lock");
            Assertions.assertTrue(globalMutexService.tryLock(Duration.ZERO, namespaces));

            globalMutexService.unlock(namespaces);

            Assertions.assertTrue(microserviceMutexServiceNs2.tryLock(Duration.ZERO, "lock", "reason-2"));
            Assertions.assertFalse(globalMutexService.tryLock(Duration.ZERO, namespaces));
            microserviceMutexServiceNs2.unlock("lock");
            Assertions.assertTrue(globalMutexService.tryLock(Duration.ZERO, namespaces));
            globalMutexService.unlock(namespaces);
        }
    }

    @Test
    void testMsLockSessionsAreRenewed() throws InterruptedException {
        GlobalMutexService globalMutexService = new ConsulGlobalMutexService(client, consulUrl);
        try (ConsulMicroserviceMutexService microserviceMutexServiceNs1 =
                     new ConsulMicroserviceMutexService(client, consulUrl, ns1, ms, pod1, MIN_TTL);
             ConsulMicroserviceMutexService microserviceMutexServiceNs2 =
                     new ConsulMicroserviceMutexService(client, consulUrl, ns2, ms, pod1, MIN_TTL)) {
            Assertions.assertTrue(microserviceMutexServiceNs1.tryLock(Duration.ZERO, "lock", "reason-1"));
            Assertions.assertTrue(microserviceMutexServiceNs2.tryLock(Duration.ZERO, "lock", "reason-2"));
            Consumer<List<MicroserviceLockInfo>> validateLocks = locks ->
                    Set.of(
                            new MicroserviceLockInfo("ns-1", "ms-1", "lock", pod1, "reason-1", OffsetDateTime.now()),
                            new MicroserviceLockInfo("ns-2", "ms-1", "lock", pod1, "reason-2", OffsetDateTime.now())
                    ).forEach(el -> Assertions.assertTrue(locks.stream().anyMatch(al ->
                            Objects.equals(el.getNamespace(), al.getNamespace()) &&
                                    Objects.equals(el.getMicroserviceName(), al.getMicroserviceName()) &&
                                    Objects.equals(el.getReason(), al.getReason())
                    )));

            List<MicroserviceLockInfo> microserviceLocks = globalMutexService.getMicroserviceLocks(namespaces);
            validateLocks.accept(microserviceLocks);

            // wait TTL time + 5 sec
            Thread.sleep(ConsulMicroserviceMutexService.MIN_TTL.plus(Duration.ofSeconds(5)).toMillis());

            microserviceLocks = globalMutexService.getMicroserviceLocks(namespaces);
            validateLocks.accept(microserviceLocks);
        }
    }

    @Test
    void testMsIsLocked() {
        try (ConsulMicroserviceMutexService microserviceMutexService = new ConsulMicroserviceMutexService(client, consulUrl, ns1, ms, pod1, MIN_TTL)) {
            Assertions.assertTrue(microserviceMutexService.tryLock(Duration.ZERO, "lock", "reason-1"));
            Assertions.assertTrue(microserviceMutexService.isLocked("lock"));
            microserviceMutexService.unlock("lock");
            Assertions.assertFalse(microserviceMutexService.isLocked("lock"));
        }
    }

    @Test
    void testMSTryLockReentry() {
        try (ConsulMicroserviceMutexService microserviceMutexService = new ConsulMicroserviceMutexService(client, consulUrl, ns1, ms, pod1, MIN_TTL)) {
            Assertions.assertEquals(0, getMsLockRenewerThreadsCount());
            microserviceMutexService.tryLock(Duration.ZERO, "lock", "reason-1");
            Assertions.assertEquals(1, getMsLockRenewerThreadsCount());
            Assertions.assertTrue(microserviceMutexService.isLocked("lock"));
            Assertions.assertDoesNotThrow(() -> microserviceMutexService.tryLock(Duration.ZERO, "lock", "reason-1"));
            Assertions.assertDoesNotThrow(() -> microserviceMutexService.tryLock(Duration.ZERO, "lock", "reason-1"));
            Assertions.assertDoesNotThrow(() -> microserviceMutexService.tryLock(Duration.ZERO, "lock", "reason-1"));
            Assertions.assertTrue(microserviceMutexService.isLocked("lock"));
            retry(Duration.ofSeconds(10), () -> Assertions.assertEquals(1, getMsLockRenewerThreadsCount()));
            microserviceMutexService.unlock("lock");
            retry(Duration.ofSeconds(10), () -> Assertions.assertEquals(0, getMsLockRenewerThreadsCount()));
        }
    }

    @Test
    void testGlobalTryLockReentry() {
        GlobalMutexService globalMutexService = new ConsulGlobalMutexService(client, consulUrl);
        globalMutexService.tryLock(Duration.ZERO, namespaces);
        Assertions.assertTrue(globalMutexService.isLocked(namespaces));
        Assertions.assertTrue(globalMutexService.tryLock(Duration.ZERO, namespaces));
        Assertions.assertTrue(globalMutexService.tryLock(Duration.ZERO, namespaces));
        Assertions.assertTrue(globalMutexService.tryLock(Duration.ZERO, namespaces));
        Assertions.assertTrue(globalMutexService.tryLock(Duration.ZERO, namespaces));
        Assertions.assertTrue(globalMutexService.isLocked(namespaces));
    }

    @Test
    void testGlobalForceLockReentry() {
        GlobalMutexService globalMutexService = new ConsulGlobalMutexService(client, consulUrl);
        globalMutexService.forceLock(namespaces);
        Assertions.assertTrue(globalMutexService.isLocked(namespaces));
        Assertions.assertTrue(globalMutexService.forceLock(namespaces));
    }

    @Test
    void testGlobalLockRetry() throws InterruptedException {
        GlobalMutexService globalMutexService = new ConsulGlobalMutexService(client, consulUrl);
        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
        try (ConsulMicroserviceMutexService msSrvNs1 = new ConsulMicroserviceMutexService(client, consulUrl, ns1, ms, pod1, MIN_TTL);
             ConsulMicroserviceMutexService msSrvNs2 = new ConsulMicroserviceMutexService(client, consulUrl, ns2, ms, pod1, MIN_TTL)) {
            CountDownLatch latch = new CountDownLatch(1);
            executorService.schedule(() -> {
                Assertions.assertTrue(msSrvNs1.tryLock(Duration.ZERO, "lock", "reason-2"));
                latch.countDown();
            }, 0, TimeUnit.SECONDS);
            executorService.schedule(() -> Assertions.assertTrue(msSrvNs2.tryLock(Duration.ZERO, "lock", "reason-1")),
                    1, TimeUnit.SECONDS);
            executorService.schedule(() -> msSrvNs1.unlock("lock"), 2, TimeUnit.SECONDS);
            executorService.schedule(() -> msSrvNs2.unlock("lock"), 3, TimeUnit.SECONDS);

            Assertions.assertTrue(latch.await(2, TimeUnit.SECONDS));
            Assertions.assertTrue(globalMutexService.tryLock(Duration.ofSeconds(5), namespaces));
            Assertions.assertTrue(globalMutexService.isLocked(namespaces));
        } finally {
            executorService.shutdownNow();
        }
    }

    @Test
    void testMSLockRetry() throws InterruptedException {
        Duration msLockTimeout = Duration.ofSeconds(10);
        GlobalMutexService globalMutexService = new ConsulGlobalMutexService(client, consulUrl);
        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
        try (ConsulMicroserviceMutexService microserviceMutexService = new ConsulMicroserviceMutexService(client, consulUrl, ns1, ms, pod1, MIN_TTL)) {
            CountDownLatch latch = new CountDownLatch(1);
            executorService.submit(() -> {
                globalMutexService.tryLock(Duration.ZERO, namespaces);
                latch.countDown();
            });
            executorService.schedule(() -> globalMutexService.unlock(namespaces), msLockTimeout.dividedBy(2).toMillis(), TimeUnit.MILLISECONDS);
            Assertions.assertTrue(latch.await(2, TimeUnit.SECONDS));
            Assertions.assertTrue(microserviceMutexService.tryLock(msLockTimeout, "lock", "reason-1"));
            Assertions.assertTrue(microserviceMutexService.isLocked("lock"));
        } finally {
            executorService.shutdownNow();
        }
    }

    @Test
    void testValidateMinMaxTTL() {
        Duration tooSmallTTL = Duration.ofSeconds(1);
        Duration tooLargeTTL = Duration.ofHours(25);
        Assertions.assertTrue(tooSmallTTL.compareTo(MIN_TTL) < 0);
        Assertions.assertTrue(tooLargeTTL.compareTo(MAX_TTL) > 0);
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            try (ConsulMicroserviceMutexService ignored = new ConsulMicroserviceMutexService(consulTokenSupplier, consulUrl, ns1, ms, pod1, tooSmallTTL)) {
                Assertions.fail("should not happen");
            }
        });
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            try (ConsulMicroserviceMutexService ignored = new ConsulMicroserviceMutexService(consulTokenSupplier, consulUrl, ns1, ms, pod1, tooLargeTTL)) {
                Assertions.fail("should not happen");
            }
        });
    }

    @Test
    void testRaceConditionMutexesCreation() {
        Duration timeout = Duration.ofSeconds(1);
        int cores = Runtime.getRuntime().availableProcessors();
        if (cores % 2 != 0) {
            cores = -1;
        }
        Assumptions.assumeTrue(cores > 0);
        GlobalMutexService globalMutexService = new ConsulGlobalMutexService(client, consulUrl);
        try (ConsulMicroserviceMutexService msMutexService = new ConsulMicroserviceMutexService(client, consulUrl, ns1, ms, pod1, MIN_TTL)) {
            List<MutexResult> results = IntStream.range(0, cores).parallel().mapToObj(i -> {
                if (i % 2 == 0) {
                    return new MutexResult(globalMutexService.tryLock(timeout, namespaces), MutexResult.Type.global);
                } else {
                    return new MutexResult(msMutexService.tryLock(timeout, "lock", "test-reason"), MutexResult.Type.ms);
                }
            }).toList();
            List<MutexResult> unlockedResults = results.stream().filter(r -> !r.locked).toList();
            List<MutexResult> lockedResults = results.stream().filter(r -> r.locked).toList();
            Assertions.assertEquals(cores / 2, unlockedResults.size());
            Assertions.assertEquals(cores / 2, lockedResults.size());
            MutexResult.Type unlockedType = unlockedResults.get(0).type;
            MutexResult.Type lockedType = lockedResults.get(0).type;
            Assertions.assertEquals(cores / 2, unlockedResults.stream().filter(r -> !Objects.equals(r.type, lockedType)).count());
            Assertions.assertEquals(cores / 2, lockedResults.stream().filter(r -> !Objects.equals(r.type, unlockedType)).count());
        }
    }

    @Test
    void testGlobalForceLock() {
        GlobalMutexService globalMutexService = new ConsulGlobalMutexService(client, consulUrl);
        MicroserviceMutexService microserviceMutexServiceNs1 =
                new ConsulMicroserviceMutexService(client, consulUrl, ns1, ms, pod1, MIN_TTL);
        MicroserviceMutexService microserviceMutexServiceNs2 =
                new ConsulMicroserviceMutexService(client, consulUrl, ns2, ms, pod1, MIN_TTL);
        Consumer<MicroserviceMutexService> validateFunc = msMutexSrv -> {
            Assertions.assertTrue(msMutexSrv.tryLock(Duration.ZERO, "lock", "reason-1"));
            Assertions.assertTrue(globalMutexService.forceLock(namespaces));
            Assertions.assertTrue(globalMutexService.isLocked(namespaces));
            Assertions.assertTrue(msMutexSrv.isLocked("lock"));
            msMutexSrv.unlock("lock");
            globalMutexService.unlock(namespaces);
            Assertions.assertFalse(msMutexSrv.isLocked("lock"));
            Assertions.assertFalse(globalMutexService.isLocked(namespaces));
        };
        validateFunc.accept(microserviceMutexServiceNs1);
        validateFunc.accept(microserviceMutexServiceNs2);
    }

    @Test
    void testMsLockFrom2Pods() throws InterruptedException {
        try (ConsulMicroserviceMutexService msMutexServicePod1 = new ConsulMicroserviceMutexService(client, consulUrl, ns1, ms, pod1, MIN_TTL);
             ConsulMicroserviceMutexService msMutexServicePod2 = new ConsulMicroserviceMutexService(client, consulUrl, ns1, ms, pod2, MIN_TTL)) {

            Assertions.assertTrue(msMutexServicePod1.tryLock(Duration.ZERO, "lock", "reason-1"));
            Assertions.assertTrue(msMutexServicePod2.tryLock(Duration.ZERO, "lock", "reason-2"));

            Thread.sleep(ConsulMicroserviceMutexService.MIN_TTL.toMillis());

            msMutexServicePod1.unlock("lock");
            Assertions.assertFalse(msMutexServicePod1.isLocked("lock"));
            Assertions.assertTrue(msMutexServicePod2.isLocked("lock"));

            msMutexServicePod2.unlock("lock");
            Assertions.assertFalse(msMutexServicePod1.isLocked("lock"));
            Assertions.assertFalse(msMutexServicePod2.isLocked("lock"));
        }
    }

    @Test
    void testMsMutexServiceAutoClose() {
        try (ConsulMicroserviceMutexService microserviceMutexService =
                     new ConsulMicroserviceMutexService(client, consulUrl, ns1, ms, pod1, MIN_TTL)) {
            Assertions.assertTrue(microserviceMutexService.tryLock(Duration.ZERO, "lock", "reason-1"));
            Assertions.assertEquals(1, getMsLockRenewerThreadsCount());
        }
        retry(Duration.ofSeconds(10), () -> Assertions.assertEquals(0, getMsLockRenewerThreadsCount()));
    }

    @Data
    @AllArgsConstructor
    static class MutexResult {
        boolean locked;
        Type type;

        enum Type {
            global, ms
        }
    }

    private static long getMsLockRenewerThreadsCount() {
        return getThreadsCount("ms-lock-renewer");
    }

    private void saveMsConfigInConsul(String namespace, String microservice, Object body) {
        this.client.invoke(req -> req.uri(URI.create(consulUrl + "/v1/kv/config/" + namespace + "/" + microservice + "/test"))
                        .header("Content-Type", "application/json")
                        .PUT(new ObjectMapperPublisher(body)),
                String.class).sendAndGet();
    }

}
