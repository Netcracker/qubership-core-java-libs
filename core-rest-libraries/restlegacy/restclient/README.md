# restclient


Restclient is a wrapper under resttemplate and it offers some convenient classes and methods for making security requests.  
If you use resttemplate then you won't have to think about which m2m or user resttemplate to use (`RestClient`). 
Also this library gives a great opportunity easy to make all request through API gateway (`ApiGatewayClient`).

Maven dependency:
----------------

```xml
    <dependency>
        <artifactId>restclient</artifactId>
        <groupId>org.qubership.cloud</groupId>
        <version>{VERSION}</version>
    </dependency>
```

Usage
-----

As was said before there are two main auxiliary classes: `RestClient` and `ApiGatewayClient`.  

##### RestClient
`RestClient` contains the following functionality:

* Performs retriable request (request will be repeated if an error occurs);
* Can work with `SmartClientContext` and dynamically determines which `m2m` or `user` resttemplate necessary to use;
* Allows to easy add your custom interceptors.

To enable restclient functionality you need to put `@EnableRestClient` annotation in your configuration class:

```java
@Configuration
@EnableRestClient
public class MyConfiguration {
    ...
}
```

When you do it, `restClient` bean will be created and you can inject this one where you want. For example:

 ```java
    @Autowired
    private RestClient restClient;
```

You can find some useful methods of RestClient, such as:
* addClientHttpRequestInterceptor
* get/post/put/delete/patch/head

##### ApiGatewayClient

`ApiGatewayClient` extends `RestClient` and makes all requests through API gateway.  
For using it, first of all you should extends your class from `ApiGatewayClient`. For example:

```java
@Component
public class UserManagementClient extends ApiGatewayClient {
    
    public UserManagementClient() {
            super(1, "user-management");
    }
}
``` 
 and pass `API version` and `microservice name` of microservice to which the call will be made to `super` method. Also pay attention that this object must be a bean (there is a `@Component` annotation).
 
 After that, you can inject this bean and call one the following methods:
 * get/post/put/delete/patch/head
 
 For example:
 
 ```java
 @Autowired
 private UserManagementClient umClient; 
 
public void getTenant() {
    ...
    ResponseEntity result = umClient.get("/tenant/123", Tenant.class);
    ...
}
```