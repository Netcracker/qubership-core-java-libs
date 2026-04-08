package com.netcracker.cloud.dbaas.client.arangodb.service;

import com.arangodb.ArangoCursor;
import com.arangodb.internal.cursor.ArangoCursorImpl;
import com.arangodb.springframework.core.ArangoOperations;
import com.arangodb.springframework.core.template.ArangoTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
public class DbaasArangoTemplateTest {

    private ArangoTemplate arangoTemplate;
    private DbaasArangoTemplate dbaasArangoTemplate;
    private ArangoCursor arangoCursor42;
    private ArangoCursor arangoCursor13;

    @BeforeEach
    public void setup() throws NoSuchFieldException, IllegalAccessException {
        arangoTemplate = Mockito.mock(ArangoTemplate.class);
        dbaasArangoTemplate = Mockito.mock(DbaasArangoTemplate.class, Mockito.CALLS_REAL_METHODS);
        Field field = dbaasArangoTemplate.getClass().getDeclaredField("lock");
        field.setAccessible(true);
        field.set(dbaasArangoTemplate, new ReentrantReadWriteLock(true));
        arangoCursor42 = Mockito.mock(ArangoCursorImpl.class);
        Mockito.lenient().when(arangoCursor42.next()).thenReturn(42);
        arangoCursor13 = Mockito.mock(ArangoCursorImpl.class);
        Mockito.lenient().when(arangoCursor13.next()).thenReturn(13);
    }

    @Test
    public void testProxiedMethods() throws InvocationTargetException, IllegalAccessException {
        Mockito.doReturn(arangoTemplate).when(dbaasArangoTemplate).getArangoTemplate();
        Method[] methods = ArangoOperations.class.getDeclaredMethods();
        for (Method method : methods) {
            Object[] params = new Object[method.getParameterCount()];
            method.invoke(dbaasArangoTemplate, params);
            method.invoke(Mockito.verify(arangoTemplate), params);
        }
    }

