package com.netcracker.cloud.dbaas.client.arangodb.service;

import com.arangodb.ArangoCursor;
import com.arangodb.ArangoCursorAsync;
import com.arangodb.ArangoDB;
import com.arangodb.ArangoDBAsync;
import com.arangodb.ArangoDatabaseAsync;
import com.arangodb.internal.cursor.ArangoCursorImpl;
import com.arangodb.springframework.core.ArangoOperations;
import com.arangodb.springframework.core.template.ArangoTemplate;
import com.netcracker.cloud.dbaas.client.arangodb.configuration.DbaasArangoDBConfigurationProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DbaasArangoTemplateTest {

    private static final int HANG_DETECTION_SECONDS = 5;

    private ArangoTemplate arangoTemplate;
    private DbaasArangoTemplate dbaasArangoTemplate;
    private ArangoCursor arangoCursor42;
    private ArangoCursor arangoCursor13;

    @BeforeEach
    public void setup() throws NoSuchFieldException, IllegalAccessException {
        arangoTemplate = mock(ArangoTemplate.class);
        dbaasArangoTemplate = mock(DbaasArangoTemplate.class, Mockito.CALLS_REAL_METHODS);
        Field field = dbaasArangoTemplate.getClass().getDeclaredField("lock");
        field.setAccessible(true);
        field.set(dbaasArangoTemplate, new ReentrantReadWriteLock(true));
        Field arangoTemplateRefField = dbaasArangoTemplate.getClass().getDeclaredField("arangoTemplateRef");
        arangoTemplateRefField.setAccessible(true);
        arangoTemplateRefField.set(dbaasArangoTemplate, new AtomicReference<>());
        Field configField = dbaasArangoTemplate.getClass().getDeclaredField("dbaasArangoConfig");
        configField.setAccessible(true);
        configField.set(dbaasArangoTemplate, new DbaasArangoDBConfigurationProperties());
        arangoCursor42 = mock(ArangoCursorImpl.class);
        Mockito.lenient().when(arangoCursor42.next()).thenReturn(42);
        arangoCursor13 = mock(ArangoCursorImpl.class);
        Mockito.lenient().when(arangoCursor13.next()).thenReturn(13);
    }

    @SuppressWarnings("unchecked")
    private void setArangoTemplate(ArangoTemplate value) throws NoSuchFieldException, IllegalAccessException {
        Field field = dbaasArangoTemplate.getClass().getDeclaredField("arangoTemplateRef");
        field.setAccessible(true);
        ((AtomicReference<ArangoTemplate>) field.get(dbaasArangoTemplate)).set(value);
    }

    @Test
    public void testProxiedMethods() throws InvocationTargetException, IllegalAccessException {
        doReturn(arangoTemplate).when(dbaasArangoTemplate).getArangoTemplate();
        Method[] methods = ArangoOperations.class.getDeclaredMethods();
        for (Method method : methods) {
            Object[] params = new Object[method.getParameterCount()];
            method.invoke(dbaasArangoTemplate, params);
            method.invoke(verify(arangoTemplate), params);
        }
    }

    @Test
    public void testConcurrentSuccess_OneInit() throws InterruptedException, ExecutionException {
        int threadsCount = 10;
        when(arangoTemplate.query(eq("RETURN 13"), any())).thenReturn(arangoCursor13);
        Mockito.doAnswer(invocationOnMock -> {
            setArangoTemplate(arangoTemplate);
            return null;
        }).when(dbaasArangoTemplate).initArangoTemplate();

        ExecutorService executorService = Executors.newFixedThreadPool(threadsCount);
        List<Future<ArangoCursor<Integer>>> futures = executorService.invokeAll(IntStream.range(0, threadsCount)
                .mapToObj(i -> (Callable<ArangoCursor<Integer>>) () -> dbaasArangoTemplate.query("RETURN 13", Integer.class))
                .toList());
        executorService.shutdown();
        executorService.awaitTermination(10, TimeUnit.SECONDS);

        for (Future<ArangoCursor<Integer>> future : futures) {
            assertEquals(13, future.get().next());
        }
        verify(arangoTemplate, times(threadsCount)).query(eq("RETURN 13"), any());
        verify(dbaasArangoTemplate, times(1)).initArangoTemplate();
        verify(dbaasArangoTemplate, times(0)).checkConnection(arangoTemplate);
    }

    @Test
    public void testConcurrentFail_BlockNewQueriesDuringInit() throws Exception {
        when(arangoTemplate.query(any(), any())).thenThrow(new RuntimeException("Bad connection"));
        setArangoTemplate(arangoTemplate);

        ArangoTemplate arangoOperationsGood = mock(ArangoTemplate.class);
        // Signals once task1 has failed against arangoTemplate and is holding the write lock to
        // reinitialize — only then is task2 submitted, so task2 can never independently race
        // task1's first (failing) call to arangoTemplate.query().
        CountDownLatch reinitStarted = new CountDownLatch(1);
        Mockito.doAnswer(invocationOnMock -> {
            reinitStarted.countDown();
            when(arangoOperationsGood.query(any(), any())).thenReturn(arangoCursor42);
            setArangoTemplate(arangoOperationsGood);
            return null;
        }).when(dbaasArangoTemplate).initArangoTemplate();

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        Future<ArangoCursor<Integer>> taskResult1 = executorService.submit(() -> dbaasArangoTemplate.query("RETURN 13", Integer.class));
        assertTrue(reinitStarted.await(HANG_DETECTION_SECONDS, TimeUnit.SECONDS));
        Future<ArangoCursor<Integer>> taskResult2 = executorService.submit(() -> dbaasArangoTemplate.query("RETURN 13", Integer.class));
        executorService.shutdown();
        executorService.awaitTermination(10, TimeUnit.SECONDS);

        assertEquals(42, taskResult1.get().next());
        assertEquals(42, taskResult2.get().next());
        verify(arangoTemplate, times(1)).query(eq("RETURN 13"), any());
        verify(arangoOperationsGood, times(2)).query(eq("RETURN 13"), any());
        verify(dbaasArangoTemplate, times(1)).initArangoTemplate();
        verify(dbaasArangoTemplate, times(1)).checkConnection(arangoTemplate);
    }

    @Test
    public void testConcurrentFail_OneCheckAndOneReinit() throws InterruptedException {
        int threadsCount = 10;
        when(arangoTemplate.query(any(), any())).thenThrow(new RuntimeException("Fail all requests"));

        AtomicBoolean firstInit = new AtomicBoolean(true);
        ArangoTemplate newArangoTemplate = mock(ArangoTemplate.class);
        Mockito.doAnswer(invocationOnMock -> {
            if (firstInit.get()) {
                firstInit.set(false);
                setArangoTemplate(arangoTemplate);
            } else {
                Mockito.lenient().when(newArangoTemplate.query(eq("RETURN 42"), any())).thenReturn(arangoCursor42);
                Mockito.lenient().when(newArangoTemplate.query(eq("RETURN 13"), any())).thenReturn(arangoCursor13);
                setArangoTemplate(newArangoTemplate);
            }
            return null;
        }).when(dbaasArangoTemplate).initArangoTemplate();

        ExecutorService executorService = Executors.newFixedThreadPool(threadsCount);
        List<Future<ArangoCursor<Integer>>> tasksResults = executorService.invokeAll(IntStream.range(0, threadsCount)
                .mapToObj(i -> (Callable<ArangoCursor<Integer>>) () -> dbaasArangoTemplate.query("RETURN 13", Integer.class))
                .toList());
        executorService.shutdown();
        executorService.awaitTermination(10, TimeUnit.SECONDS);
        tasksResults.forEach(task -> {
            try {
                assertEquals(13, task.get().next());
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        });

        verify(arangoTemplate, atLeastOnce()).query(eq("RETURN 13"), any());
        verify(newArangoTemplate, times(threadsCount)).query(eq("RETURN 13"), any());
        verify(dbaasArangoTemplate, times(2)).initArangoTemplate();
        verify(dbaasArangoTemplate, times(1)).checkConnection(arangoTemplate);
        verify(dbaasArangoTemplate, times(0)).checkConnection(newArangoTemplate);
    }

    @Test
    public void testSuccess_CheckAndRetryNotRequired() {
        Mockito.doAnswer(invocationOnMock -> {
            setArangoTemplate(arangoTemplate);
            return null;
        }).when(dbaasArangoTemplate).initArangoTemplate();

        when(arangoTemplate.query(any(), any())).thenReturn(arangoCursor13);

        ArangoCursor<Integer> query = dbaasArangoTemplate.query("RETURN 13", Integer.class);
        assertEquals(13, query.next());
        verify(dbaasArangoTemplate, times(1)).initArangoTemplate();
        verify(dbaasArangoTemplate, times(0)).checkConnection(arangoTemplate);
    }

    @Test
    public void testSuccess_WithRetry() {
        Mockito.doAnswer(invocationOnMock -> {
            setArangoTemplate(arangoTemplate);
            return null;
        }).when(dbaasArangoTemplate).initArangoTemplate();

        // checkConnection no longer consumes a query stub (it probes via driver().async()),
        // so the direct call fails, the check fails (unstubbed driver() -> NPE -> false), then
        // the retry succeeds.
        when(arangoTemplate.query(any(), any()))
                .thenThrow(new RuntimeException("Bad connection for direct call"))
                .thenAnswer(invocationOnMock -> arangoCursor13);
        ArangoCursor<Integer> query = dbaasArangoTemplate.query("RETURN 13", Integer.class);
        assertEquals(13, query.next());
        verify(dbaasArangoTemplate, times(2)).initArangoTemplate();
        verify(dbaasArangoTemplate, times(1)).checkConnection(arangoTemplate);
    }

    @Test
    public void testFail_CheckFailAndRetryFail() {
        Mockito.doAnswer(invocationOnMock -> {
            setArangoTemplate(arangoTemplate);
            return null;
        }).when(dbaasArangoTemplate).initArangoTemplate();

        when(arangoTemplate.query(any(), any())).thenThrow(new RuntimeException("Bad connection"));
        assertThrows(RuntimeException.class, () -> dbaasArangoTemplate.query("RETURN 13", Integer.class));
        verify(arangoTemplate, times(2)).query(eq("RETURN 13"), any());
        verify(dbaasArangoTemplate, times(2)).initArangoTemplate();
        verify(dbaasArangoTemplate, times(1)).checkConnection(arangoTemplate);
    }

    @Test
    public void testCheckConnection_Timeout_ReturnsFalse() throws NoSuchFieldException, IllegalAccessException {
        // set a very short timeout via config
        DbaasArangoDBConfigurationProperties config = new DbaasArangoDBConfigurationProperties();
        config.setArangodb(Map.of("timeout", "100"));
        Field configField = dbaasArangoTemplate.getClass().getDeclaredField("dbaasArangoConfig");
        configField.setAccessible(true);
        configField.set(dbaasArangoTemplate, config);

        // A never-completing future -> get(timeout) times out -> false
        stubAsyncCheckQuery(arangoTemplate, new CompletableFuture<>());

        assertFalse(dbaasArangoTemplate.checkConnection(arangoTemplate));
    }

    @Test
    public void testCheckConnection_Interrupted_ReturnsFalse() {
        // A never-completing future keeps get() blocked; the pre-set interrupt makes it abort
        // with InterruptedException -> checkConnection returns false.
        stubAsyncCheckQuery(arangoTemplate, new CompletableFuture<>());

        Thread.currentThread().interrupt(); // caller interrupted -> future.get() aborts with InterruptedException
        assertFalse(dbaasArangoTemplate.checkConnection(arangoTemplate));
        // flag is re-set only by the InterruptedException branch; verify + clear so it can't leak
        assertTrue(Thread.interrupted());
    }

    /**
     * Wires operations.driver().async().db(any()).query("RETURN 42", Integer.class) to return
     * the given future, matching how the async checkConnection probes the connection.
     */
    private void stubAsyncCheckQuery(ArangoTemplate operations, CompletableFuture<ArangoCursorAsync<Integer>> future) {
        ArangoDB driver = mock(ArangoDB.class);
        ArangoDBAsync asyncDriver = mock(ArangoDBAsync.class);
        ArangoDatabaseAsync asyncDb = mock(ArangoDatabaseAsync.class);
        Mockito.lenient().when(operations.driver()).thenReturn(driver);
        Mockito.lenient().when(driver.async()).thenReturn(asyncDriver);
        Mockito.lenient().when(asyncDriver.db(any())).thenReturn(asyncDb);
        Mockito.lenient().when(asyncDb.query(eq("RETURN 42"), eq(Integer.class))).thenReturn(future);
    }

    @SuppressWarnings("unchecked")
    private ArangoCursorAsync<Integer> cursorAsyncReturning(Integer value) {
        ArangoCursorAsync<Integer> cursor = mock(ArangoCursorAsync.class);
        Mockito.lenient().when(cursor.getResult()).thenReturn(List.of(value));
        return cursor;
    }

    @Test
    public void testFail_CheckOkAndWithoutReinitAndWithoutRetry() throws NoSuchFieldException, IllegalAccessException {
        setArangoTemplate(arangoTemplate);

        stubAsyncCheckQuery(arangoTemplate, CompletableFuture.completedFuture(cursorAsyncReturning(42)));
        when(arangoTemplate.query(eq("RETURN 13"), any())).thenThrow(new RuntimeException("Bad request"));

        assertThrows(RuntimeException.class, () -> dbaasArangoTemplate.query("RETURN 13", Integer.class));
        verify(arangoTemplate, times(1)).query(eq("RETURN 13"), any());
        verify(dbaasArangoTemplate, times(0)).initArangoTemplate();
        verify(dbaasArangoTemplate, times(1)).checkConnection(any());
    }
}
