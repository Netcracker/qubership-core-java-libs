microservice-framework-health-indicator
---------------------------------------

`microservice-framework-health-indicator` brings additional health indicators. The following indicators are supported now:
* Compressed Class Space
* Heap memory
* Metaspace memory 

All these indicators are optional and can be enabled by wish.

Usage
-----

###### 1. Maven dependency

First of all you should add the Maven dependency:

```xml
    <dependency>
        <groupId>org.qubership.cloud</groupId>
        <artifactId>framework-extension-health-indicators</artifactId>
        <version>{VERSION}</version>
    </dependency>
```

###### 2. Enable indicators

All indicators are optional and disabled by default. To enable them you should set the following settings:

```yaml
health:
  memory:
    compressed-class-space.enabled: true # to enable 'compressed class space' memory indicator
    heap.enabled: true                   # to enable 'heap' memory indicator
    metaspace.enabled: true              # to enable 'metaspace' memory indicator
```