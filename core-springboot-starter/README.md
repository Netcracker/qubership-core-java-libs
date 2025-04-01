Overview
--------

`qubership-spring-boot-starter-parent` is `spring-boot-starter-parent` wrapper and it brings some essential cloud-core libraries through `dependencyManagement` section.Thanks to it we can control sprint-boot version and avoid version conflicts in our libraries.  
This library is used when someone wants to create microservice quickly by using microservice-framework client.


Usage
-----

##### Maven dependency

Fot using this library you just need to put the following code into your POM file:

```xml
    <parent>
        <groupId>org.qubership.cloud</groupId>
        <artifactId>qubership-spring-boot-starter-parent</artifactId>
        <version>{VERSION}</version>
    </parent>
```

##### Put microservice-framework specific dependency

qubership-spring-boot-starter-parent offers to work with either microservice-framework-resttemplate or microservice-framework-webclient. 
So you should make a decision with which a restclient you service will work and put down one of the following dependencies:

```xml
    <dependency>
        <groupId>org.qubership.cloud</groupId>
        <artifactId>microservice-framework-resttemplate</artifactId>
    </dependency>
```

or

```xml
    <dependency>
        <groupId>org.qubership.cloud</groupId>
        <artifactId>microservice-framework-webclient</artifactId>
    </dependency>
```
_**Recommendation:** microservice-framework-resttemplate is a deprecated approach and will be deleted in one of the next major release. 
So we strongly recommend to use microservice-framework on WebClient._

Also notice that you do not have to specify an artifact version because the version will be taken from `qubership-spring-boot-starter-parent`.