    @Test
    public void testConcurrentSuccess_OneInit() throws InterruptedException, ExecutionException {
        int threadsCount = 10;
        Mockito.when(arangoTemplate.query(eq("RETURN 13"), any())).thenReturn(arangoCursor13);
        Mockito.doAnswer(invocationOnMock -> {
            Thread.sleep(10);
            Field field = dbaasArangoTemplate.getClass().getDeclaredField("arangoTemplate");
            field.setAccessible(true);
            field.set(dbaasArangoTemplate, arangoTemplate);
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
        Mockito.verify(arangoTemplate, times(threadsCount)).query(eq("RETURN 13"), any());
        Mockito.verify(dbaasArangoTemplate, times(1)).initArangoTemplate();
        Mockito.verify(dbaasArangoTemplate, times(0)).checkConnection(arangoTemplate);
    }

    @Test
    public void testConcurrentFail_BlockNewQueriesDuringInit() throws Exception {
        Mockito.when(arangoTemplate.query(any(), any())).thenThrow(new RuntimeException("Bad connection"));
        Field field = dbaasArangoTemplate.getClass().getDeclaredField("arangoTemplate");
        field.setAccessible(true);
        field.set(dbaasArangoTemplate, arangoTemplate);

        ArangoTemplate arangoOperationsGood = Mockito.mock(ArangoTemplate.class);
        Mockito.doAnswer(invocationOnMock -> {
            Thread.sleep(100);
            Mockito.when(arangoOperationsGood.query(any(), any())).thenReturn(arangoCursor42);
            Field field1 = dbaasArangoTemplate.getClass().getDeclaredField("arangoTemplate");
            field1.setAccessible(true);
            field1.set(dbaasArangoTemplate, arangoOperationsGood);
            return null;
        }).when(dbaasArangoTemplate).initArangoTemplate();

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        Future<ArangoCursor<Integer>> taskResult1 = executorService.submit(() -> dbaasArangoTemplate.query("RETURN 13", Integer.class));
        Thread.sleep(50);
        Future<ArangoCursor<Integer>> taskResult2 = executorService.submit(() -> dbaasArangoTemplate.query("RETURN 13", Integer.class));
        executorService.shutdown();
        executorService.awaitTermination(10, TimeUnit.SECONDS);

        assertEquals(42, taskResult1.get().next());
        assertEquals(42, taskResult2.get().next());
        Mockito.verify(arangoTemplate, times(1)).query(eq("RETURN 13"), any());
        Mockito.verify(arangoOperationsGood, times(2)).query(eq("RETURN 13"), any());
        Mockito.verify(dbaasArangoTemplate, times(1)).initArangoTemplate();
        Mockito.verify(dbaasArangoTemplate, times(1)).checkConnection(arangoTemplate);
    }

    @Test
    public void testConcurrentFail_OneCheckAndOneReinit() throws InterruptedException {
        int threadsCount = 10;
        Mockito.when(arangoTemplate.query(any(), any())).thenThrow(new RuntimeException("Fail all requests"));

        AtomicBoolean firstInit = new AtomicBoolean(true);
        ArangoTemplate newArangoTemplate = Mockito.mock(ArangoTemplate.class);
        Mockito.doAnswer(invocationOnMock -> {
            Thread.sleep(200);
            Field field = dbaasArangoTemplate.getClass().getDeclaredField("arangoTemplate");
            field.setAccessible(true);
            if (firstInit.get()) {
                firstInit.set(false);
                field.set(dbaasArangoTemplate, arangoTemplate);
            } else {
                Mockito.lenient().when(newArangoTemplate.query(eq("RETURN 42"), any())).thenReturn(arangoCursor42);
                Mockito.lenient().when(newArangoTemplate.query(eq("RETURN 13"), any())).thenReturn(arangoCursor13);
                field.set(dbaasArangoTemplate, newArangoTemplate);
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

        Mockito.verify(arangoTemplate, times(threadsCount)).query(eq("RETURN 13"), any());
        Mockito.verify(newArangoTemplate, times(threadsCount)).query(eq("RETURN 13"), any());
        Mockito.verify(dbaasArangoTemplate, times(2)).initArangoTemplate();
        Mockito.verify(dbaasArangoTemplate, times(1)).checkConnection(arangoTemplate);
        Mockito.verify(dbaasArangoTemplate, times(0)).checkConnection(newArangoTemplate);
    }

    @Test
    public void testSuccess_CheckAndRetryNotRequired() {
        Mockito.doAnswer(invocationOnMock -> {
            Thread.sleep(10);
            Field field = dbaasArangoTemplate.getClass().getDeclaredField("arangoTemplate");
            field.setAccessible(true);
            field.set(dbaasArangoTemplate, arangoTemplate);
            return null;
        }).when(dbaasArangoTemplate).initArangoTemplate();

        Mockito.when(arangoTemplate.query(any(), any())).thenReturn(arangoCursor13);

        ArangoCursor<Integer> query = dbaasArangoTemplate.query("RETURN 13", Integer.class);
        assertEquals(13, query.next());
        Mockito.verify(dbaasArangoTemplate, times(1)).initArangoTemplate();
        Mockito.verify(dbaasArangoTemplate, times(0)).checkConnection(arangoTemplate);
    }

    @Test
    public void testSuccess_WithRetry() {
        Mockito.doAnswer(invocationOnMock -> {
            Thread.sleep(10);
            Field field = dbaasArangoTemplate.getClass().getDeclaredField("arangoTemplate");
            field.setAccessible(true);
            field.set(dbaasArangoTemplate, arangoTemplate);
            return null;
        }).when(dbaasArangoTemplate).initArangoTemplate();

        Mockito.when(arangoTemplate.query(any(), any()))
                .thenThrow(new RuntimeException("Bad connection for direct call"))
                .thenThrow(new RuntimeException("Bad connection for check"))
                .thenAnswer(invocationOnMock -> arangoCursor13);
        ArangoCursor<Integer> query = dbaasArangoTemplate.query("RETURN 13", Integer.class);
        assertEquals(13, query.next());
        Mockito.verify(dbaasArangoTemplate, times(2)).initArangoTemplate();
        Mockito.verify(dbaasArangoTemplate, times(1)).checkConnection(arangoTemplate);
    }

    @Test
    public void testFail_CheckFailAndRetryFail() {
        Mockito.doAnswer(invocationOnMock -> {
            Field field = dbaasArangoTemplate.getClass().getDeclaredField("arangoTemplate");
            field.setAccessible(true);
            field.set(dbaasArangoTemplate, arangoTemplate);
            return null;
        }).when(dbaasArangoTemplate).initArangoTemplate();

        Mockito.when(arangoTemplate.query(any(), any())).thenThrow(new RuntimeException("Bad connection"));
        assertThrows(RuntimeException.class, () -> dbaasArangoTemplate.query("RETURN 13", Integer.class));
        Mockito.verify(arangoTemplate, times(2)).query(eq("RETURN 13"), any());
        Mockito.verify(dbaasArangoTemplate, times(2)).initArangoTemplate();
        Mockito.verify(dbaasArangoTemplate, times(1)).checkConnection(arangoTemplate);
    }

    @Test
    public void testFail_CheckOkAndWithoutReinitAndWithoutRetry() throws NoSuchFieldException, IllegalAccessException {
        Field field = dbaasArangoTemplate.getClass().getDeclaredField("arangoTemplate");
        field.setAccessible(true);
        field.set(dbaasArangoTemplate, arangoTemplate);

        Mockito.when(arangoTemplate.query(eq("RETURN 42"), any())).thenReturn(arangoCursor42);
        Mockito.when(arangoTemplate.query(eq("RETURN 13"), any())).thenThrow(new RuntimeException("Bad request"));

        assertThrows(RuntimeException.class, () -> dbaasArangoTemplate.query("RETURN 13", Integer.class));
        Mockito.verify(arangoTemplate, times(1)).query(eq("RETURN 13"), any());
        Mockito.verify(dbaasArangoTemplate, times(0)).initArangoTemplate();
        Mockito.verify(dbaasArangoTemplate, times(1)).checkConnection(any());
    }
}
