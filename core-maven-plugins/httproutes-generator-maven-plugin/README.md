# HTTPRoute Generator Maven Plugin

This module contains `com.netcracker.cloud.plugins:httproutes-generator-maven-plugin`.
It scans compiled Java classes and generates Gateway API `HTTPRoute` manifests
for Istio deployments.

## What It Does

- Scans Spring MVC and Quarkus/JAX-RS endpoints from compiled classes.
- Includes only classes/methods annotated with `@Route`.
- Supports gateway path remapping via `@Gateway` and `@GatewayRequestMapping`.
- Groups generated routes by route type (`INTERNAL`, `PRIVATE`, `PUBLIC`, `FACADE`).
- Emits Helm-ready YAML wrapped in:
  `{{- if eq .Values.SERVICE_MESH_TYPE "Istio" }}`.
- Runs as an **aggregator** mojo, so it collects routes across reactor modules.

## Maven Coordinates

- **GroupId:** `com.netcracker.cloud.plugins`
- **ArtifactId:** `httproutes-generator-maven-plugin`
- **Goal:** `generate-routes`
- **Default phase:** `process-classes`

## Plugin Configuration

Add the plugin to your `pom.xml`:

```xml
<build>
  <plugins>
    <plugin>
      <groupId>com.netcracker.cloud.plugins</groupId>
      <artifactId>httproutes-generator-maven-plugin</artifactId>
      <version>1.0.0<version>
      <executions>
        <execution>
          <goals>
            <goal>generate-routes</goal>
          </goals>
        </execution>
      </executions>
      <configuration>
        <packages>
          <package>com.example.service</package>
        </packages>
        <servicePort>8080</servicePort>
        <outputFile>helm-templates/my-service/templates/annotations-httproutes.yaml</outputFile>
        <backendRefVal>{{ .Values.DEPLOYMENT_RESOURCE_NAME }}</backendRefVal>
        <labels>
        </labels>
      </configuration>
    </plugin>
  </plugins>
</build>
```

### Parameters

| Parameter       | Type                 | Default                                  | Description                                                                             |
|-----------------|----------------------|------------------------------------------|-----------------------------------------------------------------------------------------|
| `packages`      | `String[]`           | `com.netcracker`                         | Package prefixes scanned in compiled classes.                                           |
| `servicePort`   | `int`                | `8080`                                   | Backend service port for generated `backendRefs`.                                       |
| `outputFile`    | `String`             | `gateway-httproutes.yaml`                | Output path relative to project base dir.                                               |
| `backendRefVal` | `String`             | `{{ .Values.DEPLOYMENT_RESOURCE_NAME }}` | Backend service name in generated routes.                                               |
| `labels`        | `Map<String,String>` | empty map                                | Custom labels for generated HTTPRoutes metadata. When set, they replace default labels. |

## Supported Annotations

### Spring

- Class/method mappings:
  - `@RequestMapping`, `@GetMapping`, `@PostMapping`, `@PutMapping`,
    `@DeleteMapping`, `@PatchMapping`
- Route metadata:
  - `com.netcracker.cloud.routesregistration.common.annotation.Route`
- Gateway path mapping:
  - `com.netcracker.cloud.routesregistration.common.annotation.Gateway`
  - `com.netcracker.cloud.routesregistration.common.spring.gateway.route.annotation.GatewayRequestMapping`

### Quarkus / JAX-RS

- `@Path` with HTTP method annotations:
  - `@GET`, `@POST`, `@PUT`, `@DELETE`, `@PATCH`
- Route metadata:
  - `@Route`
- Gateway path mapping:
  - `@Gateway`

## Example: Spring Controller

