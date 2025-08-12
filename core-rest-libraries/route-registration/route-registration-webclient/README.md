route-registration-webclient
-----------------

Purpose and process of `route-registration` is described in this [README](./../README.md). 
In this article describe procedure how to use `route-registration` on webclient.   

Usage
-----

The main usage is described in a general [README](./../README.md). Here is described some features of using `route-registration-webclient` library.

##### Maven
The firstly: add Maven dependency to your POM:

```xml
    <dependency>
        <artifactId>route-registration-webclient</artifactId>
        <groupId>com.netcracker.cloud</groupId>
        <version>{VERSION}</version>
    </dependency>
```

##### Enable route registration

The secondly: put `@EnableRouteRegistrationOnWebClient` annotation on your `@Configuration` class. For example:

```java
    @Configuration
    @EnableRouteRegistrationOnWebClient
    public class Application {
        ...
    }
```

##### Configure your application

Finally, you need to be sure that all necessary properties are set and the `@Route` annotation is specified. You can find more details on the [page](./../README.md). 
