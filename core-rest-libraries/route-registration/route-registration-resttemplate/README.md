route-registration-resttemplate
-----------------

Purpose and process of `route-registration` is described in this [README](./../README.md). 
In this article describe procedure how to use `route-registration` on resttemplate.   

_**Important:** Route-registration on a resttemplate client is a legacy approach and we stringly recommend to use route-registration on webclient instead._

Usage
-----

The main usage is described in the general [README](./../README.md) page. Here is described some features of using the `route-registration-resttemplate` library.

##### Maven
Firstly, add Maven dependency to your POM:

```xml
    <dependency>
        <artifactId>route-registration-resttemplate</artifactId>
        <groupId>com.netcracker.cloud</groupId>
        <version>{VERSION}</version>
    </dependency>
```

##### Enable route registration

Secondly, put `@EnableRouteRegistrationOnRestTemplate` annotation on your `@Configuration` class. For example:

```java
@Configuration
@EnableRouteRegistrationOnRestTemplate
public class Application {
}
```

##### Configure your application

Finally, you need to be sure that all necessary properties are set and the `@Route` annotation is specified. You can find more details on the [page](./../README.md). 