```java
import com.netcracker.cloud.routesregistration.common.annotation.Route;
import com.netcracker.cloud.routesregistration.common.spring.gateway.route.annotation.GatewayRequestMapping;
import com.netcracker.cloud.routesregistration.common.gateway.route.RouteType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@GatewayRequestMapping("/api/users")
@Route(RouteType.PRIVATE)
public class UserController {
    
    @GetMapping("/{id}")
    public User getUser(@PathVariable String id) {
        // ...
    }
    
    @PostMapping
    public User createUser(@RequestBody User user) {
        // ...
    }
}
```

Generated route characteristics for this example:

- match path `/api/users`,
- URL rewrite filter is added because gateway and service paths differ.
- parent refs for `private-gateway`, `internal-gateway-service`.

## Example: Quarkus Resource

```java
import com.netcracker.cloud.routesregistration.common.annotation.Gateway;
import com.netcracker.cloud.routesregistration.common.annotation.Route;
import com.netcracker.cloud.routesregistration.common.gateway.route.RouteType;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/sleep")
@Route(RouteType.PUBLIC)
@Gateway("/test/sleep")
public class SleepResource {

  @GET
  public String sleep() {
    return "ok";
  }
}
```

This produces a `PUBLIC` HTTPRoute rule with:
- match path `/test/sleep`,
- URL rewrite to `/sleep`,
- parent refs for `public-gateway`, `private-gateway`,
  and `internal-gateway-service`.

## Generated YAML Shape

The plugin writes:

1. Header comment (`DO NOT EDIT`),
2. Istio conditional guard:
   `{{- if eq .Values.SERVICE_MESH_TYPE "Istio" }}`,
3. One or more `HTTPRoute` resources grouped by route type.

Snippet:

```yaml
# -----------------------------------------------------------------------------
# THIS FILE WAS AUTOMATICALLY GENERATED — DO NOT EDIT.
# Any changes will be overwritten during the next build.
# Modify source annotations and regenerate using route generation maven plugin.
# -----------------------------------------------------------------------------

{{- if eq .Values.SERVICE_MESH_TYPE "Istio" }}
apiVersion: gateway.networking.k8s.io/v1
kind: HTTPRoute
metadata:
  name: {{ .Values.SERVICE_NAME }}-java-annotations-public
  labels:
    app.kubernetes.io/managed-by: {{ .Values.MANAGED_BY }}
    app.kubernetes.io/name: {{ .Values.SERVICE_NAME }}
    app.kubernetes.io/part-of: {{ .Values.APPLICATION_NAME }}
    app.kubernetes.io/processed-by-operator: istiod
    deployer.cleanup/allow: "true"
    deployment.netcracker.com/sessionId: {{ .Values.DEPLOYMENT_SESSION_ID }}
spec:
  parentRefs:
  - group: gateway.networking.k8s.io
    kind: Gateway
    name: public-gateway
  - group: gateway.networking.k8s.io
    kind: Gateway
    name: private-gateway
  - group: ''
    kind: Service
    name: internal-gateway-service
  rules:
  - matches:
    - path:
        type: PathPrefix
        value: /api/users
    filters:
    - type: URLRewrite
      urlRewrite:
        path:
          type: ReplacePrefixMatch
          replacePrefixMatch: /users
    backendRefs:
    - group: ''
      kind: Service
      name: {{ .Values.DEPLOYMENT_RESOURCE_NAME }}
      port: 8080
      weight: 1
{{- end }}
```

## Timeout Generation

The plugin can generate per-rule HTTPRoute timeouts from `@Route(timeout = ...)`.

- If timeout is greater than `0`, it renders:
  `spec.rules[].timeouts.request`.
- If timeout is `0` (or not set), the `timeouts` block is omitted.
- Timeout value is converted from milliseconds using these rules:
  - divisible by `3600000` -> `<N>h`
  - divisible by `60000` -> `<N>m`
  - divisible by `1000` -> `<N>s`
  - otherwise -> `<N>ms`

Example (`@Route(timeout = 5000)`):

