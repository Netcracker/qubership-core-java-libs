package org.qubership.cloud.restlegacy.restclient.error;

import org.junit.Test;
import org.springframework.validation.Errors;
import org.springframework.validation.MapBindingResult;

import java.util.*;
import java.util.stream.Collectors;

import static org.qubership.cloud.restlegacy.restclient.error.UtilException.*;
import static org.junit.Assert.*;

public class UtilExceptionTest {

    @Test
    public void testRethrowConsumer() {
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

    @Test(expected = Exception.class)
    public void testRethrowConsumerWhenExceptionAppears() {
        final List<String> result = new ArrayList<>();
        result.add("test");
        result.add("test2");

        result.forEach(rethrowConsumer(element -> {
            if ("test2".equals(element)) {
                throw new Exception();
            }
        }));
    }

    @Test
    public void testRethrowFunction() {
        final List<String> testData = new ArrayList<>();
        testData.add("test");
        testData.add("test2");

        List<String> actualResult = testData.stream()
                .map(rethrowFunction(str -> str + "*"))
                .collect(Collectors.toList());

        assertEquals(2, actualResult.size());
        assertEquals("test*", actualResult.get(0));
        assertEquals("test2*", actualResult.get(1));
    }

    @Test(expected = Exception.class)
    public void testRethrowFunctionWithException() {
        final List<String> testData = new ArrayList<>();
        testData.add("test");
        testData.add("test2");

        testData.stream()
                .map(rethrowFunction(str -> {
                    if ("test2".equals(str)) {
                        throw new Exception();
                    }
                    return str + "*";
                }))
                .collect(Collectors.toList());
    }

    @Test
    public void testRethrowSupplier() {
        final StringJoiner stringJoiner = rethrowSupplier(() -> new StringJoiner(",")).get()
                .add("test")
                .add("test2");
        assertEquals("test,test2", stringJoiner.toString());
    }

    @Test(expected = Exception.class)
    public void testRethrowSupplierWithException() {
        rethrowSupplier(() -> {
            throw new Exception();
        }).get();
    }

    @Test
    public void testUncheck() {
        final Class clazz = uncheck(() -> UtilExceptionTest.class);
        assertNotNull(clazz);
        assertEquals("UtilExceptionTest", clazz.getSimpleName());
    }

    @Test
    public void testUncheckRunnable() {
        List<String> list = new ArrayList<>();
        uncheck(() -> {
            list.add("test");
        });
        assertEquals(1, list.size());
        assertEquals("test", list.get(0));
    }

    @Test
    public void testUncheckFunction() {
        final Class clazz = uncheck(() -> UtilExceptionTest.class);
        assertNotNull(clazz);
        assertEquals("UtilExceptionTest", clazz.getSimpleName());
    }

    @Test(expected = Exception.class)
    public void testUncheckFunctionWithException() {
        uncheck((classForName -> {
            Class clazzTmp = Class.forName("org.qubership.cloud.microserviceframework.error.UtilExceptionTest");
            if ("UtilExceptionTest".equals(clazzTmp.getSimpleName())) {
                throw new Exception();
            }
            return clazzTmp;
        }), "org.qubership.cloud.microserviceframework.error.UtilExceptionTest");
    }

    @Test(expected = Exception.class)
    public void testUncheckRunnableWithException() {
        uncheck(() -> {
            throw new Exception();
        });
    }

    @Test(expected = Exception.class)
    public void testUncheckWithException() {
        uncheck(() -> {
            Class clazzTmp = Class.forName("org.qubership.cloud.microserviceframework.error.UtilExceptionTest");
            if ("UtilExceptionTest".equals(clazzTmp.getSimpleName())) {
                throw new Exception();
            }
            return clazzTmp;
        });
    }

    @Test
    public void testRejectConsumer() {
        Errors errors = new MapBindingResult(new HashMap<>(), "testObject");
        final List<String> testData = new ArrayList<>();
        testData.add("test");
        testData.add("test2");

        testData.forEach(rejectConsumer(errors, data -> {
        }));
        assertEquals(0, errors.getErrorCount());
    }

    @Test
    public void testRejectConsumerWithException() {
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
    public void testRejectFunction() {
        Errors errors = new MapBindingResult(new HashMap<>(), "testObject");
        List<String> data = Collections.singletonList(UtilExceptionTest.class.getName());
        List<Class> classes = data.stream()
                .map(rejectFunction(errors, Class::forName))
                .collect(Collectors.toList());
        assertEquals(1, classes.size());
        assertEquals("UtilExceptionTest", classes.get(0).getSimpleName());
    }

    @Test
    public void testRejectFunctionWithExceptions() {
        Errors errors = new MapBindingResult(new HashMap<>(), "testObject");
        final List<String> testData = new ArrayList<>();
        testData.add("test");
        testData.add("test2");
        testData.add("test3");
        List<Class> classes = testData.stream()
                .map(rejectFunction(errors, str -> {
                    switch (str) {
                        case "test": {
                            return UtilExceptionTest.class;
                        }
                        case "test2": {
                            throw new ErrorException(new Exception(), ErrorType.INTERNAL_SERVER_ERROR);
                        }
                        case "test3": {
                            throw new ProxyErrorException(new Exception(), "some-test-url");
                        }
                        default:
                            return null;
                    }
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
