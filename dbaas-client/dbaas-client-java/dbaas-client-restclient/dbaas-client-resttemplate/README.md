# DBaaS Client RestTemplate

DBaaS REST client implementation based on [RestTemplate](https://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/web/client/RestTemplate.html) 
for usage with Spring Framework. 

**It is highly recommended to use [dbaas-client-webclient](../dbaas-client-webclient/README.md) instead of this library if possible, since 
[RestTemplate](https://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/web/client/RestTemplate.html) 
will be deprecated in next Spring Framework release.**

- [Prerequisites](#prerequisites)
- [Usage](#usage)

## Prerequisites
1. If you are using [microservice-framework-resttemplate](<github link todo>/microservice-framework/tree/master/microservice-framework-resttemplate) 
    this dependency will be loaded transitively from `microservice-framework` and used by `dbaas-client` higher level modules, 
     so **you don't need to do anything**. 
2. If you are using [microservice-framework-webclient](<github link todo>/microservice-framework/tree/master/microservice-framework-webclient) 
    this dependency will be loaded transitively from `microservice-framework` and used by `dbaas-client` higher level modules, 
     so **you don't need to do anything**. 

2. If you are using [microservice-framework-webclient](<github link todo>/microservice-framework/tree/master/microservice-framework-webclient) 
    (or if your application just has [WebClient](https://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/web/reactive/function/client/WebClient.html) 
    on classpath), then **you need to use [dbaas-client-webclient](../dbaas-client-webclient/README.md) instead of `dbaas-client-resttemplate`**. 

## Usage
**Normally you should not use this library explicitly in your microservice, check [Prerequisites](#prerequisites)!**

In case you need to use this library explicitly, then follow the steps: 
1. Add library to your dependencies section of pom.xml: 
    ```xml
    <dependency>
        <groupId>com.netcracker.cloud</groupId>
        <artifactId>dbaas-client-resttemplate</artifactId>
        <version>${dbaas-client-resttemplate.version}</version>
    </dependency>
    ```
    Where `${dbaas-client-resttemplate.version}` is the latest release version
    
    Omit tag `version` if you use [qubership-springboot-starter-parent](<github link todo>/springboot-starter) or [dbaas-client-bom](../../dbaas-client-bom-parent/dbaas-client-bom/README.md). if you use [qubership-springboot-starter-parent](<github link todo>/springboot-starter) or [dbaas-client-bom](../../dbaas-client-bom-parent/dbaas-client-bom/README.md).
2. Enable Spring component-scan for package `com.netcracker.cloud.dbaas.client.restclient` to locate `dbaasRestClient` bean automatically 
    or import `DbaasRestTemplateConfiguration` and `DbaasBasicAuthRestTemplateConfiguration` classes in your configuration. 
3. Decide whether you will use M2M or basic authorization for requesting DBaaS REST API. 
    * If you want to use M2M authorization then property `dbaas.restclient.resttemplate.basic-auth` must equal `false` 
        (which is the default value). 
    * If you want to use basic authorization then you need to set property `dbaas.restclient.resttemplate.basic-auth=true` 
        and specify DBaaS API credentials using settings `dbaas.api.username`, `dbaas.api.password`.  
4. (Optional) As you may have noticed, there are two configuration classes (`DbaasRestTemplateConfiguration`, `DbaasBasicAuthRestTemplateConfiguration`) 
    and only one will actually create `dbaasRestClient` bean depending on `dbaas.restclient.resttemplate.basic-auth` property value. 
    So to simplify application startup a little bit you can import only one configuration class that is required for you 
    (`DbaasRestTemplateConfiguration` for M2M or `DbaasBasicAuthRestTemplateConfiguration` for basic auth).    

Now `dbaasRestClient` bean will be registered in your application context and used by all the database-specific starters you use. 
To access this bean yourself you can autowire it: 
```java
@Autowired
@Qualifier("dbaasRestClient")
private MicroserviceRestClient dbaasRestClient;
```
This bean can be passed to [DbaasClientImpl](<github link todo>/dbaas-client/blob/master/dbaas-client-java/dbaas-client-core/src/main/java/org/qubership/cloud/dbaas/client/DbaasClientImpl.java) 
constructor and used as REST client for DBaaS API. 
