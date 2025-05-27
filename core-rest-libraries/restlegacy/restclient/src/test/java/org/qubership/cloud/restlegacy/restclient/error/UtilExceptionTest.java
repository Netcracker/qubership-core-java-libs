package org.qubership.cloud.restlegacy.restclient.error;

import org.junit.jupiter.api.Test;
import org.springframework.validation.Errors;
import org.springframework.validation.MapBindingResult;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.qubership.cloud.restlegacy.restclient.error.UtilException.*;

class UtilExceptionTest {

    @Test
    void testRethrowConsumer() {
        final List<String> result = new ArrayList<>();
        result.add("test");
        result.add("test2");

        final List<String> actualResult = new ArrayList<>();

        result.forEach(rethrowConsumer(element -> {
            if ("test3".equals(element)) {
                throw new Exception();
            }
            actualResult.add(element);
        }));

        assertEquals("test", actualResult.get(0));
    }

    @Test
    void testRethrowConsumerWhenExceptionAppears() {
        final List<String> result = new ArrayList<>();
        result.add("test");
        result.add("test2");
        assertThrows(Exception.class, () -> result.forEach(rethrowConsumer(element -> {
            if ("test2".equals(element)) {
                throw new Exception();
            }
        })));
    }

    @Test
    void testRethrowFunction() {
        final List<String> testData = new ArrayList<>();
        testData.add("test");
        testData.add("test2");

        List<String> actualResult = testData.stream()
                .map(rethrowFunction(str -> str + "*"))
                .toList();

        assertEquals(2, actualResult.size());
        assertEquals("test*", actualResult.get(0));
        assertEquals("test2*", actualResult.get(1));
    }

    @Test
    void testRethrowFunctionWithException() {
        final List<String> testData = new ArrayList<>();
        testData.add("test");
        testData.add("test2");

        assertThrows(Exception.class, () -> testData.stream()
                .map(rethrowFunction(str -> {
                    if ("test2".equals(str)) {
                        throw new Exception();
                    }
                    return str + "*";
                }))
                .toList());
    }

    @Test
    void testRethrowSupplier() {
        final StringJoiner stringJoiner = rethrowSupplier(() -> new StringJoiner(",")).get()
                .add("test")
                .add("test2");
        assertEquals("test,test2", stringJoiner.toString());
    }

    @Test
    void testRethrowSupplierWithException() {
        assertThrows(Exception.class, () -> rethrowSupplier(() -> {
            throw new Exception();
        }).get());
    }

    @Test
    void testUncheck() {
        final Class<UtilExceptionTest> clazz = uncheck(() -> UtilExceptionTest.class);
        assertNotNull(clazz);
        assertEquals("UtilExceptionTest", clazz.getSimpleName());
    }

    @Test
    void testUncheckRunnable() {
        List<String> list = new ArrayList<>();
        uncheck(() -> {
            list.add("test");
        });
        assertEquals(1, list.size());
        assertEquals("test", list.get(0));
    }

    @Test
    void testUncheckFunction() {
        final Class<UtilExceptionTest> clazz = uncheck(() -> UtilExceptionTest.class);
        assertNotNull(clazz);
        assertEquals("UtilExceptionTest", clazz.getSimpleName());
    }

    @Test
    void testUncheckFunctionWithException() {
        assertThrows(Exception.class, () -> uncheck((classForName -> {
            Class<?> clazzTmp = Class.forName("org.qubership.cloud.microserviceframework.error.UtilExceptionTest");
            if ("UtilExceptionTest".equals(clazzTmp.getSimpleName())) {
                throw new Exception();
            }
            return clazzTmp;
        }), "org.qubership.cloud.microserviceframework.error.UtilExceptionTest"));
    }

    @Test
    void testUncheckRunnableWithException() {
        assertThrows(Exception.class, () -> uncheck(() -> {
            throw new Exception();
        }));
    }

    @Test
    void testUncheckWithException() {
        assertThrows(Exception.class, () -> uncheck(() -> {
            Class<?> clazzTmp = Class.forName("org.qubership.cloud.microserviceframework.error.UtilExceptionTest");
            if ("UtilExceptionTest".equals(clazzTmp.getSimpleName())) {
                throw new Exception();
            }
            return clazzTmp;
        }));
    }

    @Test
    void testRejectConsumer() {
        Errors errors = new MapBindingResult(new HashMap<>(), "testObject");
        final List<String> testData = new ArrayList<>();
        testData.add("test");
        testData.add("test2");

        testData.forEach(rejectConsumer(errors, data -> {
        }));
        assertEquals(0, errors.getErrorCount());
    }

    @Test
    void testRejectConsumerWithException() {
        Errors errors = new MapBindingResult(new HashMap<>(), "testObject");
        final List<String> testData = new ArrayList<>();
        testData.add("test");
        testData.add("test2");
        testData.add("test3");

        testData.forEach(rejectConsumer(errors, data -> {
            switch (data) {
                case "test": {
                    throw new Exception();
                }
                case "test2": {
                    throw new ErrorException(new Exception(), ErrorType.INTERNAL_SERVER_ERROR);
                }
                case "test3": {
                    throw new ProxyErrorException(new Exception(), "some-test-url");
                }
            }
        }));
        assertEquals(3, errors.getErrorCount());
        assertEquals("Exception", Objects.requireNonNull(errors.getAllErrors().get(0).getArguments())[0]);
        assertEquals("error.internal_server_error", errors.getAllErrors().get(1).getCode());
        assertEquals("error.validation.proxy", errors.getAllErrors().get(2).getCode());
    }

    @Test
    void testRejectFunction() {
        Errors errors = new MapBindingResult(new HashMap<>(), "testObject");
        List<String> data = Collections.singletonList(UtilExceptionTest.class.getName());
        List<Class<?>> classes = data.stream()
                .map(rejectFunction(errors, Class::forName))
                .collect(Collectors.toList());
        assertEquals(1, classes.size());
        assertEquals("UtilExceptionTest", classes.get(0).getSimpleName());
    }

    @Test
    void testRejectFunctionWithExceptions() {
        Errors errors = new MapBindingResult(new HashMap<>(), "testObject");
        final List<String> testData = new ArrayList<>();
        testData.add("test");
        testData.add("test2");
        testData.add("test3");
        List<Class<?>> classes = testData.stream()
                .map(rejectFunction(errors, str -> switch (str) {
                    case "test" -> UtilExceptionTest.class;
                    case "test2" -> throw new ErrorException(new Exception(), ErrorType.INTERNAL_SERVER_ERROR);
                    case "test3" -> throw new ProxyErrorException(new Exception(), "some-test-url");
                    default -> null;
                }))
                .collect(Collectors.toList());

        assertEquals(3, classes.size());
        assertEquals("UtilExceptionTest", classes.get(0).getSimpleName());
        assertNull(classes.get(1));
        assertNull(classes.get(2));
        assertEquals(2, errors.getErrorCount());
        assertEquals("error.internal_server_error", errors.getAllErrors().get(0).getCode());
        assertEquals("error.validation.proxy", errors.getAllErrors().get(1).getCode());
    }
}
