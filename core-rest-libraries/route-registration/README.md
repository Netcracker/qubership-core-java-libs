Route-registration
-----------------

`route-registration` library allows to register microservice API in gateways (`facade`, `internal`,`private`,`public`). 
Thanks to this any microservice can make REST call without knowing a url host. 

Usage
-----
###### 1. Add Maven dependency
First of all you should add the `route-registration-resttemplate` or `route-registration-webclient` artifact. 
How to do it and information about logic you can find at the pages: [route-registration-resttemplate](./route-registration-resttemplate/README.md) and [route-registration-webclient](./route-registration-webclient/README.md)

###### 2. Check your yml configuration file
Gateway needs microservice cloud url to know where to forward the request. Check that properties `cloud.microservice.name` and `server.port` are set in application.yml:
```yaml
spring:
  application:
    name: tenant-manager 
server:
  port: 8080
```
If you want to register microservice routes in the facade gateway, you must add the following additional properties:
```yaml
spring:
  application:
    cloud_service_name: tenant-manager-v1
server:
  port: 8080
```

###### 3. Enable automatic route registration

You need to add one of the following annotations for loading necessary configurations:
*  `@EnableRouteRegistrationOnRestTemplate` - if you use `route-registration-resttemplate`;
*  `@EnableRouteRegistrationOnWebClient` - if you use `route-registration-webclient`.

###### 4. Mark out routes in your code for registration in `internal`,`private`,`public` gateways

Add the `@Route` annotation to your controller class and specify a route type as a parameter. 
Route type parameter can have one of the following values: `RouteType.INTERNAL`, `RouteType.PRIVATE`, `RouteType.PUBLIC`.
The `@Route` annotation can be placed on class or method level.  
For example:

_Method level_
```java
import route.gateway.org.qubership.cloud.routesregistration.common.RouteType;
import org.qubership.cloud.routesregistration.common.spring.gateway.route.annotation.Route;
...
 
@RestController
@RequestMapping(path = "/api/v1/tenant-manager/tenant-registrations")
public class TenantRegistrationsController {
 
    @RequestMapping(path = "/tenant/{tenantId}/activate", method = {RequestMethod.PUT})
    @Route(type = RouteType.PRIVATE)
    public ResponseEntity<String> activateTenant() {
        ...
    }

    @GetMapping(path = "/dns")
    @Route(RouteType.PUBLIC)
    public ResponseEntity<String> findTenantIdBy() {
        ...
    }

    @PostMapping(path = "/tenant")
    @Route(RouteType.INTERNAL)
    public ResponseEntity<String> saveTenant(@RequestBody Tenant tenant) {
    	...
    }

}
```

This code creates mapping in the control plane: 

```text

Internal gateway:
"/api/v1/tenant-manager/tenant-registrations/tenant/*/activate/": "http://tenant-manager:8080/api/v1/tenant-manager/tenant-registrations/tenant"
"/api/v1/tenant-manager/tenant-registrations/dns/" : "http://tenant-manager:8080/api/v1/tenant-manager/tenant-registrations/dns"
"/api/v1/tenant-manager/tenant-registrations/tenant/" : "http://tenant-manager:8080/api/v1/tenant-manager/tenant-registrations/tenant"


Private gateway:
"/api/v1/tenant-manager/tenant-registrations/tenant/*/activate/": "http://tenant-manager:8080/api/v1/tenant-manager/tenant-registrations/tenant"
"/api/v1/tenant-manager/tenant-registrations/dns/" : "http://tenant-manager:8080/api/v1/tenant-manager/tenant-registrations/dns"



Public gateway:
"/api/v1/tenant-manager/tenant-registrations/dns/" : "http://tenant-manager:8080/api/v1/tenant-manager/tenant-registrations/dns"
```

_class level_

```java
import route.gateway.org.qubership.cloud.routesregistration.common.RouteType;
import org.qubership.cloud.routesregistration.common.spring.gateway.route.annotation.Route;
...
 
@RestController
@RequestMapping(path = "/api/v1/tenant-manager/tenant-registrations")
@Route(RouteType.PUBLIC)
public class TenantRegistrationsController {
 
    @RequestMapping(path = "/tenant/{tenantId}/activate", method = {RequestMethod.PUT})
    public ResponseEntity<String> activateTenant() {
        ...
    }

    @GetMapping(path = "/dns")
    public ResponseEntity<String> findTenantIdBy() {
        ...
    }

    @PostMapping(path = "/tenant")
    public ResponseEntity<String> saveTenant(@RequestBody Tenant tenant) {
    	...
    }

}

```

After that you can have access to that API by endpoint `http://public-gateway-service:8080/api/v1/tenant-manager/tenant-registrations/dns/`

###### 5. Mark out routes in your code for registration in `facade` gateway

Add the `@Route` annotation with type RouteType.FACADE to your controller class.  
The `@Route` annotation can be placed on class or method level.  
For example:

_Method level_
```java
import annotation.org.qubership.cloud.routesregistration.common.Route;
...
 
@RestController
public class TenantRegistrationsController {
 
    @Route(type = RouteType.FACADE)
    @RequestMapping(path = "/tenant-registrations/tenant/{tenantId}/activate", method = {RequestMethod.PUT})
    public ResponseEntity<String> activateTenant() {
        ...
    }
}
```

This code creates mapping in the control plane: 

```text

Facade gateway:
"/tenant-registrations/tenant/*/activate/": "http://tenant-manager:8080/tenant-registrations/tenant"  

```

