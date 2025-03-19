![Coverage](https://sonarcloud.io/api/project_badges/measure?metric=coverage&project=Netcracker_qubership-core-context-propagation)
![duplicated_lines_density](https://sonarcloud.io/api/project_badges/measure?metric=duplicated_lines_density&project=Netcracker_qubership-core-context-propagation)
![vulnerabilities](https://sonarcloud.io/api/project_badges/measure?metric=vulnerabilities&project=Netcracker_qubership-core-context-propagation)
![bugs](https://sonarcloud.io/api/project_badges/measure?metric=bugs&project=Netcracker_qubership-core-context-propagation)
![code_smells](https://sonarcloud.io/api/project_badges/measure?metric=code_smells&project=Netcracker_qubership-core-context-propagation)

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

* [Framework-contexts](#framework-contexts)
* [How to write own context](#how-to-write-own-context)
* [How to override existed context](#how-to-write-own-context)
* [Migration to Jandex context provider discovery](#migration-to-jandex-context-provider-discovery)
* [Getting list of propagated header names](#getting-set-of-propagated-header-names)
* [Spring context-propagation](#spring-context-propagation)
* [Spring resttemplate-context-propagation](#spring-resttemplate-context-propagation)
* [Spring webclient-context-propagation](#spring-webclient-context-propagation)
* [Spring kafka-context-propagation](#spring-kafka-context-propagation)
* [Spring rabbimq-context-propagation](#spring-rabbitmq-context-propagation)

* [Context snapshots](#context-snapshots)
* [Thread context propagation](#thread-context-propagation)
    * [Context propagation through execution service](#thread-context-propagation-using-executeservice)
    * [Thread context propagation using Callable delegator](#thread-context-propagation-using-callable-delegator)
    * [Thread context propagation using Supplier delegator](#thread-context-propagation-using-supplier-delegator)
* [Context-propagation bom](#context-propagation-bom)
* [Jandex Test Extension](#jandex-test-extension)

# Framework contexts

Framework provides contexts for propagating the following data:

* [Accept-Language](#accept-language);
* [Any custom headers](#allowed-headers);
* [API version](#api-version);
* [X-Request-Id](#x-request-id);
* [X-Version](#x-version);
* [X-Version-Name](#x-version-name);
* [X-Nc-Client-Ip](#x-nc-client-ip)
* [Business-Process-Id](#business-process-id)
* [Originating-Bi-Id](#originating-bi-id)

##### How to use

1) Add the framework-contexts dependency:

```xml

<dependency>
    <groupId>org.qubership.cloud</groupId>
    <artifactId>framework-contexts</artifactId>
    <version>${context.propagation.version}</version>
</dependency>
```

2) Add one from the propagation library:

[Spring context-propagation](#spring-context-propagation): if you need only to handle incoming REST request.
[Spring resttemplate-context-propagation](#spring-resttemplate-context-propagation): If you need to handle outgoing
request and `resttemplate` is used as restclient. Also you can use the library in Rest - Rest case.

* [Spring webclient-context-propagation](#spring-webclient-context-propagation)If you need to handle outgoing request
  and `webclient` is used as restclient. Also you can use the library in Rest - Rest case.
* [Spring kafka-context-propagation](#spring-kafka-context-propagation) If you need to continuously propagate contexts through Kafka messaging.
* [Spring rabbimq-context-propagation](#spring-rabbitmq-context-propagation) If you need to continuously propagate contexts through RabbitMQ messaging.

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

If we use one of the [Spring context-propagation](#spring-context-propagation),
[Spring resttemplate-context-propagation](#spring-resttemplate-context-propagation),
[Spring webclient-context-propagation](#spring-webclient-context-propagation) libraries and in your service there is
an `@EnableSpringContextProvider` annotation then you can just specify a list of headers in `application.properties`
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

Please note that the context is only initialized in the presence of an incoming request with the `X-Version` header

##### X-Version-Name

Propagates and allows to get `X-Version-Name` header.

Access:

```java
        XVersionNameContext.get();
```

Set:

```java
        XVersionNameContext.set(someXVersionName);
```

##### X-Nc-Client-Ip

Propagates and allows to get `X-Nc-Client-Ip` header.
As init value can accept `X-Forwarded-For` header value.

Access:

```java
        ClientIPContext.get();
```

Set:

```java
        ClientIPContext.set(someClientIp);
```

##### Business-Request-Id

Propagates and allows to get `Business-Process-Id` header.
Value of header shouldn't be empty. If header is empty and value not set, propagation won't work.

Access:

```java
        String businessProcessId = BusinessProcessIdContext.get();
```

Set:

```java
        BusinessProcessIdContext.set(someID);
```

##### originating-request-id

Propagates and allows to get `originating-bi-id` header.
If header is not set, propagation won't work.

Access:

```java
        String originatingBiId = OriginatingBiIdContext.get();
```

Set:

```java
        OriginatingBiIdContext.set(someID);
```

# How to write own context

There is an example of new context creation
in [here](./context-propagation-core/src/test/java/com/qubership/cloud/context/propagation/core/providers/xversion)

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

*SerializableDataContext*  
Context can be serialized to json format. In order to add this possibility your context object should implement
**SerializableDataContext** interface. This interface provides only one method:

```java
Map<String, Object> getSerializableContextData();
```

where Map<String, Object> is map of data which are needed to build context during deserialization step.  
Meanwhile, Object must be a simple primitive type, such as int, bool, string and so on which can be simply serialized
and
deserialized by Jackson.

**Secondly,** Strategy - this is a way how your context will be stored.

You can choose one from our default strategies or create a new one.

Default strategies for threadLocal are: `ThreadLocalDefaultStrategy`, `ThreadLocalWithInheritanceDefaultStrategy`.
ThreadLocalWithInheritanceDefaultStrategy supports propagation between Threads.

If you decided to use one of default strategies - then just go to Provider.

To implement your own strategy your class should implement `Strategy<ContextObject>` and override next functions:

* public void clear()           - to remove all stored info
* public ContextObject get()    - get stored ContextObject or exception if ContextObject is null.
* public void set(ContextObject value)  - set new ContextObject for storing
* public Optional<ContextObject> getSafe()      - get stored ContextObject without Exception

Instead of ContextObject insert name of your ContextObject class.

**Thirdly,** Provider - provides information about context to contextManager. You can use default provider or create
your own.

Default providers: `AbstractContextProviderOnInheritableThreadLocal`, `AbstractContextProviderOnThreadLocal`. If you decided to use one of the default providers all you need to override are this
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

**Finally** you need to create jandex index to make your provide discoverable by `ContextManager`. You can do this by adding jandex maven plugin to build plugins section in your pom.xml:
```
<plugin>
    <groupId>io.smallrye</groupId>
    <artifactId>jandex-maven-plugin</artifactId>
    <version>${jandex.version}</version>
    <executions>
        <execution>
            <id>make-index</id>
            <goals>
                <goal>jandex</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

# How to override existed context

It means that you can use default contexts (see [Framework context](#framework-contexts)) but with other ways of
storage. For example, default way to store contexts in Spring is ThreadLocalWithInheritanceDefaultStrategy. We can
change it by overriding existing contexts.

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

Also create jandex index using jandex maven plugin to make your provider discoverable by `ContextManager`:
```
<plugin>
    <groupId>io.smallrye</groupId>
    <artifactId>jandex-maven-plugin</artifactId>
    <version>${jandex.version}</version>
    <executions>
        <execution>
            <id>make-index</id>
            <goals>
                <goal>jandex</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

# Migration to Jandex context provider discovery

Starting from Context Propagation version 6.2.0 old approach with context provider discovery using reflection is getting deprecated.
Jandex will be used for context provider discovery instead. To migrate your contexts to the new approach you need to add jandex plugin call in your `pom.xml`:

```
<plugin>
    <groupId>io.smallrye</groupId>
    <artifactId>jandex-maven-plugin</artifactId>
    <version>${jandex.version}</version>
    <executions>
        <execution>
            <id>make-index</id>
            <goals>
                <goal>jandex</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```
System property `core.contextpropagation.providers.reflection-discovery.disabled` can be used 
to preemptively disable old approach with class path scanning using reflection in order to validate 
that jandex discovery is working as intended after the migration. 
By setting it to `true` reflection won't be used to load context providers and only providers discovered in jandex index will be available.

# Getting set of propagated header names

To get the set of propagated header names use the following method:

```java
Set<String> downstreamHeaders = RequestContextPropagation.getDownstreamHeaders();
```

This method produces a set of header names that are used for propagation by initialized contexts.

# Spring context propagation

The library allows to handle incoming requests and fill registered contexts. The library contains only filter and is
suitable for terminated microservice(which accept a request but does not send)

#### How to use

1) Add the dependency:

```xml

<dependency>
    <groupId>org.qubership.cloud</groupId>
    <artifactId>context-propagation-spring-common</artifactId>
    <version>${context.propagation.version}</version>
</dependency>
```

2) Put `@EnableSpringContextProvider` annotation to your configuration class

## Spring resttemplate-context-propagation

You should use this module if you are working with Spring and REST template.

If your way of service communication is REST -> REST, then you have to mark your configuration class with
@EnableResttemplateContextProvider (Already contain @EnableSpringContextProvider annotation)
If it is REST -> Messaging, then you need only @EnableSpringContextProvider annotation for your configuration class.

`@EnableResttemplateContextProvider` - enables REST interceptor for rest template. `@EnableSpringContextProvider` -
enables REST filter.

```java

@Configuration
@EnableResttemplateContextProvider
public class MyConfig {
}
```

Then you should autowire your interceptor bean:

```java

@Service
public class MyService {
    @Autowired
    private SpringContextProviderFilter springContextProviderFilter;

    @Autowired
    private SpringRestTemplateInterceptor springRestTemplateInterceptor;

    public void foo() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setInterceptors(springRestTemplateInterceptor);
    }
}

```

### How to use

If you want to use it you just need to add the below dependencies and add @EnableResttemplateContextProvider annotation
to your configuration class(Already contain @EnableSpringContextProvider annotation).

```xml

<dependency>
    <groupId>org.qubership.cloud</groupId>
    <artifactId>context-propagation-spring-resttemplate</artifactId>
    <version>${context.propagation.version}</version>
</dependency>
```

## Spring webclient-context-propagation

You should use this module if you are working with Spring and web client.

If your way of service communication is REST -> REST, then you have to mark your configuration class with
@EnableWebclientContextProvider(Already contain @EnableSpringContextProvider annotation)
If it is REST -> Messaging, then you need only @EnableSpringContextProvider annotation for your configuration class.

`@EnableWebclientContextProvider` - enables REST interceptor for web client. `@EnableSpringContextProvider` - enables
REST filter.

```java

@Configuration
@EnableWebclientContextProvider
public class MyConfig {
}
```

Then you should autowire your interceptor bean:

```java

@Service
public class MyService {
    @Autowired
    private SpringContextProviderFilter springContextProviderFilter;

    @Autowired
    private SpringWebClientInterceptor springWebClientInterceptor;

    public void foo() {
        WebClient webClient = new WebClient();
        webClient.setInterceptors(springWebClientInterceptor);
    }
}
```

Autowire it somewhere and inject into interceptors configuration for web client.

### How to use

If you want to use it you just need to add the below dependencies and add @EnableWebclientContextProvider annotation to
your configuration class(Already contain @EnableSpringContextProvider annotation).

```xml

<dependency>
    <groupId>org.qubership.cloud</groupId>
    <artifactId>context-propagation-spring-webclient</artifactId>
    <version>${context.propagation.version}</version>
</dependency>
```

# Spring Kafka context propagation
Context propagation through Kafka messaging is done by using message headers. 

On Producer side context will be serialized to message headers as is. On Listener side context restoration also 
performed by deserializing header values from incoming message into contexts.

- Add dependency
```xml
  <dependency>
     <groupId>org.qubership.cloud</groupId>
     <artifactId>context-propagation-spring-kafka</artifactId>
     <version>${context-propagation.kafka.version}</version>
  </dependency>
```
- Add `@EnableKafkaContextPropagation` annotation on Application/Configuration class 

# Spring RabbitMQ context propagation
Context propagation through RabbitMQ messaging is done by using message headers.

On Producer side context will be serialized to message headers as is. The one exception is `x-version` header. To support 
RabbitMQ Blue/Green, we add alias `version` header for `x-version` header during serialization. 

On Listener side context restoration also performed by deserializing header values from incoming message into contexts.
Alias header `version` just ignored during deserialization.

- Add dependency
```xml
  <dependency>
     <groupId>org.qubership.cloud</groupId>
     <artifactId>context-propagation-spring-rabbit</artifactId>
     <version>${context-propagation.kafka.version}</version>
  </dependency>
```
- Add `@EnableRabbitContextPropagation` annotation on Application/Configuration class


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

Sometimes, you may use `CompletableFuture` class and this way it would be convenient to use `ContextPropagationSupplier`
delegator. This class takes delegate and context snapshot.
If you want to perform a task in a current context then you can perform the following code:

 ```java
    ContextPropagationSupplier contextPropagationSupplier=new ContextPropagationSupplier(ContextManager.createContextSnapshot(),delegate);
```

## Context serialization/deserialization from String

All context which implement ```SerializableDataContext``` interface can be serialized to string. In order to get
snapshot serialized context data
you should call `ContextManager.getSerializableContextData()` method. Also you can
use `ContextManager.getSerializableContextData(java.util.Set<java.lang.String>)` and
pass list of contexts which you don't need to be serialized. These methods return `Map<String, Map<String, Object>>`
which can be
easily serialized by Jackson.  
To activate context from serializable data you have to call `ContextManager.activateWithSerializableContextData` and
pass data which was collected with
`ContextManager.getSerializableContextData()` previously.

# Context-propagation bom

This is a BOM which contains all necessary context propagation libraries.

#### Usage

Add the following artifact to your POM:

```xml
 <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.qubership.cloud</groupId>
                <artifactId>context-propagation-bom</artifactId>
                <version>{VERSION}</version>
                <scope>import</scope>
                <type>pom</type>
            </dependency>
        </dependencies>
    </dependencyManagement>
```

After it, you can add any library from `dependencyManagement` from [POM](./context-propagation-bom/pom.xml) without specifying a version. For example:

```xml
    <dependency>
        <groupId>org.qubership.cloud</groupId>
        <artifactId>context-propagation-spring-common</artifactId>
    </dependency>
    <dependency>
        <groupId>org.qubership.cloud</groupId>
        <artifactId>context-propagation-spring-resttemplate</artifactId>
    </dependency>
```

List of supported libraries:
```
    * context-propagation-core
    * framework-contexts
    * context-propagation-spring-common
    * context-propagation-spring-rabbitmq
    * context-propagation-spring-resttemplate
    * context-propagation-spring-webclient
```

# Jandex test extension

`context-propagation-test-extensions` module provides Junit extension that can help running context tests from your IDE.
It addresses the problem that maven plugins execution can be skipped when running tests directly from IDE. This leads to issue
that jandex index isn't built and context defined in your module aren't loaded. `JandexContextLoaderExtension` Junit resolves
this problem by building jandex index and loading contexts from it before tests are executed. You can add it to your tests using standard
Junit annotation `@ExtendWith`:
```
@ExtendWith(JandexContextLoaderExtension.class)
class SampleContextTest {
    ...
}
```