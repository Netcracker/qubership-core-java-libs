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
      * [How to perform login and get access token](#how-to-perform-login-and-get-access-token)
        * [1) Inject TokenService](#1-inject-tokenservice)
        * [2) Perform login as cloud admin or as microservice.](#2-perform-login-as-cloud-admin-or-as-microservice-)
      * [How to perform port forward](#how-to-perform-port-forward)
        * [To get URL with https protocol of the port-forward connection to the service named 'service' at port 9090 located in current namespace and current cloud:](#to-get-url-with-https-protocol-of-the-port-forward-connection-to-the-service-named-service-at-port-9090-located-in-current-namespace-and-current-cloud)
        * [To get NetSocketAddress of the port-forward connection to the service named 'postgres' at port 5432 located in custom namespace and custom clouds:](#to-get-netsocketaddress-of-the-port-forward-connection-to-the-service-named-postgres-at-port-5432-located-in-custom-namespace-and-custom-clouds)
        * [To get PortForwardService instance to create port-forwards in runtime:](#to-get-portforwardservice-instance-to-create-port-forwards-in-runtime)
      * [More examples how to inject tests util services can be found here](#more-examples-how-to-inject-tests-util-services-can-be-found-here)
      * [Pod Scale > 1](#pod-scale--1)
      * [Migration from 6.x.x version to 7.x.x](#migration-from-6xx-version-to-7xx)
        * [PlatformClient was deleted - use KubernetesClient directly](#platformclient-was-deleted---use-kubernetesclient-directly)
        * [ITHelper was deleted - use TokenService instead](#ithelper-was-deleted---use-tokenservice-instead)
        * [TlsConfig was deleted because TLS in Cloud-Core provided via static-core-gateway is deprecated and will be deleted](#tlsconfig-was-deleted-because-tls-in-cloud-core-provided-via-static-core-gateway-is-deprecated-and-will-be-deleted)
        * [There are brand-new annotations to create port-forwards, Kubernetes client etc. These new annotations support work with multiple Kubernetes clouds and allows to create port forwards for raw TCP/UDP connections](#there-are-brand-new-annotations-to-create-port-forwards-kubernetes-client-etc-these-new-annotations-support-work-with-multiple-kubernetes-clouds-and-allows-to-create-port-forwards-for-raw-tcpudp-connections)
<!-- TOC -->

#### Requirements:
##### Supports only Kubernetes 1.27+
##### Requires Java 21+

###  How to use qubership extension library
You can use qubership extension for integration tests in your services. For this you need to put dependency in your pom.xml:
``` xml
        <dependency>
            <groupId>com.netcracker.cloud.junit.cloudcore</groupId>
            <artifactId>cloud-core-extension</artifactId>
            <version>{library version}</version>
        </dependency>
```

#### Jacoco
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

#### How to enable extension
To enable extension annotate class with tests or parent class with annotation @EnableExtension

#### How to include test to smoke bundle
To include tests to smoke bundle add annotation @SmokeTest on class or method. To run tests for smoke tests execute maven with key -Dgroups=Smoke
``` java
@SmokeTest
class Test
```

#### How to perform login and get access token
In different situations you may need to know access token of cloud administrator or service to send request. In order to get it you should perform the following steps:
##### 1) Inject TokenService
##### 2) Perform login as cloud admin or as microservice. 
You can be authorized as cloud admin and as microservice in ITHelper at the time:

``` java
@PortForward(serviceName = @Value(value = "internal-gateway-service"))
static TokenService tokenService;

void test() {
    String adminToken = tokenService.loginAsCloudAdmin();
    String m2mToken = tokenService.loginAsM2M("my-service");
}
```

#### How to perform port forward
##### To get URL with https protocol of the port-forward connection to the service named 'service' at port 9090 located in current namespace and current cloud:
``` java
    @PortForward(serviceName = @Value(value = "service"), protocol = @Value(value = "https"), port = @IntValue(9090))
    URL url;
```

##### To get NetSocketAddress of the port-forward connection to the service named 'postgres' at port 5432 located in custom namespace and custom clouds:
``` java
    @PortForward(serviceName = @Value(value = "postgres"), port = @IntValue(5432),
                 cloud = @Cloud(cloud = @Value(prop = "clouds.cloud_1.name"), namespace = @Value(prop = "clouds.cloud_1.namespaces.origin")))
    NetSocketAddress postgresAddressCloud1;

    @PortForward(serviceName = @Value(value = "postgres"), port = @IntValue(5432),
                 cloud = @Cloud(cloud = @Value(prop = "clouds.cloud_2.name"), namespace = @Value(prop = "clouds.cloud_2.namespaces.origin")))
    NetSocketAddress postgresAddressCloud2;
```

##### To get PortForwardService instance to create port-forwards in runtime:
To achieve this you should:
1) Autowire via @PortForwardClient PortForwardService
2) Call method portForward(...)
3) Close portforward after the test. (see @AfterEach cleanup() method). If you doesn't close portforward connection in will be done automatically in the after @AfterAll phase
If the service is located in same namespace as is specified in NAMESPACE variable or in property env.cloud-namespace then you may pass only service name otherwise you should pass namespace too. 
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
        testUrl = portForwardService.portForward(new PortForwardParams("my-service, 8080));
    }
 
    @Test
    public void createPortForwardTest2() {
        // if service is located in another namespace you should specify it
        testUrl = portForwardService.portForward(new PortForwardParams("my-service, 8080).withNamespace("my-namespace");
    }
```

#### More examples how to inject tests util services can be found [here](cloud-core-extension/src/test/java/com/netcracker/cloud/junit/cloudcore/extension/callbacks/classes/TestClass.java)

#### Pod Scale > 1
By default, port-forward is linked to any of pods found by service selector.
In case when port-forward to particular pod is required - use
``` java
String podName = ""; // find name of required pod
kubernetesClient.pods().withName(podName).portForward(...);
```

#### Migration from 6.x.x version to 7.x.x
##### PlatformClient was deleted - use KubernetesClient directly
##### ITHelper was deleted - use TokenService instead
##### TlsConfig was deleted because TLS in Cloud-Core provided via static-core-gateway is deprecated and will be deleted
##### There are brand-new annotations to create port-forwards, Kubernetes client etc. These new annotations support work with multiple Kubernetes clouds and allows to create port forwards for raw TCP/UDP connections
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
        URL url = URI.create(String.format("http://%s", portForwardService.portForward(new PortForwardParams("service", 8181)).getEndpoint())).toURL();
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
