# springcloud-config-source

`springcloud-config-source` is extension for Quarkus framework which allows working with config-server.

## Table of contents:
* [Usage](#usage)
* [Programmatic API](#programmatic-api)

## Usage

In order to start using you only need to add this library to the dependencies and specify the config-server address in the property value.

Maven dependency:

```xml
        <dependency>
            <groupId>org.qubership.cloud.quarkus</groupId>
            <artifactId>springcloud-config-source</artifactId>
            <version>${version}</version>
        </dependency>
``` 
where a version can be found in a artifactory by the link:
https://artifactorycn.qubership.org/pd.saas.mvn/org/qubership/cloud/quarkus/springcloud-config-source/

The config-server address must be specified in the following property:

| Property                                                                      | Description                 | Default value              | Status      |
|-------------------------------------------------------------------------------|-----------------------------|----------------------------|-------------|
| org.qubership.cloud.springcloud.config.source.ConfigServerClient/mp-rest/url | URL to access config-server | http://config-server:8080  | since 2.2.0 |


## Programmatic API

This extension provides an API with which you can get a configuration list from the config-server at runtime.

You can use the PropertyManager class.
It has one method:

```java
public Map<String, String> getProperties()
```

With this method, you can get properties from config-server in runtime.

Сreate an instance of the PropertyManager class:
```java
private PropertyManager pm = PropertyManager.getInstance();
```

To get your properties from config-server, use getProperties() method:
```java
Map<String, String> properties = pm.getProperties()
```


