## rest-api-deprecation-switcher-spring

This library allows to disable deprecated JAX RS REST API (Reactive Routes `https://quarkus.io/guides/reactive-routes` not supported) in a Quarkus microservice. This allows to make deprecated REST endpoints return
TMF error responses with 404 HTTP status code and predefined error code NC-COMMON-2101 as if endpoint has been already removed.
Deprecated REST API is the set of REST endpoints annotated with @java.lang.Deprecated annotation.

### How to deprecate REST API

1. To deprecate all endpoints in the class set @java.lang.Deprecated annotation at the class level
   ~~~
   @Path("/api/v1")
   @Deprecated
   public class ControllerV1 {
   }
   ~~~
   
2. To deprecate particular endpoint set @java.lang.Deprecated annotation at this particular endpoint (method)
   ~~~
   @Path("/api/v2/test")
   public class ControllerV2 {
      @GET
      @Deprecated
      public Response apiGet() {
          return Response.ok("ok").build();
      }
   }
   ~~~

### How to disable deprecate REST API

1. Add maven dependency
   ~~~
   <dependency>
       <groupId>org.qubership.cloud.quarkus</groupId>
       <artifactId>rest-api-deprecation-switcher-quarkus</artifactId>
   </dependency>
   ~~~
 
2. Add DISABLE_DEPRECATED_API env variable to the Helm templates with default value = false

3. There are 2 options how to specify the set of deprecated API endpoints. You can use only one at a time:
- via @Deprecated annotation
- via application property.

#### application.properties approach

##### Option #1 - Disable all REST API annotated with @Deprecated annotation:

   Add the following property to the application.properties file:
   ~~~
   deprecated.api.disabled=${DISABLE_DEPRECATED_API:false}
   ~~~

##### Option #2 - Disable REST API via Ant style patterns:

   Add the following property to the application.properties file:
   ~~~
   deprecated.api.disabled=${DISABLE_DEPRECATED_API:false}
   deprecated.api.patterns[0]=/api/v1/** [GET POST PUT DELETE]
   deprecated.api.patterns[1]=/api/v2/**/example/* [GET]
   deprecated.api.patterns[2]=/api/v2/**/test
   ~~~
   'deprecated.api.patterns' property can contain the list of ant path patterns optionally prepended with configuration of deprecated HTTP methods.
   If optional HTTP methods config is not specified for ant path pattern, then all HTTP methods considered to be deprecated and disabled
   This configuration will disable endpoints which paths match provided ant patterns and HTTP methods

#### application.yaml approach
  To use application.yaml instead of application.properties add the following maven dependency
  ~~~
  <dependency>
     <groupId>io.quarkus</groupId>
     <artifactId>quarkus-config-yaml</artifactId>
  </dependency>
   ~~~

##### Option #1 - Disable all REST API annotated with @Deprecated annotation:

   Add the following property to the application.yml file:
   ~~~
   deprecated:
     api:
       disabled: ${DISABLE_DEPRECATED_API:false}
   ~~~

##### Option #2 - Disable REST API via Ant style patterns:

   Add the following property to the application.yml file:
   ~~~
   deprecated:
     api:
       disabled: ${DISABLE_DEPRECATED_API:false}
       patterns:
         - /api/v1/** [GET POST PUT DELETE]
         - /api/v2/**/example/* [GET]
         - /api/v2/**/test 
   ~~~
   'deprecated.api.patterns' property can contain the list of ant path patterns optionally prepended with configuration of deprecated HTTP methods. 
   If optional HTTP methods config is not specified for ant path pattern, then all HTTP methods considered to be deprecated and disabled
   This configuration will disable endpoints which paths match provided ant patterns and HTTP methods

#### 404 TMF response example:
```json
{
   "id": "0a056216-1a2a-486c-a210-d6e06c2ae2ec",
   "referenceError": null,
   "code": "NC-COMMON-2101",
   "reason": "Request is declined with 404 Not Found, because deprecated REST API is disabled",
   "status": "404",
   "source": null,
   "meta": null,
   "errors": null,
   "message": "Request [GET] '/api/v1/test' is declined with 404 Not Found, because the following deprecated REST API is disabled: [[POST, GET]] /api/v1/test",
   "@type": "NC.TMFErrorResponse.v1.0",
   "@schemaLocation": null
}
```
