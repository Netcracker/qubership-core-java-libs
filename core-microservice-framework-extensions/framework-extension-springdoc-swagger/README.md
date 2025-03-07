framework-extension-springdoc-swagger
---------------------------------------

`framework-extension-springdoc-swagger` brings additional functionality to springdoc-swagger and allows 
to perform a `m2m`, `user` or `bearer` secured request through Swagger UI.

Usage 
-----

###### 1. Add Maven dependency

Add the Maven dependency:

```xml
    <dependency>
        <groupId>org.qubership.cloud</groupId>
        <artifactId>framework-extension-springdoc-swagger</artifactId>
        <version>{VERSION}</version>
    </dependency>
```

###### 2. Enable springdoc-swagger extension configuration

Create your spring swagger configuration file and put the `@EnableSwaggerAuth` annotation.  
For example:

```java
import annotation.org.qubership.cloud.frameworkextensions.swagger.config.EnableSwaggerAuth;
...

@Configuration
@EnableSwaggerAuth
public class SwaggerConfiguration {
    ...
}
```

All your endpoints will be secured with 3 security schemas:
 
```java
org.qubership.cloud.frameworkextensions.swagger.config.M2M_SECURITY_DEF
org.qubership.cloud.frameworkextensions.swagger.config.CLOUD_ADMIN_SECURITY_DEF
org.qubership.cloud.frameworkextensions.swagger.config.API_KEY_SECURITY_DEF

```

###### 3. Configure Swagger security schemas (optional)

The configuration above will allow you to use `m2m`, `user`, and `bearer` authorization for all your REST API. 

If you want to limit some of them, you should specify `security` parameter in `@Operation` annotation.  
For example: 
```java
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
...

@Operation(summary = "Add new item", description = "Adds new items",
        security = { @SecurityRequirement(name = M2M_SECURITY_DEF) })
```
where value of @SecurityRequirement.name can have the following values:
* M2M_SECURITY_DEF  -  for `m2m` auth
* CLOUD_ADMIN_SECURITY_DEF - for `user` auth
* API_KEY_SECURITY_DEF - for `bearer` auth

If you want to unsecure some API, use @SecurityRequirements annotation.
For example:
```java
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
...

@SecurityRequirements
@Operation(summary = "Add new item", description = "Adds new items")
```