_class level_

```java
import annotation.org.qubership.cloud.routesregistration.common.Route;
...
 
@RestController
@RequestMapping(path = "/tenant-registrations")
@Route(type = RouteType.FACADE)
public class TenantRegistrationsController {
 
    @RequestMapping(path = "/tenant/{tenantId}/activate", method = {RequestMethod.PUT})
    public ResponseEntity<String> activateTenant() {
        ...
    }
}

```

###### 6. Custom routes creation (optional)

In the process above you'll get routes as `path -> microservice-url/path`. 
Custom routes have different paths for source & target resources: `pathA -> microservice-url/pathB`
To override you rest endpoint you need to add `@GatewayRequestMapping` or `@FacadeGatewayRequestMapping` (to override facade gateway routing) on controller class or method level and 
specify your desired path.  
For example:

```java
import org.qubership.cloud.microservicecore.gateway.route.RouteType;
import org.qubership.cloud.microservicecore.gateway.route.annotation.Route;
...
 
@RestController
@Route(RouteType.PUBLIC)
@GatewayRequestMapping("/api/v1/user-management/scim/users")
@RequestMapping("/v1/users")
public class UserRestController {
  
    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<?> createUser(){
        ...
    }

    @RequestMapping(value = "/{userId}", method = RequestMethod.DELETE)
    public ResponseEntity<?> deleteUser() {
        ...
    }
}
```

This code creates a mapping in control plane:

```text

Internal gateway:
"/api/v1/user-management/scim/users/**" : "http://user-management:8080/v1/users"

Private gateway:
"/api/v1/user-management/scim/users/**" : "http://user-management:8080/v1/users"
 
Public gateway:
"/api/v1/user-management/scim/users/**" : "http://user-management:8080/v1/users"
```


###### 7. Specifying custom gateway name (e.g. composite-gateway) (optional)

Annotation `@Route` have field `gateways` which can be used to specify an array of gateway names. 
Routes marked with such annotation will be registered in all the specified gateways. 
This can be useful to configure composite (ingress) gateway routes as in the example below. 
```java
package org.qubership.cloud.sample.controller;

import annotation.org.qubership.cloud.routesregistration.common.Route;
import annotation.route.gateway.spring.org.qubership.cloud.routesregistration.common.GatewayRequestMapping;
//...

@Slf4j
@RestController
@RequestMapping(path = "/api/v1")
@GatewayRequestMapping("/api/v1/sample-service")
@Route(gateways = "my-service")
public class IngressRoutesController {
    @GetMapping("/common")
    @Route(gateways = "private-gateway-service")
    @Route(gateways = "my-service")
    public ResponseEntity bothPrivateAndCompositeRoute(HttpServletRequest request) {
        //...
    }

    @GetMapping("/only-composite")
    public ResponseEntity onlyCompositeRoute(HttpServletRequest request) {
        //...
    }
}
```
In this example route `/api/v1/sample-service/common` will be registered in private, internal and `my-service` gateways, 
while `/api/v1/sample-service` will be registered only in `my-service` gateway. 

> :information_source: Please note, that specifying `public-gateway-service`, `private-gateway-service` or `internal-gateway-service` 
> in the `gateways` field acts as specifying corresponding route type in `type` field, 
> e.g. annotation `@Route(gateways = "private-gateway-service")` will cause route to be sent in private and internal gateways. 

If `gateways` field specified in `@Route` annotation, `type` (`value`) field of this annotation will be ignored. 

**Effective gateway types, `@GatewayRequestMapping` and `@FacadeGatewayRequestMapping`**

If provided gateway name is the same as the microservice family name (cluster name in terms of control-plane), 
then route considered to be **facade** route. Gateway path mapping for such route can be affected only by `@FacadeGatewayRequestMapping` annotation. 

All the other routes' gateway path mappings can be affected only by `@GatewayRequestMapping` annotation. 

###### 8. Specifying custom host name for composite gateway (optional)

In order to specify custom hosts, you need to specify the following 2 properties in application.yml (or application.properties for quarkus)

Spring application.yml example:
```yml
mesh:
  gateway:
    name: my-service
    virtualHosts: my-service-host1, my-service-host2
```

Quarkus application.properties example:
```properties
mesh.gateway.name=my-service
mesh.gateway.virtualHosts=my-service-host1,my-service-host2
```
this mapping will apply the hosts specified in "mesh.gateway.virtualHosts" to every route with "my-service" gateway. Normally mesh.gateway.virtualhosts will have the exact value as ENV_SERVICE_NAME provided by the deployer.

Also annotation `@Route` have field `hosts` which can be used to specify an array of hosts names.
```java
package org.qubership.cloud.sample.controller;

import annotation.org.qubership.cloud.routesregistration.common.Route;
import annotation.route.gateway.spring.org.qubership.cloud.routesregistration.common.GatewayRequestMapping;
//...

@Slf4j
@RestController
@RequestMapping(path = "/api/v1")
@GatewayRequestMapping("/api/v1/sample-service")
@Route(gateways = "my-service", hosts = "my-service-host")
public class IngressRoutesController {
    @GetMapping("/facade")
    public ResponseEntity facadeRoute(HttpServletRequest request) {
        //...
    }
}
```
There are several restrictions on using custom hosts:
*  Don't use mixed facade and border gateways in one annotation if you used custom hosts. Split it into two annotations.
*  If you have several facade routes and use hosts field in the @Route annotation, make sure all your routes use this field or have you set the default mapping in application.yml/application.properties 