# Context propagation

Context-propagation framework is intended for propagating some value from one microservice to another. Additionally, the
library allows to store custom request data and get them where you want.  
Context-propagation is designed to propagate values in the following ways:

* Rest - Rest
* Rest - Messaging
* Messaging - Rest
* Messaging - Messaging

Also, the framework contains some useful methods for working with context such as propagating contexts to other threads,
create a snapshot and activate it sometime later. Also, you can create own contexts for propagating your data or
override existed for customization for your needs.

Design overview: [context-propagation diagram](./design.png)

* [Quarkus framework-contexts](#quarkus-framework-contexts)
* [How to write own context](#how-to-write-own-context)
* [How to override existed context](#how-to-write-own-context)

* [Quarkus context-propagation](#quarkus-context-propagation)

* [Context snapshots](#context-snapshots)
* [Thread context propagation](#thread-context-propagation)
    * [Context propagation through execution service](#thread-context-propagation-using-executeservice)
    * [Thread context propagation using Callable delegator](#thread-context-propagation-using-callable-delegator)
    * [Thread context propagation using Supplier delegator](#thread-context-propagation-using-supplier-delegator)
* [Context-propagation bom](#context-propagation-bom)

# Quarkus framework contexts

Framework provides contexts for propagating the following data:

* [Accept-Language](#accept-language);
* [Any custom headers](#allowed-headers);
* [API version](#api-version);
* [X-Request-Id](#x-request-id);
* [X-Version](#x-version);
* [Business-Request-Id](#business-request-id)

##### How to use

1) Add the framework-contexts dependency:

```xml

<dependency>
    <groupId>org.qubership.cloud.quarkus</groupId>
    <artifactId>framework-contexts</artifactId>
    <version>${context.propagation.version}</version>
</dependency>
```


##### Accept-Language

Accept-Language context allows propagating 'Accept-Language' headers from one microservice to another. To get context
value, you should call:

Access:

```java
    AcceptLanguageContextObject acceptLanguageContextObject=ContextManager.get(ACCEPT_LANGUAGE);
        String acceptLanguage=acceptLanguageContextObject.getAcceptedLanguages();
```

##### Allowed headers

Allows propagating any specified headers. To set a list of headers you should put either
`HEADERS_ALLOWED` environment or set the `headers.allowed` property. Property has more precedence than env.

Access:

```java
        AllowedHeadersContextObject allowedHeadersContextObject=ContextManager.get(ALLOWED_HEADER);
        Map<String, Object> allowedHeaders=allowedHeadersContextObject.getHeaders();
```

You just need to specify a list of headers in `application.properties`
in the `headers.allowed` property. For example:

```text
headers.allowed=myheader1,myheader2,...
```

Otherwise, you need to take care that this parameter is in System#property or environment.

##### API version

This context retrieves API version from an incoming request URL and stores it.

Access:

```java
        ApiVersionContextObject apiVersionContextObject=ContextManager.get(API_VERSION_CONTEXT_NAME);
        String apiVersion=apiVersionContextObject.getVersion();
```

If request URL does not contain API version then the context contains default value `v1`.

##### X-Request-Id

Propagates and allows to get `X-Request-Id` value. If an incoming request does not contains the `X-Request-Id`
header then a random value is generated.

Access:

```java
        XRequestIdContextObject xRequestIdContextObject=ContextManager.get(X_REQUEST_ID);
        String xRequestId=xRequestIdContextObject.getRequestId();
```

##### X-Version

Propagates and allows to get `X-Version` header.

Access:

```java
        XVersionContextObject xVersionContextObject=ContextManager.get(XVersionProvider.CONTEXT_NAME);
        String xVersion=xVersionContextObject.getXVersion();
```

##### Business-Request-Id

Propagates and allows to get `Business-Request-Id` header.
Value of header shouldn't be empty. If header is empty and value not set, propagation won't work.

Access:

```java
        String businessProcessId = BusinessProcessIdContext.get();
```

Set:

```java
        BusinessProcessIdContext.set(someID);
```


# How to write own context

There is an example of new context creation
in [here](./context-propagation-core/src/test/java/org/qubership/cloud/context/propagation/core/providers/xversion)

**At first,** implement your ContextObject class. ContextObject is a place where you can parse and store data from
IncomingContextData. IncomingContextData is an object where is located request context data.

**Note**! Implement `SerializableContext` if you want to propagate data from your context in outgoing request. You have
to override below function. It's aim to get values from context and put them into OutgoingContextData.

```java
public class ContextObject implements SerializableContext {
    @Override
    public void serialize(OutgoingContextData contextData) {
        contextData.set(SERIALIZATION_NAME, storedValue);
    }
}
```

**Note!** ContextObject should implement `DefaultValueAwareContext` if it contains default value. You have to override _
getDefault()_
function and return default value from it.

```java
@Override
public String getDefault(){
        return"default";
        }
```

**Secondly,** Strategy - this is a way how your context will be stored.

You can choose one from our default strategies or create a new one.

Default strategies for threadLocal are: `ThreadLocalDefaultStrategy`, `ThreadLocalWithInheritanceDefaultStrategy`.
ThreadLocalWithInheritanceDefaultStrategy supports propagation between Threads. Default strategy for Quarkus
is: `RestEasyDefaultStrategy`.

If you decided to use one of default strategies - then just go to Provider.

To implement your own strategy your class should implement `Strategy<ContextObject>` and override next functions:

* public void clear()           - to remove all stored info
* public ContextObject get()    - get stored ContextObject or exception if ContextObject is null.
* public void set(ContextObject value)  - set new ContextObject for storing
* public Optional<ContextObject> getSafe()      - get stored ContextObject without Exception

Instead of ContextObject insert name of your ContextObject class.

**Thirdly,** Provider - provides information about context to contextManager. You can use default provider or create
your own.

Default providers: `AbstractContextProviderOnInheritableThreadLocal`, `AbstractContextProviderOnThreadLocal`. There is
no default provider for Quarkus. If you decided to use one of the default providers all you need to override are this
two functions:

```java
/* The name of context. ContextName is unique key of context. By this name you can get or set context object in context.
 * Can't be registered more than one context in contextManager with the same name. <p>
 * Additionally, we strongly recommend to make method realization as final because class that overrides existed
 * context must have the same name.  */
@Override
public final String contextName(){
        return CONTEXT_NAME;
        }

/* The method creates contextObject. Context object may be initialized based on data from {@link IncomingContextData}
 * For example if context is serialized and propagated from microservice to microservice
 * then this method should describe how context object can be deserialized.
 * If incomingContextData is not null and there are not data relevant to this context, method should return null. */
@Override
public ContextObject provide(@Nullable IncomingContextData incomingContextData){
        return new ContextObject(incomingContextData);
        }
```

To create your own provider you should implement ContextProvider<ContextObject> and **mark provider class with
@RegisterProvider**

Also, you have to override several functions:

```java

@RegisterProvider
public class MyProvider implements ContextProvider<ContextObject> {
    // should return instance of your own strategy 
    // or instance of default strategy
    @Override
    public Strategy<ContextObject> strategy() {
        return this.strategy;
    }

    // Determined context level order. ContextManager sorts context providers by levels and
    // performs bulk operations (init, clear and so on) with contexts
    // with a lower level at first and then ascending levels.
    // If you don't care about the order of the context among other contexts then method can return 0 value.
    // Smaller will be done first
    @Override
    public int initLevel() {
        return 0;
    }

    // Determines which of several context providers with the same name should be used.
    // If there are several context providers with the same name
    // and their provider orders are equal then runtime exception will be.
    // We recommend to use 0 if you write your own and don't override existed context. <p>
    // If you override existed context then value should be multiple of 100. For example: 0, -100, -200 <p>
    // Context provider with smaller value wins.
    @Override
    public int providerOrder() {
        return 0;
    }


    // The name of context. ContextName is unique key of context. By this name you can get or set context object in context.
    // Can't be registered more than one context in contextManager with the same name. <p>
    // Additionally, we strongly recommend to make method realization as final because class that overrides existed
    // context must have the same name.
    @Override
    public final String contextName() {
        return CONTEXT_NAME;
    }

    // The method creates contextObject. Context object may be initialized based on data from {@link IncomingContextData}
    // For example if context is serialized and propagated from microservice to microservice
    // then this method should describe how context object can be deserialized.
    // If incomingContextData is not null and there are not data relevant to this context, method should return null.
    @Override
    public ContextObject provide(@Nullable IncomingContextData contextData) {
        return new ContextObject(contextData);
    }
}
```

# How to override existed context

It means that you can use default contexts (see [Quarkus framework context](#quarkus-framework-contexts)) but with other ways of
storage. 

To override existing context you should extend its default provider, mark new class with `@RegisterProvider` and
override next functions:

```java

@RegisterProvider
public class MyOverridedProvider extends MyProvider {
    Strategy<ContextObject> newStrategy = ...;

    @Override
    public Strategy<ContextObject> strategy() {
        return this.newStrategy;
    }

    @Override
    public int providerOrder() {
        return -100;
    }
}
```

**Important!** Make _providerOrder()_ return value less than default one. If you don't do that, `ContextManager` won't
be able to detect new Provider and will use default one. Remember, that providerOrder() should be multiple of 100.


# Quarkus context propagation

This module contains filters and interceptors for quarkus application.

### How to use

All that you need is to add the below dependency.

```xml

<dependency>
    <groupId>org.qubership.cloud</groupId>
    <artifactId>context-propagation-quarkus</artifactId>
    <version>${context.propagation.version}</version>
</dependency>
```

And write own or use our `Quarkus framework contexts`.


### How to use

1) You should add the below dependency.

```xml

<dependency>
    <groupId>org.qubership.cloud</groupId>
    <artifactId>framework-contexts-quarkus</artifactId>
    <version>${context.propagation.version}</version>
</dependency>
```

2) If we use `Allowed headers` context you should specify a list of headers in `application.properties` in the
   `quarkus.headers.allowed` property.

```text
quarkus.headers.allowed=myheader1,myheader2,...
```

# Context snapshots

There is a possibility to create a context snapshot - to remember current contexts' data and after to store it. To get stored data you have to
execute `ContextManager.executeWithContext()`.

```java
    AcceptLanguageContext.set(initialContextValue);
    Map<String, Object> contextSnapshot=ContextManager.createContextSnapshot();

    AcceptLanguageContext.set(newContextValue);

    ContextManager.executeWithContext(contextSnapshot,()->{
        assertEquals(initialContextValue, AcceptLanguageContext.get()); // <-- true
        return null;
    });
```
In order to restore you have to perform `ContextManager.activateContextSnapshot(contextSnapshot)`

# Thread context propagation
Thread context propagation functionality allows performing users' task in a dedicated thread in a specific context. Context can be original or snapshot.

## Thread context propagation using executeService

If you want to use Executor Service with our contexts, you need to wrap executor with our
delegator `ContextAwareExecutorService`. In this case we guarantee correct context propagation over threads.

```java
    final ExecutorService simpleExecutor=new ContextAwareExecutorService(Executors.newFixedThreadPool(2));
```
`ContextAwareExecutorService` has two type of constructors. One of them takes context snapshot, and the other takes only executorService delegate.
If we use which takes and pass context snapshot then all submitted task will be performed in this specific context. If you don't pass context snapshot then we create full
context snapshot by themselves and will be performed all task in this context.

## Thread context propagation using Callable delegator

There are cases when you want to use original `ExecutorService` as dedicated thread pool and use tasks which run in specific context. In this way
you can use `ContextPropagationCallable` delegator. This delegator takes context snapshot object and `Callable` delegate. When task is executed the
delegate will be performed in the passed context snapshot.

 ```java
    ContextPropagationCallable contextPropagationCallable = new ContextPropagationCallable(ContextManager.createContextSnapshot(), delegate);

    ExecutorService executorService = Executors.newSingleThreadExecutor();
    executorService.submit(contextPropagationCallable).get();
```

## Thread context propagation using Supplier delegator

Sometimes, you may use `CompletableFuture` class and this way it would be convenient to use `ContextPropagationSupplier` delegator. This class takes delegate and context snapshot.
If you want to perform a task in a current context then you can perform the following code:

 ```java
    ContextPropagationSupplier contextPropagationSupplier = new ContextPropagationSupplier(ContextManager.createContextSnapshot(), delegate);
```

# SmallRye messaging context propagation

Module [messaging-context](messaging-context/) provides support for context propagation in smallrye messaging. It dumps execution context data to message
metadata (usually headers) and restore execution context from received message before calling `onMessage` callback.

Look at [README.md](messaging-context/README.md) for details. 