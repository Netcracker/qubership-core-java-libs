[![Maven build](https://github.com/Netcracker/qubership-core-junit-k8s-extension/actions/workflows/maven-build.yaml/badge.svg)](https://github.com/Netcracker/qubership-core-junit-k8s-extension/actions/workflows/maven-build.yaml)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?metric=coverage&project=Netcracker_qubership-core-junit-k8s-extension)](https://sonarcloud.io/summary/overall?id=Netcracker_qubership-core-junit-k8s-extension)
[![duplicated_lines_density](https://sonarcloud.io/api/project_badges/measure?metric=duplicated_lines_density&project=Netcracker_qubership-core-junit-k8s-extension)](https://sonarcloud.io/summary/overall?id=Netcracker_qubership-core-junit-k8s-extension)
[![vulnerabilities](https://sonarcloud.io/api/project_badges/measure?metric=vulnerabilities&project=Netcracker_qubership-core-junit-k8s-extension)](https://sonarcloud.io/summary/overall?id=Netcracker_qubership-core-junit-k8s-extension)
[![bugs](https://sonarcloud.io/api/project_badges/measure?metric=bugs&project=Netcracker_qubership-core-junit-k8s-extension)](https://sonarcloud.io/summary/overall?id=Netcracker_qubership-core-junit-k8s-extension)
[![code_smells](https://sonarcloud.io/api/project_badges/measure?metric=code_smells&project=Netcracker_qubership-core-junit-k8s-extension)](https://sonarcloud.io/summary/overall?id=Netcracker_qubership-core-junit-k8s-extension)

## JUnit-5 extension to connect to Kubernetes in integration tests

<!-- TOC -->
  * [JUnit-5 extension to connect to Kubernetes in integration tests](#junit-5-extension-to-connect-to-kubernetes-in-integration-tests)
    * [Requirements:](#requirements)
      * [Supports only Kubernetes 1.27+](#supports-only-kubernetes-127)
      * [Requires Java 21+](#requires-java-21)
    * [How to use qubership extension library](#how-to-use-qubership-extension-library)
    * [Jacoco](#jacoco)
    * [How to enable extension](#how-to-enable-extension)
    * [How to include test to smoke bundle](#how-to-include-test-to-smoke-bundle)
    * [How to use KubernetesClientFactory](#how-to-use-kubernetesclientfactory)
    * [How to perform port forward](#how-to-perform-port-forward)
      * [To get URL with https protocol of the port-forward connection to the service named 'service' at port 9090 located in current namespace and current cloud:](#to-get-url-with-https-protocol-of-the-port-forward-connection-to-the-service-named-service-at-port-9090-located-in-current-namespace-and-current-cloud)
      * [To get NetSocketAddress of the port-forward connection to the service named 'postgres' at port 5432 located in custom namespace and custom clouds:](#to-get-netsocketaddress-of-the-port-forward-connection-to-the-service-named-postgres-at-port-5432-located-in-custom-namespace-and-custom-clouds)
      * [Port-forward URLs format](#port-forward-urls-format)
      * [To get PortForwardService instance to create port-forwards in runtime:](#to-get-portforwardservice-instance-to-create-port-forwards-in-runtime)
    * [More examples how to inject tests util services can be found here](#more-examples-how-to-inject-tests-util-services-can-be-found-here)
    * [Pod Scale > 1](#pod-scale--1)
    * [Fabric8 Kubernetes ConfigBuilder configuration](#fabric8-kubernetes-configbuilder-configuration)
      * [Default implementation: DefaultFabric8ConfigBuilderAdapter.java](#default-implementation-defaultfabric8configbuilderadapterjava)
      * [Configuration Properties](#configuration-properties)
      * [Request & Network](#request--network)
      * [Watches (event streaming)](#watches-event-streaming)
      * [WebSocket (exec / logs / port-forward)](#websocket-exec--logs--port-forward)
    * [Fabric8 Kubernetes KubernetesClientBuilder configuration](#fabric8-kubernetes-kubernetesclientbuilder-configuration)
      * [DefaultFabric8KubernetesClientBuilderAdapter](#defaultfabric8kubernetesclientbuilderadapter)
      * [Purpose](#purpose)
      * [Configuration Properties](#configuration-properties-1)
    * [Use free local ports in port-forwards](#use-free-local-ports-in-port-forwards)
    * [Migration from 6.x.x version to 7.x.x](#migration-from-6xx-version-to-7xx)
      * [PlatformClient was deleted - use KubernetesClient directly](#platformclient-was-deleted---use-kubernetesclient-directly)
      * [ITHelper was deleted - use TokenService instead](#ithelper-was-deleted---use-tokenservice-instead)
      * [TlsConfig was deleted because TLS in Cloud-Core provided via static-core-gateway is deprecated and will be deleted](#tlsconfig-was-deleted-because-tls-in-cloud-core-provided-via-static-core-gateway-is-deprecated-and-will-be-deleted)
      * [There are brand-new annotations to create port-forwards, Kubernetes client etc. These new annotations support work with multiple Kubernetes clouds and allows to create port forwards for raw TCP/UDP connections](#there-are-brand-new-annotations-to-create-port-forwards-kubernetes-client-etc-these-new-annotations-support-work-with-multiple-kubernetes-clouds-and-allows-to-create-port-forwards-for-raw-tcpudp-connections)
<!-- TOC -->

### Requirements:

#### Supports only Kubernetes 1.27+

#### Requires Java 21+

### How to use qubership extension library

You can use qubership extension for integration tests in your services. For this you need to put dependency in your
pom.xml:

``` xml
        <dependency>
            <groupId>com.netcracker.cloud.junit.cloudcore</groupId>
            <artifactId>cloud-core-extension</artifactId>
            <version>{library version}</version>
        </dependency>
```

### Jacoco

Usage example.

```
<plugins>
    <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-surefire-plugin</artifactId>
        <version>{surefire.plugin.version}</version>
    </plugin>
    <plugin>
        <groupId>org.jacoco</groupId>
        <artifactId>jacoco-maven-plugin</artifactId>
        <version>${jacoco.plugin.version}</version>
        <executions>
            <execution>
                <goals>
                    <goal>prepare-agent</goal>
                    <goal>report</goal>
                </goals>
            </execution>
        </executions>
        <configuration>
            <dataFile>target/jacoco.exec</dataFile>
            <outputDirectory>target/jacoco-ut</outputDirectory>
        </configuration>
    </plugin>
</plugins>
```

### How to enable extension

To enable extension annotate class with tests or parent class with annotation @EnableExtension

### How to include test to smoke bundle

To include tests to smoke bundle add annotation @SmokeTest on class or method. To run tests for smoke tests execute
maven with key -Dgroups=Smoke

``` java
@SmokeTest
class Test
```

### How to use KubernetesClientFactory

specify field KubernetesClientFactory:

``` java
    KubernetesClientFactory kubernetesClientFactory;
```

### How to perform port forward

#### To get URL with https protocol of the port-forward connection to the service named 'service' at port 9090 located in current namespace and current cloud:

``` java
    @PortForward(serviceName = @Value(value = "service"), protocol = @Value(value = "https"), port = @IntValue(9090))
    URL url;
```

#### To get NetSocketAddress of the port-forward connection to the service named 'postgres' at port 5432 located in custom namespace and custom clouds:

``` java
    @PortForward(serviceName = @Value(value = "postgres"), port = @IntValue(5432),
                 cloud = @Cloud(cloud = @Value(prop = "clouds.cloud_1.name"), namespace = @Value(prop = "clouds.cloud_1.namespaces.origin")))
    NetSocketAddress postgresAddressCloud1;

    @PortForward(serviceName = @Value(value = "postgres"), port = @IntValue(5432),
                 cloud = @Cloud(cloud = @Value(prop = "clouds.cloud_2.name"), namespace = @Value(prop = "clouds.cloud_2.namespaces.origin")))
    NetSocketAddress postgresAddressCloud2;
```

#### Port-forward URLs format

By default, when there is single cloud specified among System properties (via property clouds.<cloud>.name) then
port-forward service constructs host in format:
<service-name>.<namespace>
But if there is property portforward.fqdn.hosts.enabled=true or there are more than one cloud property then port-forward
service will construct hosts in format:
<service-name>.<namespace>.svc.<cloud> where cloud - host name of kubernetes master URL of particular cloud

#### To get PortForwardService instance to create port-forwards in runtime:

To achieve this you should:

1) Autowire via @PortForwardClient PortForwardService
2) Call method portForward(...)
3) Close portforward after the test. (see @AfterEach cleanup() method). If you doesn't close portforward connection in
   will be done automatically in the after @AfterAll phase
   If the service is located in the same namespace as is specified in NAMESPACE variable or in property
   env.cloud-namespace then you may pass only service name otherwise you should pass namespace too.
   For example:

``` java
    @Cloud
    private PortForwardService portForwardService;
   
    private URL testUrl;
    
    @AfterEach
    public void cleanup() {
        if (testUrl != null) {
            portForwardService.closePortForward(testUrl);
        }
    }

    @Test
    public void createPortForwardTest1() {
        // if service is located in same namespace you may call the next method
        testUrl = portForwardService.portForward(ServicePortForwardParams.builderAsUrl("my-service, 8080).build());
    }
 
    @Test
    public void createPortForwardTest2() {
        // if service is located in another namespace you should specify it
        testUrl = portForwardService.portForward(ServicePortForwardParams.builderAsUrl("my-service, 8080).namespace("my-namespace").build());
    }

    @Test
    public void createPortForwardFromGivenUri() {
        // if url was provided by some in-cloud service then you can create port-forward from it
        String givenUri = "http://service:8080"; 
        testUrl = portForwardService.portForward(UrlPortForwardParams.builderAsUrl(givenUri));
    }
```

### More examples how to inject tests util services can be found [here](cloud-core-extension/src/test/java/com/netcracker/cloud/junit/cloudcore/extension/callbacks/classes/TestClass.java)

### Pod Scale > 1

By default, port-forward is linked to any of the pods found by the service selector.
In case when port-forward to the particular pod is required - use

``` java
    @Cloud
    private PortForwardService portForwardService;
    
    private URL pod1_url;
    private URL pod2_url;
    
    @AfterEach
    public void cleanup() {
        if (pod1_url != null) {
            portForwardService.closePortForward(pod1_url);
        }
        if (pod2_url != null) {
            portForwardService.closePortForward(pod2_url);
        }
    }

    @Test
    public void createPortForwardPod1() {        
        pod1_url = portForwardService.portForward(PodPortForwardParams.builder("pod-1-name, 8080).build()).toHttpUrl();
        pod2_url = portForwardService.portForward(PodPortForwardParams.builder("pod-2-name, 8080).build()).toHttpUrl();
    }

```

### Fabric8 Kubernetes ConfigBuilder configuration

DefaultKubernetesClientFactory is used to create KubernetesClient to be injected into tests.
This factory uses implementations of Fabric8ConfigBuilderAdapter to configure ConfigBuilder which is used to create
KubernetesClient.
You can provide your own implementation by implementing Fabric8ConfigBuilderAdapter and putting it at
'src/main/resources/META-INF/services/com.netcracker.cloud.junit.cloudcore.extension.client.Fabric8ConfigBuilderAdapter'
of your test project.
All discovered at classpath adapters will be used to configure KubernetesClient, applied in order according to their
@Priority annotation.

---

#### Default implementation: [DefaultFabric8ConfigBuilderAdapter.java](cloud-core-extension/src/main/java/com/netcracker/cloud/junit/cloudcore/extension/client/DefaultFabric8ConfigBuilderAdapter.java)

The `adapt(ConfigBuilder)` method:

- Applies **network and retry configuration**
- Configures **watch reconnect behavior**
- Configures **WebSocket keepalive**
- Reads all values from **JVM system properties**
- Falls back to **sensible defaults** if properties are not provided

---

#### Configuration Properties

All properties are **optional**.  
If not set, the documented default value is used.

#### Request & Network

| Property                          | Type     | Default | Description                                                                                  |
|-----------------------------------|----------|---------|----------------------------------------------------------------------------------------------|
| `k8s.request.retry.backoff.limit` | int      | `3`     | Number of retries for failed REST requests (GET/LIST/CREATE/PATCH) using exponential backoff |
| `k8s.connection.timeout.ms`       | int (ms) | `10000` | TCP connection timeout to Kubernetes API server                                              |
| `k8s.request.timeout.ms`          | int (ms) | `60000` | Overall timeout for a single HTTP request                                                    |

---

#### Watches (event streaming)

| Property                          | Type     | Default | Description                                |
|-----------------------------------|----------|---------|--------------------------------------------|
| `k8s.watch.reconnect.interval.ms` | int (ms) | `3000`  | Delay between watch reconnect attempts     |
| `k8s.watch.reconnect.limit`       | int      | `5`     | Maximum number of watch reconnect attempts |

> Watches are automatically re-established when they drop due to network issues, API restarts, or resourceVersion
> expiration.

---

#### WebSocket (exec / logs / port-forward)

| Property                         | Type      | Default | Description                                                                          |
|----------------------------------|-----------|---------|--------------------------------------------------------------------------------------|
| `k8s.websocket.ping.interval.ms` | long (ms) | `10000` | Ping interval to keep WebSocket connections alive through proxies and load balancers |

---

### Fabric8 Kubernetes KubernetesClientBuilder configuration

DefaultKubernetesClientFactory is used to create KubernetesClient to be injected into tests.
This factory uses implementations of Fabric8KubernetesClientBuilderAdapter to configure KubernetesClientBuilder which is
used to create KubernetesClient.
You can provide your own implementation by implementing Fabric8KubernetesClientBuilderAdapter and putting it at
'
src/main/resources/META-INF/services/com.netcracker.cloud.junit.cloudcore.extension.client.Fabric8KubernetesClientBuilderAdapter'
of your test project.
All discovered at classpath adapters will be used to configure KubernetesClient, applied in order according to their
@Priority annotation.

#### DefaultFabric8KubernetesClientBuilderAdapter

[DefaultFabric8KubernetesClientBuilderAdapter](cloud-core-extension/src/main/java/com/netcracker/cloud/junit/cloudcore/extension/client/DefaultFabric8KubernetesClientBuilderAdapter.java)
is the default implementation of
`Fabric8KubernetesClientBuilderAdapter`. It customizes the Fabric8
`KubernetesClientBuilder` at the **HTTP transport layer** before the
`KubernetesClient` is created.

This adapter is discovered via Java SPI and applied together with any other classpath adapters, ordered by `@Priority`.

---

#### Purpose

This adapter provides a minimal, deterministic baseline for HTTP client setup:

- Ensures a specific **HTTP client factory** is used:
    - `withHttpClientFactory(getHttpClientFactory())`
- Applies an explicit **connect timeout** to the underlying HTTP client:
    - `builder.connectTimeout(..., TimeUnit.SECONDS)`
- Supports overriding behavior via **JVM system properties**, with defaults.

---

#### Configuration Properties

All properties are optional. If not set, defaults are used.

| Property                       | Type | Default | Description                                                                             |
|--------------------------------|------|---------|-----------------------------------------------------------------------------------------|
| `k8s.http.connect.timeout.sec` | int  | `15`    | TCP connect timeout (seconds) for establishing connections to the Kubernetes API server |

---

### Use free local ports in port-forwards

By default, port-forwards use as local ports the target ports specified in @PortForward annotation.
But if you want to use random free OS local port, you need to specify the following property:
```properties
portforward.use.free.local.ports=true
```

### Migration from 6.x.x version to 7.x.x

#### PlatformClient was deleted - use KubernetesClient directly

#### ITHelper was deleted - use TokenService instead

#### TlsConfig was deleted because TLS in Cloud-Core provided via static-core-gateway is deprecated and will be deleted

#### There are brand-new annotations to create port-forwards, Kubernetes client etc. These new annotations support work with multiple Kubernetes clouds and allows to create port forwards for raw TCP/UDP connections

before migration - 6.x.x config:

``` java
    // #1
    @Named("internal-gateway-service")
    @PortForward
    protected static URL internalGateway;
    
    // #2 
    @Client
    protected static PlatformClient platformClient;
    
    // #3
    protected static ITHelper itHelper;

    @BeforeAll
    public static void initITHelper() throws Exception {
        itHelper = new ITHelper(internalGateway, platformClient);
        String m2mToken = itHelper.loginAsM2M("paas-mediation");
    }

    // #4 
    @PortForwardClient
    protected static PortForwardService portForwardService;
   
    @BeforeAll
    public static void init() throws Exception {
        URL url = portForwardService.createPortForward("service", 8181)
    }

    // #5 
    @Resource
    @WithLabel(name = "name", value = "service)
    protected static Pod pod;

    // #6
    @Named(value = "target-service", namespace = "custom-namespace")
    @Port(9090)
    @PortForward
    private URL url;
```

after migration - 7.x.x config:

``` java
    // #1 
    @PortForward(serviceName = @Value(value = "internal-gateway-service"))
    protected static URL internalGateway;

    // #2 
    @Cloud
    protected static KubernetesClient kubernetesClient;

    // #3
    @PortForward(serviceName = @Value("internal-gateway-service"))
    protected static TokenService tokenService;

    @BeforeAll
    public static void init() throws Exception {
        String m2mToken = tokenService.loginAsM2M("paas-mediation");
    }

    // #4 
    @Cloud
    protected static PortForwardService portForwardService;
 
    @BeforeAll
    public static void init() throws Exception {
        URL url = portForwardService.portForward(ServicePortForwardParams.builder("service", 8181).build()).toHttpUrl();
    }

    // #5
    @Cloud
    protected static KubernetesClient kubernetesClient;
    
    protected static Pod pod;
    
    @BeforeAll
    public static void getPod() throws Exception {
       pod = kubernetesClient.pods().withLabel("name", "service").list().getItems().getFirst();
    }

    // #6
    @PortForward(serviceName = @Value("target-service"), port = @IntValue(9090), cloud = @Cloud(namespace = @Value("custom-namespace")))
    private URL url;
```