```yaml
rules:
- matches:
  - path:
      type: PathPrefix
      value: /api/test
  backendRefs:
  - group: ''
    kind: Service
    name: {{ .Values.DEPLOYMENT_RESOURCE_NAME }}
    port: 8080
    weight: 1
  timeouts:
    request: 5s
```

## Route Type to parentRefs mapping

Generated HTTPRoute names use:

`{{ .Values.SERVICE_NAME }}-java-annotations-<type>`

Where `<type>` is lowercase route type (`internal`, `private`, `public`, `facade`).

- `INTERNAL` -> Service `internal-gateway-service`
- `PRIVATE` -> Gateway `private-gateway` + Service `internal-gateway-service`
- `PUBLIC` -> Gateway `public-gateway` + Gateway `private-gateway` + Service `internal-gateway-service`
- `FACADE` -> Service `{{ .Values.SERVICE_NAME }}`

## Generated Labels

When `labels` is **not** configured, each generated `HTTPRoute` includes these
default metadata labels:

- `app.kubernetes.io/name: {{ .Values.SERVICE_NAME }}`
- `app.kubernetes.io/part-of: {{ .Values.APPLICATION_NAME }}`
- `app.kubernetes.io/managed-by: {{ .Values.MANAGED_BY }}`
- `deployment.netcracker.com/sessionId: {{ .Values.DEPLOYMENT_SESSION_ID }}`
- `deployer.cleanup/allow: "true"`
- `app.kubernetes.io/processed-by-operator: istiod`

These labels are emitted by the renderer for ownership, tracking, and cleanup
semantics in platform deployments.

You can replace default label set via plugin configuration labels section:

```xml
<configuration>
  <labels>
    <team>platform</team>
    <owner>control-plane</owner>
    <app.kubernetes.io/managed-by>custom-manager</app.kubernetes.io/managed-by>
  </labels>
</configuration>
```

When custom `labels` are provided, they are used as-is and default labels are
not added automatically.

## Route Sorting

Generated rules are sorted by path specificity before rendering:

1. More path segments first (for example `/api/v1/users/profile` before `/api/users`).
2. If segment count is equal, longer path first.
3. If still equal, lexical path order for deterministic output.

This keeps more specific routes ahead of generic ones and makes generated YAML
stable between builds.

## Build and Run

Run with lifecycle:

```bash
mvn clean process-classes
```

Or run goal directly:

```bash
mvn com.netcracker.cloud.plugins:httproutes-generator-maven-plugin:generate-routes
```

## Troubleshooting

### No routes generated

- Ensure classes are compiled (`target/classes` exists for scanned modules).
- Ensure `packages` includes your controllers/resources.
- Ensure classes or methods use `@Route` (without it, routes are ignored).

### Expected gateway rewrite is missing

- Rewrite filters are only generated when `gatewayPath != servicePath`.
- Add `@Gateway` or `@GatewayRequestMapping` to provide gateway path.

### Superclass endpoints not discovered

- Scanner recursively processes superclasses, but those classes still must be
  available in compiled output and inside accepted packages.

### Helm template syntax issues

**Problem**: Generated YAML has invalid Helm templates

**Solution**: The plugin wraps output with:
```yaml
{{- if eq .Values.SERVICE_MESH_TYPE "Istio" }}
# ... routes ...
{{ end }}
```

Ensure your Helm chart defines SERVICE_MESH_TYPE.


## Dependencies

The plugin uses:

- **ClassGraph**: For bytecode scanning and annotation discovery
- **Jackson**: For YAML serialization
- **Maven Plugin API**: For Maven integration

No runtime dependencies required in your application.

## Related Resources
- [Kubernetes Gateway API](https://gateway-api.sigs.k8s.io/)
- [Spring Web Annotations](https://docs.spring.io/spring-framework/docs/current/reference/html/web.html#mvc-ann-requestmapping)
- [Quarkus REST](https://quarkus.io/guides/rest-json)
- [Istio Gateway](https://istio.io/latest/docs/reference/config/networking/gateway/)
