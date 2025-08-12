Overview
--------

`config-server-loader-webclient` is a library which allows you to load spring properties from config-server.
This module uses webclient for making rest requests.

Usage
-----

##### Maven
The firstly: add Maven dependency to your POM:

```xml
    <dependency>
        <artifactId>config-server-loader-webclient</artifactId>
        <groupId>com.netcracker.cloud</groupId>
        <version>{VERSION}</version>
    </dependency>
```

##### Enable config server loader

The secondly: put `@EnableConfigServerLoaderOnWebClient` annotation on your `@Configuration` class. For example:

```java
    @Configuration
    @EnableConfigServerLoaderOnWebClient
    public class Application {
        ...
    }
```

`config-server-loader-webclient` is wrapper under spring-cloud-config-server so if you need to configure more accurately 
then you should refer to spring documentation and tune as you need.
