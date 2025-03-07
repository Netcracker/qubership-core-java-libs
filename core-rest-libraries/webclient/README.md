webclient
---------

Webclient module offers `webclient` client for making secured request. `JWT` token is automatically inserted to this client during 
outgoing request so you do not need to think about a needed bearer token when you want to make a `m2m` or `user` REST request.


Usage
-----

###### 1. Add the Maven dependency:

```xml
    <dependency>
        <groupId>org.qubership.cloud</groupId>
        <artifactId>webclient</artifactId>
        <version>{VERSION}</version>
    </dependency>
```

###### 2. Enable webclient configuration

To enable webclient and for loading necessary configuration you should put the `@EnableFrameworkWebClient` annotation on the your `@Configuration` class.  
For example:
```java
    @Configuration
    @EnableFrameworkWebClient
    public class Application {
        ...
    }
```

###### 3. Autowire webclient beans

For `m2m` webclient injection you should specify `m2mWebClient` qualifier:

 ```java
    @Autowired("m2mWebClient")
    private WebClient m2mWebClient;
```

For `user` webclient injection specify `userWebClient` qualifier:

```java
    @Autowired("userWebClient")
    private WebClient userWebClient;
```

For `smartWebClient` webclient injection specify `smartWebClient` qualifier:

```java
    @Autowired("smartWebClient")
    private WebClient smartWebClient;
```

`smartWebClient` has opportunity dynamically to figure out which `m2mWebClient` or `userWebClient` to use. 
This bean uses `SmartClientContext` which keeps information about incoming request. If the request was with a M2M role 
then `m2mWebClient` will be used. And if the request was with a user role then `userWebClient` will be used.

###### 4. Webclient usage sample

For webclient injection you should specify the corresponding qualifier (see above) and use general webclient API to make requests.
If your request is done via cloud-core gateway, you can use predefined properties that are received from config-server, from global profile:


| Property name                                     | Description | Default value |
| -------------------------------------------------- | ----------- | --------------- |
| `apigateway.url` | Internal gateway address in the cloud  | `http://internal-gateway-service:8080` |
| `apigateway.url-https` | Internal gateway address in the cloud when TLS is enabled  | `https://internal-gateway-service:8443` |
| `apigateway.private.url` | Private gateway address in the cloud  | `http://private-gateway-service:8080` |
| `apigateway.private.url-https` | Private gateway address in the cloud when TLS is enabled  | `https://private-gateway-service:8443` |
| `apigateway.public.url` | Public gateway address in the cloud  | `http://public-gateway-service:8080` |
| `apigateway.public.url-https` | Public gateway address in the cloud when TLS is enabled  | `https://public-gateway-service:8443` |


For example,

 ```java

import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

...

    @Autowired("m2mWebClient")
    private WebClient m2mWebClient;

    @Value("${apigateway.url:http://internal-gateway-service:8080}")
    private final String apiGatewayUrl;

    public String getTestServiceName(Long id) {
            URI uri = UriComponentsBuilder.fromPath("/api/v1/test-service/name/{id}").build(id);
            return m2mWebClient.get().uri(apiGatewayUrl + uri)
            .retrieve()
            .bodyToMono(String.class)
            .block();
    }
```
