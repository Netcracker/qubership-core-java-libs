## rest-api-deprecation-switcher-spring

This library allows to disable deprecated REST API in a Spring Boot microservice. This allows to make deprecated REST endpoints return
TMF error responses with 404 HTTP status code and predefined error code NC-COMMON-2101 as if endpoint has been already removed.
Deprecated REST API is the set of REST endpoints annotated with @java.lang.Deprecated annotation.

### How to deprecate REST API
1. To deprecate all endpoints in the class set @java.lang.Deprecated annotation at the class level
   ~~~
   @RestController
   @RequestMapping("/api/v1")
   @Deprecated
   public class ControllerV1 {
   }
   ~~~
2. To deprecate particular endpoint set @java.lang.Deprecated annotation at this particular endpoint (method)
   ~~~
   @RestController
   @RequestMapping("/api/v2")
   public class ControllerV2 {
      @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
      @Deprecated
      public ResponseEntity<String> apiGet() {
          return new ResponseEntity<>("ok", HttpStatus.OK);
      }
   }
   ~~~

### How to switch off deprecated REST API

1. Add maven dependency
   ~~~
   <dependency>
       <groupId>com.netcracker.cloud</groupId>
       <artifactId>rest-api-deprecation-switcher-spring</artifactId>
   </dependency>
   ~~~

2. At any Spring configuration class add @DisableDeprecatedApi annotation
   ~~~
   import spring.com.netcracker.cloud.disableapi.DisableDeprecatedApi;

   @SpringBootApplication
   @DisableDeprecatedApi
   public static class Application {
      public static void main(String[] args) {
         SpringApplication.run(Application.class, args);
      }
   }
   ~~~
   
3. Add DISABLE_DEPRECATED_API env variable to the Helm templates with default value = false

4. There are 2 options how to specify the set of deprecated API endpoints. You can use only one at a time:
- via @Deprecated annotation 
- via application property.

#### Option #1 - Disable all REST API annotated with @Deprecated annotation:

1. Add the following property to the application.yml file:
   ~~~
   deprecated:
     api:
       disabled: ${DISABLE_DEPRECATED_API:false}
   ~~~

#### Option #2 - Disable REST API via Ant style patterns:

1. Add the following property to the application.yml file:
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
   "id": "9d395c14-0972-43d0-b628-8f2cc633a5b0",
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
