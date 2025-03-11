Route-registrator
------------------

Route-registrator is the quarkus extension which allows sending requests to control plane for route registration.
The library can register routes in internal, private, public, and facade gateways.
For using the library you should do:

#### 1. Add maven dependency
```xml
     <dependency>
         <groupId>org.qubership.cloud.quarkus</groupId>
         <artifactId>routes-registrator</artifactId>
         <version>${version}</version>
     </dependency>
```
Where version can be found by the link: https://artifactorycn.qubership.org/pd.saas.mvn/org/qubership/cloud/quarkus/routes-registrator/

#### 2. Put annotations
After the first step you should put `@Route(RouteType.<type>)` annotation on a class or method level.
Type value specifies where is the route will be registered. Also you can override the path by which clients send requests to your microservice.
For this you need to add the additional `@Gateway(<path>)` annotation.
For example:

```java
@Path("/customer")
@Route(RouteType.PUBLIC)
@Gateway("/quarkus-quickstart/customer")
public class CustomerResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Customer> getAll() {
        // logic of the method
    }
}
```

The result of the code is the route `GET /customer` wil be registered in public gateway and will available by the `GET /quarkus-quickstart/customer` endpoint.

However, you route can be registered in `facade` gateway. For it, you have to add the `@FacadeRoute` annotation.
In this can, you can also override a registered path by `@FacadeGateway(<path>)` annotation.
For example:

```java
@Path("/customer")
@Route(RouteType.PUBLIC)
@Gateway("/quarkus-quickstart/customer")
@FacadeRoute
@FacadeGateway("/quarkus-quickstart/customer")
public class CustomerResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Customer> getAll() {
        // logic of the method
    }
}
```

##### Specifying custom gateway name (e.g. composite-gateway) (optional)

Both annotations `@Route` and `@FacadeRoute` have field `gateways` which can be used to specify an array of gateway names.
Routes marked with such annotation will be registered in all the specified gateways.
This can be useful to configure composite (ingress) gateway routes as in the example below.
```java
package org.qubership.cloud.sample.controller;

import org.qubership.cloud.routesregistration.common.annotation.Route;
import org.qubership.cloud.routesregistration.common.annotation.Gateway;
//...

@Path("/api/v1")
@Gateway("/api/v1/sample-service")
@Route(gateways = "ingress-service")
@Produces(MediaType.APPLICATION_JSON)
public class IngressRoutesController {
    @GET
    @Path("/common")
    @Route(gateways = {"ingress-service", "private-gateway-service"})
    public ResponseEntity bothPrivateAndCompositeRoute(HttpServletRequest request) {
        //...
    }

    @GET
    @Path("/only-composite")
    public ResponseEntity onlyCompositeRoute(HttpServletRequest request) {
        //...
    }
}
```
In this example route `/api/v1/sample-service/common` will be registered in private, internal and `ingress-service` gateways,
while `/api/v1/sample-service` will be registered only in `ingress-service` gateway.

> :information_source: Please note, that specifying `public-gateway-service`, `private-gateway-service` or `internal-gateway-service`
> in the `gateways` field acts as specifying corresponding route type in `type` field,
> e.g. annotation `@Route(gateways = "private-gateway-service")` will cause route to be sent in private and internal gateways.

If `gateways` field specified in `@Route` or `@FacadeRoute` annotation, `type` (`value`) field of this annotation will be ignored.

**Effective gateway types, `@Gateway` and `@FacadeGateway` annotations**

Since both `@Route` and `@FacadeRoute` annotations have `gateways` field,
it doesn't really matter which of these two annotations to use when specifying gateway names in `gateways` field.
Effective route type will be resolved based on `gateways` value.

If provided gateway name is the same as the microservice family name (cluster name in terms of control-plane),
then route considered to be **facade** route. Gateway path mapping for such route can be affected only by `@FacadeGateway` annotation.

All the other routes' gateway path mappings can be affected only by `@Gateway` annotation.

Problems and workarounds
------------------

#### 1. Subresources (jax-rs)
**Problem** 

Registration of routes works incorrectly in case usage of subresources. 

Info about subresource you may read [here](https://docs.jboss.org/resteasy/docs/1.0.1.GA/userguide/html/JAX-RS_Resource_Locators_and_Sub_Resources.html)

Example case of subresources usage:
```java
@Path(API_V2_UM)
public class RootUmResourceV2 {
  @Route(RouteType.INTERNAL)
  @Path("{realmName}")
  public RealmResourceV2 getRealmResourceV2 () {
    ...
  }
}

public class RealmResourceV2 {
  @POST
  @Route(RouteType.INTERNAL)
  @Path(DISABLE)
  public Response disableRealm() {
    ...
  }
}
```

As you see, we have three separated classes with methods which defined endpoint or it resource-parent.
Method role is subresource in case when method has combination of two annotations - `@Path` and one of next: `@GET, @PUT, @POST, @DELETE, @HEAD`.
Subresource functionality allows concatennation of paths for endpoint to subresource.
In our example we expect next path to endpoint: API_V2_UM/{realmName}/DISABLE.

Usage of this pattern with this lib for routes registration is strongly not recommended.
If you still dare to use this opportunity with the creation routes for subresources, please follow the workaround.

:information_source: Support this functionality will be planned to implementation in future

**Workaround**  

There are two different ways to solve this problem depending on the approach to routes registration:

1) If you prefer to register the routes using Annotation @Route all you need to do is to specify a full endpoint path in subresource, on our example it will look like this:
```java
public class RealmResourceV2 {
  @POST
  @Route(RouteType.INTERNAL)
  @Path(API_V2_UM + "/{realmName}/" + DISABLE)
  public Response disableRealm() {
    ...
  }
}
```

2) In order to programmatically register the routes you need to do the following:
```java
@Inject
RoutesRestRegistrationProcessor routesRestRegistrationProcessor;
...

List<RouteEntry> routes = Collections.singletonList(new RouteEntry(API_V2_UM + "/{realmName}/" + DISABLE, RouteType.PUBLIC));
routesRestRegistrationProcessor.postRoutes(routes)
```

