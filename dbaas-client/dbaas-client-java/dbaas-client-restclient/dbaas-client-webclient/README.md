# DBaaS Client WebClient

DBaaS REST client implementation based on [WebClient](https://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/web/reactive/function/client/WebClient.html) 
for usage with Spring Framework 5 or higher. 

- [Prerequisites](#prerequisites)
- [Usage](#usage)

## Prerequisites
1. If you are using [microservice-framework-webclient](https://github.com/Netcracker/qubership-core-microservice-framework/blob/main/microservice-framework-webclient) 
    this dependency will be loaded transitively from `microservice-framework` and used by `dbaas-client` higher level modules, 
     so **you don't need to do anything**. 

2. If you are using [microservice-framework-resttemplate](https://github.com/Netcracker/qubership-core-microservice-framework/blob/main/microservice-framework-resttemplate) 
    (or for some other reason your application still uses 
    [RestTemplate](https://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/web/client/RestTemplate.html) 
    instead of [WebClient](https://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/web/reactive/function/client/WebClient.html)), 
    then **you need to use [dbaas-client-resttemplate](../dbaas-client-resttemplate) instead of `dbaas-client-webclient`**. 

## Usage
**Normally you should not use this library explicitly in your microservice, check [Prerequisites](#prerequisites)!**


In case you need to use this library explicitly, then follow the steps: 
1. Add library to your dependencies section of pom.xml: 
    ```xml
    <dependency>
        <groupId>com.netcracker.cloud</groupId>
        <artifactId>dbaas-client-webclient</artifactId>
        <version>${dbaas-client-webclient.version}</version>
    </dependency>
    ```
    Where `${dbaas-client-webclient.version}` is the latest release version
    
    Omit tag `version` if you use [dbaas-client-bom](../../../dbaas-client-bom-parent/dbaas-client-bom/README.md). if you use [qubership-springboot-starter-parent](hhttps://github.com/Netcracker/qubership-core-springboot-starter) or [dbaas-client-bom](../../../dbaas-client-bom-parent/dbaas-client-bom/README.md).
2. Enable Spring component-scan for package `com.netcracker.cloud.dbaas.client.restclient` to locate `dbaasRestClient` bean automatically 
    or import `DbaasWebClientConfiguration` class in your configuration. 

Now `dbaasRestClient` bean will be registered in your application context and used by all the database-specific starters you use. 
To access this bean yourself you can autowire it: 
```java
@Autowired
@Qualifier("dbaasRestClient")
private MicroserviceRestClient dbaasRestClient;
```
This bean can be passed to [DbaasClientImpl](https://github.com/Netcracker/qubership-dbaas-client/blob/main/dbaas-client-java/dbaas-client-core/src/main/java/com/netcracker/cloud/dbaas/client/DbaasClientImpl.java) 
constructor and used as REST client for DBaaS API. 
