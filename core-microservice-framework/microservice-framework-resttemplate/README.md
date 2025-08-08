Overview
--------

You can find a common overview in [readme](./../README.md).  
This document describes in more detail how to use microservice-framework on resttemplate.  
_**Please note** that this restclient is deprecated and we strongly recommend to use [microservice-framework on webclient](./../microservice-framework-webclient/README.md) instead._

Usage
-----
##### Add Maven dependency

First of all put `microservice-framework-resttemplate` dependency in you POM file:
```xml
    <dependency>
        <groupId>com.netcracker.cloud</groupId>
        <artifactId>microservice-framework-resttemplate</artifactId>
        <version>{VERSION}</version>
    </dependency>
```

##### Enable framework functionality
In order to enable microservice-framework functionality you need to be inherited from `BaseApplicationOnRestTemplate` and initialize Spring context via MicroserviceApplicationBuilder. For example:

```java
public class Application extends BaseApplicationOnRestTemplate {

    public static void main(String[] args) {
        MicroserviceApplicationContext context = new MicroserviceApplicationBuilder()
                .withApplicationClass(Application.class)
                .withArguments(args)
                .build();
    }
}
```

##### Enable DBaaS-client
`microservice-framework-resttemplate` contains two DBaaS-client starts: `dbaas-client-postgresql-starter` and `dbaas-client-mongodb-starter`, but they are disabled by default.
So if you need mongodb or postgresql database you should add of the following annotations:
 
```text
    @EnableDbaasMongo/@EnableDbaasPostgres – to enable only microservice database
    @EnableMicroserviceMongoDb/@EnableMicroservicePostgresDb – to enable microservice and tenant database
```
