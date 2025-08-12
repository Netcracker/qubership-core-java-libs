package com.netcracker.cloud.restlegacy.restclient.error;

import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.Errors;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class UtilException {

    /**
     * .forEach(rethrowConsumer(name -&gt; System.out.println(Class.forName(name)))); or .forEach(rethrowConsumer(ClassNameUtil::println));
     */
    public static <T> Consumer<T> rethrowConsumer(Consumer_WithExceptions<T> consumer) {
        return t -> {
            try {
                consumer.accept(t);
            } catch (Exception exception) {
                throwAsUnchecked(exception);
            }
        };
    }

    /**
     * .map(rethrowFunction(name -&gt; Class.forName(name))) or .map(rethrowFunction(Class::forName))
     */
    public static <T, R> Function<T, R> rethrowFunction(Function_WithExceptions<T, R> function) {
        return t -> {
            try {
                return function.apply(t);
            } catch (Exception exception) {
                throwAsUnchecked(exception);
                return null;
            }
        };
    }

    /**
     * rethrowSupplier(() -&gt; new StringJoiner(new String(new byte[]{77, 97, 114, 107}, "UTF-8"))),
     */
    public static <T> Supplier<T> rethrowSupplier(Supplier_WithExceptions<T> function) {
        return () -> {
            try {
                return function.get();
            } catch (Exception exception) {
                throwAsUnchecked(exception);
                return null;
            }
        };
    }

    /**
     * uncheck(() -&gt; Class.forName("xxx"));
     */
    public static void uncheck(Runnable_WithExceptions t) {
        try {
            t.accept();
        } catch (Exception exception) {
            throwAsUnchecked(exception);
        }
    }

    /**
     * uncheck(() -&gt; Class.forName("xxx"));
     */
    public static <R> R uncheck(Supplier_WithExceptions<R> supplier) {
        try {
            return supplier.get();
        } catch (Exception exception) {
            throwAsUnchecked(exception);
            return null;
        }
    }

    /**
     * uncheck(Class::forName, "xxx");
     */
    public static <T, R> R uncheck(Function_WithExceptions<T, R> function, T t) {
        try {
            return function.apply(t);
        } catch (Exception exception) {
            throwAsUnchecked(exception);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public static <E extends Throwable> void throwAsUnchecked(Exception exception) throws E {
        throw (E) exception;
    }

    private static void rejectErrorException(final Errors errors, final ErrorException e) {
        errors.reject(e.getErrorType().getMessageCode(), e.getVars(), "");
    }

    private static void rejectProxyErrorException(final Errors errors, final ProxyErrorException e) {
        if (e.getResponseEntity().getBody() != null) {
            errors.reject("error.validation.proxy",
                    new String[]{e.getResponseEntity()
                            .getBody()
                            .getErrors()
                            .stream()
                            .map(ErrorsDescription.ErrorDescription::getErrorMessage)
                            .collect(Collectors.joining("; "))},
                    "");
        }
    }

    private static void rejectException(final Errors errors, final Exception e) {
        errors.reject("error.validation.proxy", new String[]{!StringUtils.isEmpty(e.getMessage()) ? e.getMessage() : e.getClass().getSimpleName()}, "");
    }

    /**
     * .forEach(rejectConsumer(errors, t -&gt; someFunctionWithException(t))); or .forEach(errors, rejectConsumer(ClassNameUtil::println));
     */
    public static <T> Consumer<T> rejectConsumer(final Errors errors, final Consumer_WithExceptions<T> consumer) {
        return t -> {
            try {
                consumer.accept(t);
            } catch (ProxyErrorException e) {
                rejectProxyErrorException(errors, e);
            } catch (ErrorException e) {
                rejectErrorException(errors, e);
            } catch (Exception e) {
                rejectException(errors, e);
            }
        };
    }

    /**
     * rejectException(errors, t -&gt; { someFunctionWithException(t); }); or rejectException(errors, SomeClass::someFunctionWithException());
     */
    public static <T> void rejectException(final Errors errors, final Consumer_WithExceptions<T> consumer) {
        rejectConsumer(errors, consumer).accept(null);
    }

    /**
     * .map(rejectFunction(errors, name -&gt; Class.forName(name))) or .map(rejectFunction(Class::forName))
     */
    public static <T, R> Function<T, R> rejectFunction(final Errors errors, final Function_WithExceptions<T, R> function) {
        return t -> {
            try {
                return function.apply(t);
            } catch (ProxyErrorException e) {
                rejectProxyErrorException(errors, e);
            } catch (ErrorException e) {
                rejectErrorException(errors, e);
            } catch (Exception e) {
                rejectException(errors, e);
            }
            return null;
        };
    }

    /**
     * rejectFuncException(errors, t -&gt; { someFunctionWithException(t); }); or rejectFuncException(errors, SomeClass::someFunctionWithException());
     */
    public static <T, R> R rejectFuncException(final Errors errors, final Function_WithExceptions<T, R> function) {
        return rejectFunction(errors, function).apply(null);
    }

    @FunctionalInterface
    public interface Consumer_WithExceptions<T> {
        void accept(T t) throws Exception;
    }

    @FunctionalInterface
    public interface Function_WithExceptions<T, R> {
        R apply(T t) throws Exception;
    }

    @FunctionalInterface
    public interface Supplier_WithExceptions<T> {
        T get() throws Exception;
    }

    @FunctionalInterface
    public interface Supplier_WithException<T, E extends Exception> {
        T get() throws E;
    }

    @FunctionalInterface
    public interface Runnable_WithExceptions {
        void accept() throws Exception;
    }

}
