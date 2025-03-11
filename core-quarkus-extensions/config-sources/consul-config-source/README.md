# Consul Config Source

Consul Config Source provides quarkus extension based on plain java core Consul Config library: [consul-config-provider-common](https://git.qubership.org/PROD.Platform.Cloud_Core/rest-libraries/-/tree/main/consul-config-provider/consul-config-provider-common).

Consul usage architecture, installation and migration guides: [https://perch.qubership.org/display/CLOUDCORE/Consul+as+Config+Server](https://<github link todo>/Consul+as+Config+Server)

# Configure
To include extension to your project add:

```xml
<dependency>
    <groupId>org.qubership.cloud.quarkus</groupId>
    <artifactId>consul-config-source</artifactId>
    <version>2.1.16</version>
</dependency>
```

Then, fill the config parameters. Configuration information and typical default parameters are described in the consul-client module: https://git.qubership.org/PROD.Platform.Cloud_Core/libs/cloud-core-quarkus-extensions/-/blob/main/config-sources/consul-client/README.md#consul-client

# Using at runtime 
You can use properties injection in standard Quarkus declarative way:
```java
    @ConfigProperty(name = "my.property")
    String myProperty;
```

or programmatically by explicit call to microprofile config source 
This property will be retrieved from Consul at http://consul-server:8500/v1/kv/config/mynamespace/myservice/my/property

