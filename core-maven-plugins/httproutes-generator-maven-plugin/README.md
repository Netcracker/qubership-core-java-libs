# HTTPRoute Generator Maven Plugin

This module contains `com.netcracker.cloud.plugins:httproutes-generator-maven-plugin`.
It scans compiled Java classes and generates Gateway API `HTTPRoute` manifests
and Istio `AuthorizationPolicy` manifests for Istio deployments.

## What It Does

- Scans Spring MVC and Quarkus/JAX-RS endpoints from compiled classes.
- Includes only classes/methods annotated with `@Route`, `@Routes`, `@FacadeRoute` or `@ForbiddenRoute`.
- Supports gateway path remapping via `@Gateway` and `@GatewayRequestMapping`, and facade gateway paths via
  `@FacadeGateway` and `@FacadeGatewayRequestMapping`.
- Generates only `PathPrefix` and `Exact` path matches (see [How Paths Are Matched](#how-paths-are-matched)).
- Groups generated routes by route type (`PUBLIC`, `PRIVATE`, `INTERNAL`, `FACADE`).
- Generates `AuthorizationPolicy` resources with `DENY` rules for paths that must not be reachable
  through a gateway (see [Forbidden Routes and AuthorizationPolicies](#forbidden-routes-and-authorizationpolicies)).
- Compares the legacy mesh routing with the generated Istio routing before writing anything, and fails the build
  when they differ (see [Route Validation](#route-validation)).
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
      <version>1.0.0</version>
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
          <label>
            <key>app.kubernetes.io/name</key>
            <value>{{ .Values.SERVICE_NAME }}</value>
          </label>
        </labels>
      </configuration>
    </plugin>
  </plugins>
</build>
```

### Parameters

| Parameter                           | Type                  | Default                                  | Description                                                                             |
|-------------------------------------|-----------------------|------------------------------------------|-----------------------------------------------------------------------------------------|
| `packages`                          | `String[]`            | `com.netcracker`                         | Package prefixes scanned in compiled classes.                                           |
| `servicePort`                       | `int`                 | `8080`                                   | Backend service port for generated `backendRefs`.                                       |
| `outputFile`                        | `String`              | `gateway-httproutes.yaml`                | Output path relative to project base dir.                                               |
| `backendRefVal`                     | `String`              | `{{ .Values.DEPLOYMENT_RESOURCE_NAME }}` | Backend service name in generated routes.                                               |
| `labels`                            | `List<Label>`         | empty list                               | Custom labels for the metadata of generated HTTPRoutes and AuthorizationPolicies. When set, they replace default labels. Each `<label>` entry has a `<key>` and `<value>` child element, which allows label names containing `/`. |
| `authorizationPolicyPorts`          | `Map<String, String>` | `8080` for every gateway                 | Ports of the generated `DENY` rules, per border gateway. Keys are `public-gateway-service`, `private-gateway-service` and `internal-gateway-service`; values are comma-separated port lists. A gateway that isn't listed uses `8080`. See [Ports](#ports). |
| `autoGenerateAuthorizationPolicies` | `boolean`             | `false`                                  | When `true`, the plugin generates the `DENY` rules that keep the legacy behavior, so you don't have to declare them with `@ForbiddenRoute`. See [Automatic DENY rules](#automatic-deny-rules). |

Example that sets the ports for one gateway and enables automatic DENY rules:

```xml
<configuration>
  <packages>
    <package>com.example.service</package>
  </packages>
  <authorizationPolicyPorts>
    <internal-gateway-service>8080,8443</internal-gateway-service>
  </authorizationPolicyPorts>
  <autoGenerateAuthorizationPolicies>true</autoGenerateAuthorizationPolicies>
</configuration>
```

Here `public-gateway-service` and `private-gateway-service` use `8080`.

## Supported Annotations

### Spring

- Class/method mappings:
  - `@RequestMapping`, `@GetMapping`, `@PostMapping`, `@PutMapping`,
    `@DeleteMapping`, `@PatchMapping`
- Gateway path mapping:
  - `com.netcracker.cloud.routesregistration.common.annotation.Gateway`
  - `com.netcracker.cloud.routesregistration.common.spring.gateway.route.annotation.GatewayRequestMapping`
- Facade gateway path mapping:
  - `com.netcracker.cloud.routesregistration.common.annotation.FacadeGateway`
  - `com.netcracker.cloud.routesregistration.common.spring.gateway.route.annotation.FacadeGatewayRequestMapping`

### Quarkus / JAX-RS

- `@Path` with HTTP method annotations:
  - `@GET`, `@POST`, `@PUT`, `@DELETE`, `@PATCH`
- Gateway path mapping:
  - `@Gateway`
- Facade gateway path mapping:
  - `@FacadeGateway`

### Route metadata (both frameworks)

All in `com.netcracker.cloud.routesregistration.common.annotation`:

- `@Route`: the route type (`PUBLIC`, `PRIVATE`, `INTERNAL`, `FACADE`), `timeout` and `gateways`.
  Several `@Route` annotations on one element (the `@Routes` container) are all read.
- `@FacadeRoute`: the same as `@Route(RouteType.FACADE)`, with its own `gateways`.
- `@ForbiddenRoute`: gateways on which the gateway path of the element must not be reachable
  (see [Forbidden Routes and AuthorizationPolicies](#forbidden-routes-and-authorizationpolicies)).

A method-level route list replaces the class-level list, as in the legacy route registration.
Each `@Route`/`@FacadeRoute` entry becomes these routes:

- no `gateways`, type `PUBLIC`/`PRIVATE`/`INTERNAL`: a route of that type, with the gateway path from
  `@Gateway`/`@GatewayRequestMapping`;
- no `gateways`, type `FACADE`: a **facade route**, with the gateway path from
  `@FacadeGateway`/`@FacadeGatewayRequestMapping`, or the service path when there is none;
- every name in `gateways`: `public-gateway-service`, `private-gateway-service` and `internal-gateway-service`
  give a route of type `PUBLIC`, `PRIVATE` and `INTERNAL`. Every other name is a composite gateway and gives a
  **composite route**, with the gateway path from `@Gateway`/`@GatewayRequestMapping`.

`hosts` are ignored: Istio has no composite gateways and no virtual hosts on a Service-bound HTTPRoute.
`hosts` together with a border gateway name in `gateways` is a `LEGACY_INVALID` error, because the legacy runtime
rejects it too ("Only composite gateway can have hosts").

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

- match `PathPrefix /api/users` (the `/{id}` route is cut to the same prefix and merged with it),
- URL rewrite filter `ReplacePrefixMatch /users`, because gateway and service paths differ,
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

## How Paths Are Matched

The legacy mesh matched every gateway path as a pattern: a variable such as `{id}` matched one path segment, and
the longest matching gateway path won. The generated HTTPRoutes use only `PathPrefix` and `Exact` matches, which
Istio evaluates natively.

### The cut

Every gateway path is **cut** at its first variable: the match is the part before the segment that contains the
first `{`, without its trailing slash, or `/` if nothing is left. A path without variables only loses its trailing
slash, because the legacy mesh matched `/a/` and `/a` alike, and Istio ignores the trailing slash of a `PathPrefix`.

| Gateway path                    | Match                        |
|---------------------------------|------------------------------|
| `/api/v1/svc/items`             | `PathPrefix /api/v1/svc/items` |
| `/api/v1/svc/items/`            | `PathPrefix /api/v1/svc/items` |
| `/api/v1/svc/items/{id}`        | `PathPrefix /api/v1/svc/items` |
| `/api/v1/svc/users/{id}/profile`| `PathPrefix /api/v1/svc/users` |
| `/{tenant}/items`               | `PathPrefix /`               |

A `PathPrefix` match also matches every path below it, so a cut rule routes more requests than the legacy pattern
did. [Route Validation](#route-validation) finds every such request and fails the build when the legacy mesh didn't
route it.

### Rewrite derivation

When the gateway path and the service path differ, the service path is cut at its first variable too, and the result
becomes the `ReplacePrefixMatch` rewrite. For example, `/api/v1/{id}/items` → `/items/{id}/items` gives match
`PathPrefix /api/v1` and rewrite `ReplacePrefixMatch /items`.

`ReplacePrefixMatch` keeps the part of the request after the match unchanged, so this is correct only when the part
after the cut is the same in both paths (variables are compared by position). This holds for the routes services
declare in practice, and **the plugin doesn't check it**.

### Merging and the Exact split

Routes cut to the same match are grouped together, whatever their route types, because Istio can't tell apart two
`PathPrefix` rules with the same value, even in different HTTPRoutes:

1. If they all have the same rewrite and timeout, they become one rule.
2. If exactly one route has the gateway path `P` itself (or `P/`, such as a Spring `@GetMapping("/")` in a controller
   mapped to `P`), at least one other route has `P/{var}`, and all the other routes share one rewrite, the route with `P` is split into two `Exact` rules with `ReplaceFullPath`, one for `P`
   and one for `P/`. The other routes are merged into the `PathPrefix P` rule. In legacy, the longer `P/{var}` route
   takes every request below `P/`, so the route with `P` only ever received `P` and `P/`.
3. If they all have the same rewrite but different timeouts, they are merged with the largest timeout, and a
   `TIMEOUT_MERGE` warning is logged.
4. Otherwise, one rule can't express them: the plugin reports a `CONFLICT` error.

A merged rule goes into the HTTPRoute of the widest route type, because `PUBLIC` routes are exposed on every gateway
that `PRIVATE` and `INTERNAL` routes are exposed on. On the wider gateways, the narrower routes of the group must be
forbidden by DENY rules; [Route Validation](#route-validation) reports each such request as `EXPOSURE`.

Example of the Exact split: the `PUBLIC` routes `/api/v1/svc/items` → `/v1/items` and
`/api/v1/svc/items/{id}` → `/v2/items/{id}` give these rules:

```yaml
  rules:
  - matches:
    - path:
        type: "Exact"
        value: "/api/v1/svc/items/"
    filters:
    - type: "URLRewrite"
      urlRewrite:
        path:
          type: "ReplaceFullPath"
          replaceFullPath: "/v1/items/"
    backendRefs:
    - group: ""
      kind: "Service"
      name: "{{ .Values.DEPLOYMENT_RESOURCE_NAME }}"
      port: 8080
      weight: 1
  - matches:
    - path:
        type: "Exact"
        value: "/api/v1/svc/items"
    filters:
    - type: "URLRewrite"
      urlRewrite:
        path:
          type: "ReplaceFullPath"
          replaceFullPath: "/v1/items"
    backendRefs:
    - group: ""
      kind: "Service"
      name: "{{ .Values.DEPLOYMENT_RESOURCE_NAME }}"
      port: 8080
      weight: 1
  - matches:
    - path:
        type: "PathPrefix"
        value: "/api/v1/svc/items"
    filters:
    - type: "URLRewrite"
      urlRewrite:
        path:
          type: "ReplacePrefixMatch"
          replacePrefixMatch: "/v2/items"
    backendRefs:
    - group: ""
      kind: "Service"
      name: "{{ .Values.DEPLOYMENT_RESOURCE_NAME }}"
      port: 8080
      weight: 1
```

## Route Validation

Before it writes any file, the plugin simulates the legacy mesh routing and the generated Istio routing (HTTPRoutes
and AuthorizationPolicies) on each border gateway (`public-gateway`, `private-gateway`, `internal-gateway-service`)
for a set of sample requests derived from the declared paths. When the result differs in a way that matters, or the
annotations are invalid, the build fails and the output file is left unchanged.

In legacy, a route of a narrower type was registered as forbidden on the wider gateways: for example an `INTERNAL`
route returned 404 on `public-gateway` and `private-gateway`, even when a shorter `PUBLIC` route covered its path.
Validation compares routed/forbidden, the upstream path (after the rewrite) and the timeout.

Every finding is logged as a block that starts with `[ROUTE-MIGRATION] <KIND> on <gateway>`, followed by the request,
the legacy and Istio decisions, the problem and the possible fixes. The build then fails with a summary, for example:

```
Route migration validation failed with 5 errors (5 EXPOSURE), see the [ROUTE-MIGRATION] errors in the build log
```

| Kind                          | Severity | Meaning                                                                                                   |
|-------------------------------|----------|-----------------------------------------------------------------------------------------------------------|
| `EXPOSURE`                    | error    | Legacy returned 404 for a request, and the generated routing routes it.                                   |
| `CONFLICT`                    | error    | Routes cut to the same match need different rewrites, or legacy and Istio forward a request to different upstream paths. |
| `LOST_ROUTE`                  | error    | Legacy routes a request, and the generated routing doesn't (for example a `DENY` rule that is too wide).  |
| `LEGACY_INVALID`              | error    | The annotations are invalid for the legacy runtime too.                                                   |
| `LEGACY_PRECEDENCE_UNDEFINED` | error    | Legacy routing of a request depends on an undefined order.                                                |
| `CONTRADICTORY_DECLARATION`   | error    | `@ForbiddenRoute` forbids the gateway path of a route that is allowed on that gateway.                    |
| `INVALID_FORBIDDEN_ROUTE`     | error    | `@ForbiddenRoute` is empty or lists `FACADE`, or its gateway path has `*` wildcards, regex-constrained variables such as `{id:\d+}`, or variables that don't take up a whole segment. |
| `TIMEOUT_CHANGE`              | warning  | Legacy and the generated rule use different timeouts for a request.                                       |
| `TIMEOUT_MERGE`               | warning  | Routes with different timeouts were merged into one rule with the largest timeout.                        |
| `TIMEOUT_NOT_APPLIED`         | warning  | Facade or composite routes have timeouts, and no service-bound HTTPRoute is generated.                    |

### Example: exposure of a narrower route

`ResourceController` is a `PUBLIC` controller at `/api/v1/my-service/resource` with an `INTERNAL` method at
`/{var1}/internal-api`. The generated rule `PathPrefix /api/v1/my-service/resource` routes the internal API on
every gateway:

```
[ERROR] [ROUTE-MIGRATION] EXPOSURE on public-gateway
    request: /api/v1/my-service/resource/x-probe-0/internal-api
    legacy:  FORBIDDEN (implicit: INTERNAL route /api/v1/my-service/resource/{var1}/internal-api -> /resource/{var1}/internal-api (from com.example.ResourceController#internalApi))
    istio:   ROUTED by PathPrefix /api/v1/my-service/resource -> /resource (from com.example.ResourceController, com.example.ResourceController#get, com.example.ResourceController#internalApi, com.example.ResourceController#status)
    problem: legacy forbids this request on public-gateway, and the generated rule routes it
    fix:     add @ForbiddenRoute({RouteType.PUBLIC, RouteType.PRIVATE}) to com.example.ResourceController#internalApi
    fix:     or set <autoGenerateAuthorizationPolicies>true</autoGenerateAuthorizationPolicies> in the plugin configuration to generate the DENY rule automatically
```

### Example: exposure caused by the cut

`OrderController` declares only `/api/v1/my-service/order/{var1}/items`. It is cut to
`PathPrefix /api/v1/my-service/order`, which also routes `/api/v1/my-service/order` itself:

```
[ERROR] [ROUTE-MIGRATION] EXPOSURE on public-gateway
    request: /api/v1/my-service/order
    legacy:  NOT ROUTED (404)
    istio:   ROUTED by PathPrefix /api/v1/my-service/order -> /order (from com.example.OrderController#items)
    problem: legacy doesn't route this request on public-gateway, and the generated rule PathPrefix /api/v1/my-service/order routes it, because the gateway path was cut at its first variable
    fix:     add @ForbiddenRoute({RouteType.PUBLIC, RouteType.PRIVATE, RouteType.INTERNAL}) to class com.example.OrderController
    fix:     or set <autoGenerateAuthorizationPolicies>true</autoGenerateAuthorizationPolicies> in the plugin configuration to generate the DENY rule automatically
```

The fix names the class whose mapping equals the cut prefix. When there is no such class, it names a method mapped
to it, or asks for a class or method mapped to the prefix.

### Example: exposure that no DENY rule can fix

`FileController` is a `PUBLIC` controller at `/api/v1/my-service/files` with a `PRIVATE` method at `/{name}.txt`.
Neither `@ForbiddenRoute` nor an AuthorizationPolicy path can express a variable that takes up only part of a segment,
so no DENY rule is generated for it, even with `autoGenerateAuthorizationPolicies`:

```
[ERROR] [ROUTE-MIGRATION] EXPOSURE on public-gateway
    request: /api/v1/my-service/files/x-probe-0.txt
    legacy:  FORBIDDEN (implicit: PRIVATE route /api/v1/my-service/files/{name}.txt -> /files/{name}.txt (from com.example.FileController#text))
    istio:   ROUTED by PathPrefix /api/v1/my-service/files -> /files (from com.example.FileController, com.example.FileController#text)
    problem: legacy forbids this request on public-gateway, and the generated rule routes it
    fix:     change the gateway path /api/v1/my-service/files/{name}.txt of com.example.FileController#text, which neither @ForbiddenRoute nor an automatic DENY rule can forbid: forbidden paths need whole-segment variables, a variable must take up a whole path segment
```

### Example: conflict

```
[ERROR] [ROUTE-MIGRATION] CONFLICT on public-gateway, private-gateway, internal-gateway-service
    match:   PathPrefix /api/v1/svc/items
    request: /api/v1/svc/items/x-probe-0/details
    route:   PUBLIC route /api/v1/svc/items/{id} -> /items/{id} (from com.acme.ItemController#get) [ReplacePrefixMatch /items]
    route:   PUBLIC route /api/v1/svc/items/{id}/details -> /details/{id} (from com.acme.ItemController#details) [ReplacePrefixMatch /details]
    problem: these routes are cut to PathPrefix /api/v1/svc/items and need different rewrites, which one Istio rule can't express
    fix:     align the gateway paths or service paths of these routes so that they share one rewrite, or migrate the affected waypoint manually to VirtualService outside this plugin
```

### Legacy-invalid routes

Two routes allowed on the same gateway with the same gateway path and different service paths are rejected by the
legacy runtime too ("several target paths for forwarding from the same source path"). They are reported once per
gateway and pattern:

```
[ERROR] [ROUTE-MIGRATION] LEGACY_INVALID on public-gateway
    path:    /api/v1/svc/items
    route:   PUBLIC route /api/v1/svc/items -> /items (from com.acme.ItemController)
    route:   PUBLIC route /api/v1/svc/items -> /v2/items (from com.acme.ItemV2Controller)
    problem: the routes are allowed on public-gateway with the same gateway path and different service paths; the legacy runtime rejects this too ("several target paths for forwarding from the same source path")
    fix:     use one service path for gateway path /api/v1/svc/items, or change the gateway path of one of the routes
```

The same gateway path with the same service path and different timeouts is valid; it is merged with the largest
timeout.

Legacy has no defined behavior for such a gateway path, so `CONFLICT` and `LEGACY_PRECEDENCE_UNDEFINED` findings that
involve it are not reported. They can show up once the `LEGACY_INVALID` error is fixed.

### Undefined legacy precedence

When gateway paths of the same length match the same request with different results, legacy picked one of them in an
undefined order. When they differ only in timeout, the largest timeout is used and nothing is reported. Otherwise:

```
[ERROR] [ROUTE-MIGRATION] LEGACY_PRECEDENCE_UNDEFINED on public-gateway
    request: /api/v1/svc/info/info
    entry:   FORBIDDEN (implicit: INTERNAL route /api/v1/svc/info/{id} -> /info/{id} (from com.acme.InfoController#get))
    entry:   ROUTED by PUBLIC route /api/v1/svc/{id}/info -> /items/{id}/info (from com.acme.ItemController#info)
    problem: these legacy entries have gateway paths of the same length that match the request with different results, so legacy picks one of them in an undefined order
    fix:     change the gateway path of one of these routes so that only one of them matches such requests
```

No other finding is reported for such requests.

### What is not checked

- Overlaps with routes of **other services**. The plugin sends every rule to the single backend
  (`backendRefVal`:`servicePort`) and doesn't know the routes of other services on the same gateways.
- Whether the part after the cut is the same in the gateway path and the service path
  (see [Rewrite derivation](#rewrite-derivation)).

## Forbidden Routes and AuthorizationPolicies

### `@ForbiddenRoute`

`@ForbiddenRoute` (in `com.netcracker.cloud.routesregistration.common.annotation`, route-registration-common) marks
the gateway path of a class or method as forbidden on the listed gateways: `PUBLIC`, `PRIVATE` and/or `INTERNAL`.
The legacy route registration ignores it. The gateway path is resolved like the gateway path of a route on the same
element, so `@ForbiddenRoute` can be used on a class or method without `@Route`.

```java
@RestController
@RequestMapping("/resource")
@GatewayRequestMapping("/api/v1/my-service/resource")
@Route(RouteType.PUBLIC)
public class ResourceController {

    @GetMapping("/{var1}")
    @GatewayRequestMapping("/{var1}")
    @Route(RouteType.PUBLIC)
    public void get() {
    }

    @GetMapping("/{var1}/internal-api")
    @GatewayRequestMapping("/{var1}/internal-api")
    @Route(RouteType.INTERNAL)
    @ForbiddenRoute({RouteType.PUBLIC, RouteType.PRIVATE})
    public void internalApi() {
    }

    @GetMapping("/{var1}/internal-api/status")
    @GatewayRequestMapping("/{var1}/internal-api/status")
    @Route(RouteType.PUBLIC)
    public void status() {
    }
}

@RestController
@RequestMapping("/order")
@GatewayRequestMapping("/api/v1/my-service/order")
@ForbiddenRoute({RouteType.PUBLIC, RouteType.PRIVATE, RouteType.INTERNAL})
public class OrderController {

    @GetMapping("/{var1}/items")
    @GatewayRequestMapping("/{var1}/items")
    @Route(RouteType.PUBLIC)
    public void items() {
    }
}
```

`@ForbiddenRoute` must not forbid a route's own gateway path on a gateway where that route is allowed
(`CONTRADICTORY_DECLARATION`).

### AuthorizationPolicy output

For each gateway with at least one DENY rule, the plugin generates one `AuthorizationPolicy` named
`{{ .Values.SERVICE_NAME }}-java-annotations-deny-<public|private|internal>`, with `action: DENY`, bound to
Gateway `public-gateway`, Gateway `private-gateway` or Service `internal-gateway-service`. No policy is generated
when there are no DENY rules. Each forbidden path `F` gives one rule:

- `paths`: `F` and everything below it, with each variable replaced by `{*}`;
- `notPaths`: every longer route allowed on that gateway that matches some of the same requests as `F`, and
  everything below it, so these routes stay reachable. The paths are compared segment by segment, and a variable
  segment in one of them matches a literal segment in the other: for `F` = `/a/lit/x`, the `PUBLIC` route
  `/a/{id}/x/y` gives the `notPaths` `/a/{*}/x/y` and `/a/{*}/x/y/{**}`;
- `ports`: the ports of that gateway from `authorizationPolicyPorts`.

For the controllers above, the generated file has the `PUBLIC` HTTPRoute with the rules
`PathPrefix /api/v1/my-service/resource` → `/resource` and `PathPrefix /api/v1/my-service/order` → `/order`, followed
by three policies. The public one is:

```yaml
---
apiVersion: "security.istio.io/v1"
kind: "AuthorizationPolicy"
metadata:
  name: "{{ .Values.SERVICE_NAME }}-java-annotations-deny-public"
  labels:
    app.kubernetes.io/managed-by: "{{ .Values.MANAGED_BY }}"
    app.kubernetes.io/name: "{{ .Values.SERVICE_NAME }}"
    app.kubernetes.io/part-of: "{{ .Values.APPLICATION_NAME }}"
    app.kubernetes.io/processed-by-operator: "istiod"
    deployer.cleanup/allow: "true"
    deployment.netcracker.com/sessionId: "{{ .Values.DEPLOYMENT_SESSION_ID }}"
spec:
  targetRefs:
  - group: "gateway.networking.k8s.io"
    kind: "Gateway"
    name: "public-gateway"
  action: "DENY"
  rules:
  - to:
    - operation:
        ports:
        - "8080"
        paths:
        - "/api/v1/my-service/order"
        - "/api/v1/my-service/order/{**}"
        notPaths:
        - "/api/v1/my-service/order/{*}/items"
        - "/api/v1/my-service/order/{*}/items/{**}"
  - to:
    - operation:
        ports:
        - "8080"
        paths:
        - "/api/v1/my-service/resource/{*}/internal-api"
        - "/api/v1/my-service/resource/{*}/internal-api/{**}"
        notPaths:
        - "/api/v1/my-service/resource/{*}/internal-api/status"
        - "/api/v1/my-service/resource/{*}/internal-api/status/{**}"
```

`deny-private` has the same rules, bound to `private-gateway`. `deny-internal` has only the order rule, bound to
Service `internal-gateway-service`.

### Ports

A DENY rule applies only to requests on the listed ports, so each entry of `authorizationPolicyPorts` must match the
listener port of that Gateway, or the port of `internal-gateway-service`. A gateway that isn't listed uses `8080`.
Whitespace around ports is trimmed, and duplicate ports are removed. These configuration errors fail the build before
scanning:

- `Unknown gateway name 'facade-gateway' in <authorizationPolicyPorts>, allowed names are public-gateway-service, private-gateway-service, internal-gateway-service`
- `Empty port list for 'internal-gateway-service' in <authorizationPolicyPorts>`
- `Invalid port 'abc' for 'internal-gateway-service' in <authorizationPolicyPorts>, ports must be numbers from 1 to 65535`

### Automatic DENY rules

With `autoGenerateAuthorizationPolicies` set to `true`, the plugin adds DENY rules for the border gateways itself:

- for every route that legacy forbade on a wider gateway because of its narrower type, when the generated HTTPRoutes
  would route it there. The rule is built like a `@ForbiddenRoute` rule, with `notPaths` for longer allowed routes
  that overlap it. A gateway path that `@ForbiddenRoute` can't express, such as `/files/{name}.txt`, gets no rule;
  its exposure is reported (see
  [Example: exposure that no DENY rule can fix](#example-exposure-that-no-deny-rule-can-fix));
- for every cut match `P` of a route with a variable, when legacy didn't route `P` on that gateway: a rule for `P`
  and everything below it, with `notPaths` for the longer allowed routes that overlap `P`. Only `P` itself is checked,
  so if a route shorter than `P` routed requests below `P` in legacy, the rule denies them, and the build fails with
  `LOST_ROUTE`.

For example, a `PUBLIC` route `/{t}/{a}/{b}/{c}/{d}` next to `/api/v1/my-service/order/{var1}/items`:

```
[ERROR] [ROUTE-MIGRATION] LOST_ROUTE on public-gateway
    request: /api/v1/my-service/order/x-probe-0
    legacy:  ROUTED by PUBLIC route /{t}/{a}/{b}/{c}/{d} (from com.example.TenantController#any)
    istio:   DENIED by DENY /api/v1/my-service/order, /api/v1/my-service/order/{**} except /api/v1/my-service/order/{*}/items, /api/v1/my-service/order/{*}/items/{**} (from auto: cut exposure of /api/v1/my-service/order from com.example.OrderController#items)
    problem: legacy routes this request, and the automatic DENY rule from auto: cut exposure of /api/v1/my-service/order from com.example.OrderController#items denies it
    fix:     change the gateway path of this route or of the routes the DENY rule comes from; the rule was generated automatically, so there is no @ForbiddenRoute declaration to narrow
```

For the example above without any `@ForbiddenRoute`, it generates the same policies. An explicit `@ForbiddenRoute`
rule wins over an identical automatic one. The combined result is validated like explicit rules, so the build still
fails on whatever they don't fix.

The default is `false`, because DENY rules on shared border gateways are a security-relevant change that service
owners should opt into explicitly. By default, the build reports these cases and offers both fixes.

### DENY rules on shared gateways

The border gateways are shared by all services. A DENY rule on a gateway also applies to paths of other services under
the same prefix. This is safe as long as gateway paths are namespaced per service (`/api/<version>/<service>/...`).

## Facade and Composite Routes

Istio has no facade or composite gateways. Clients call the Service `{{ .Values.SERVICE_NAME }}` directly, and
Istio sends every request for it to the service unchanged, unless an HTTPRoute is bound to the Service. So the
facade and composite routes (see [Route metadata](#route-metadata-both-frameworks)) are planned together, with the
same cut, merging and conflict rules as a gateway, but they are not compared with legacy routing, and no
AuthorizationPolicies are generated for them.

- If **no** facade or composite rule has a rewrite, no service-bound HTTPRoute is generated. If any of these routes has
  a timeout, the timeout is not applied, and a warning says so:

  ```
  [WARNING] [ROUTE-MIGRATION] TIMEOUT_NOT_APPLIED on service-bound HTTPRoute
      route:   FACADE route /orders (from com.acme.OrderController#list) [timeout 30s]
      problem: no facade or composite route has a rewrite, so no service-bound HTTPRoute is generated, and the timeouts of these routes are not applied
  ```

- If **any** of them has a rewrite, one HTTPRoute `{{ .Values.SERVICE_NAME }}-java-annotations-facade` is generated,
  bound only to Service `{{ .Values.SERVICE_NAME }}`, and it lists **all** facade and composite rules, including those
  without a rewrite. While it exists, Istio returns **404 for every other path** through the Service. To fix that,
  declare those endpoints as facade routes, or call them through a border gateway.

Legacy facade and composite gateways were separate gateways, so two such routes with the same gateway path and
different service paths were valid there. In the service-bound HTTPRoute they collide, and the plugin reports a
`CONFLICT` on `service-bound HTTPRoute`.

Example: a method with `@FacadeRoute` and `@FacadeGatewayRequestMapping("/facade/items")` on `/items`, a method with
`@Route(RouteType.FACADE)` on `/orders`, and a method on `/shared` with `@GatewayRequestMapping("/api/v1/svc/shared")`,
`@Route(RouteType.PUBLIC)` and `@Route(gateways = "composite-gw", hosts = "example.com")` give the `PUBLIC` HTTPRoute
with the rule `PathPrefix /api/v1/svc/shared` → `/shared`, and this service-bound HTTPRoute:

```yaml
---
apiVersion: "gateway.networking.k8s.io/v1"
kind: "HTTPRoute"
metadata:
  name: "{{ .Values.SERVICE_NAME }}-java-annotations-facade"
  labels:
    app.kubernetes.io/managed-by: "{{ .Values.MANAGED_BY }}"
    app.kubernetes.io/name: "{{ .Values.SERVICE_NAME }}"
    app.kubernetes.io/part-of: "{{ .Values.APPLICATION_NAME }}"
    app.kubernetes.io/processed-by-operator: "istiod"
    deployer.cleanup/allow: "true"
    deployment.netcracker.com/sessionId: "{{ .Values.DEPLOYMENT_SESSION_ID }}"
spec:
  parentRefs:
  - group: ""
    kind: "Service"
    name: "{{ .Values.SERVICE_NAME }}"
  rules:
  - matches:
    - path:
        type: "PathPrefix"
        value: "/api/v1/svc/shared"
    filters:
    - type: "URLRewrite"
      urlRewrite:
        path:
          type: "ReplacePrefixMatch"
          replacePrefixMatch: "/shared"
    backendRefs:
    - group: ""
      kind: "Service"
      name: "{{ .Values.DEPLOYMENT_RESOURCE_NAME }}"
      port: 8080
      weight: 1
  - matches:
    - path:
        type: "PathPrefix"
        value: "/facade/items"
    filters:
    - type: "URLRewrite"
      urlRewrite:
        path:
          type: "ReplacePrefixMatch"
          replacePrefixMatch: "/items"
    backendRefs:
    - group: ""
      kind: "Service"
      name: "{{ .Values.DEPLOYMENT_RESOURCE_NAME }}"
      port: 8080
      weight: 1
  - matches:
    - path:
        type: "PathPrefix"
        value: "/orders"
    filters: []
    backendRefs:
    - group: ""
      kind: "Service"
      name: "{{ .Values.DEPLOYMENT_RESOURCE_NAME }}"
      port: 8080
      weight: 1
```

## Generated YAML Shape

The plugin writes:

1. Header comment (`DO NOT EDIT`),
2. Istio conditional guard:
   `{{- if eq .Values.SERVICE_MESH_TYPE "Istio" }}`,
3. `HTTPRoute` resources grouped by route type, in the order public, private, internal, facade,
4. `AuthorizationPolicy` resources, in the order public, private, internal.

Snippet:

```yaml
# -----------------------------------------------------------------------------
# THIS FILE WAS AUTOMATICALLY GENERATED — DO NOT EDIT.
# Any changes will be overwritten during the next build.
# Modify source annotations and regenerate using route generation maven plugin.
# -----------------------------------------------------------------------------

{{- if eq .Values.SERVICE_MESH_TYPE "Istio" }}
---
apiVersion: "gateway.networking.k8s.io/v1"
kind: "HTTPRoute"
metadata:
  name: "{{ .Values.SERVICE_NAME }}-java-annotations-public"
  labels:
    app.kubernetes.io/managed-by: "{{ .Values.MANAGED_BY }}"
    app.kubernetes.io/name: "{{ .Values.SERVICE_NAME }}"
    app.kubernetes.io/part-of: "{{ .Values.APPLICATION_NAME }}"
    app.kubernetes.io/processed-by-operator: "istiod"
    deployer.cleanup/allow: "true"
    deployment.netcracker.com/sessionId: "{{ .Values.DEPLOYMENT_SESSION_ID }}"
spec:
  parentRefs:
  - group: "gateway.networking.k8s.io"
    kind: "Gateway"
    name: "public-gateway"
  - group: "gateway.networking.k8s.io"
    kind: "Gateway"
    name: "private-gateway"
  - group: ""
    kind: "Service"
    name: "internal-gateway-service"
  rules:
  - matches:
    - path:
        type: "PathPrefix"
        value: "/api/v1/my-service/resource"
    filters:
    - type: "URLRewrite"
      urlRewrite:
        path:
          type: "ReplacePrefixMatch"
          replacePrefixMatch: "/resource"
    backendRefs:
    - group: ""
      kind: "Service"
      name: "{{ .Values.DEPLOYMENT_RESOURCE_NAME }}"
      port: 8080
      weight: 1
---
apiVersion: "security.istio.io/v1"
kind: "AuthorizationPolicy"
# ...
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
- When routes with different timeouts are merged into one rule, the largest timeout is used
  (see [Merging and the Exact split](#merging-and-the-exact-split)).

Example (`@Route(timeout = 5000)`):

```yaml
  rules:
  - matches:
    - path:
        type: "PathPrefix"
        value: "/api/test"
    filters: []
    backendRefs:
    - group: ""
      kind: "Service"
      name: "{{ .Values.DEPLOYMENT_RESOURCE_NAME }}"
      port: 8080
      weight: 1
    timeouts:
      request: "5s"
```

## Route Type to parentRefs mapping

Generated HTTPRoute names use:

`{{ .Values.SERVICE_NAME }}-java-annotations-<type>`

Where `<type>` is lowercase route type (`public`, `private`, `internal`, `facade`).

- `PUBLIC` -> Gateway `public-gateway` + Gateway `private-gateway` + Service `internal-gateway-service`
- `PRIVATE` -> Gateway `private-gateway` + Service `internal-gateway-service`
- `INTERNAL` -> Service `internal-gateway-service`
- `FACADE` -> Service `{{ .Values.SERVICE_NAME }}`, only when a facade or composite rule has a rewrite
  (see [Facade and Composite Routes](#facade-and-composite-routes))

## Generated Labels

When `labels` is **not** configured, each generated `HTTPRoute` and `AuthorizationPolicy` includes these
default metadata labels:

- `app.kubernetes.io/name: {{ .Values.SERVICE_NAME }}`
- `app.kubernetes.io/part-of: {{ .Values.APPLICATION_NAME }}`
- `app.kubernetes.io/managed-by: {{ .Values.MANAGED_BY }}`
- `deployment.netcracker.com/sessionId: {{ .Values.DEPLOYMENT_SESSION_ID }}`
- `deployer.cleanup/allow: "true"`
- `app.kubernetes.io/processed-by-operator: istiod`

These labels are emitted by the renderer for ownership, tracking, and cleanup
semantics in platform deployments.

You can replace the default label set via the plugin configuration `labels` section.
Each entry uses a `<label>` element with nested `<key>` and `<value>` children,
which allows label names containing `/` (common in Kubernetes label conventions):

```xml
<configuration>
  <labels>
    <label>
      <key>team</key>
      <value>platform</value>
    </label>
    <label>
      <key>owner</key>
      <value>control-plane</value>
    </label>
    <label>
      <key>app.kubernetes.io/managed-by</key>
      <value>custom-manager</value>
    </label>
  </labels>
</configuration>
```

When custom `labels` are provided, they are used as-is and default labels are
not added automatically.

## Route Sorting

Within each HTTPRoute, rules are sorted by their match value before rendering:

1. More path segments first (for example `/api/v1/users/profile` before `/api/users`).
2. If segment count is equal, longer path first.
3. If still equal, lexical path order.
4. For the same value, `Exact` before `PathPrefix`.

Istio picks `Exact` matches first and then the longest `PathPrefix`, so the order doesn't change routing; it makes
the generated YAML stable between builds. Each match value appears in only one HTTPRoute (see
[Merging and the Exact split](#merging-and-the-exact-split)).

## Differences from Legacy Routing

- **403 instead of 404.** A request denied by an `AuthorizationPolicy` returns `403 Forbidden`, where the legacy mesh
  returned `404 Not Found`.
- **Port scoping.** DENY rules apply only to the ports in `authorizationPolicyPorts`. A request on another port of the
  gateway is not denied (see [Ports](#ports)).
- **Shared gateways.** DENY rules also apply to other services' paths under the same prefix
  (see [DENY rules on shared gateways](#deny-rules-on-shared-gateways)).
- **Facade gateway paths.** Facade routes take their gateway path from `@FacadeGateway`/`@FacadeGatewayRequestMapping`,
  or use the service path, as in legacy. Earlier plugin versions used `@Gateway`/`@GatewayRequestMapping` paths for them.
- **Service-bound HTTPRoute.** While it exists, other paths through the Service return 404
  (see [Facade and Composite Routes](#facade-and-composite-routes)).

## Migrating Existing Services

Validation and AuthorizationPolicy generation are introduced in a new major version of the plugin. Validation is always
on, so an existing service can fail the build after the upgrade, for example when a narrower route lies below a wider
one, or when a cut prefix routes a path that legacy didn't. Such a service was either exposing paths or routing
them differently than the annotations say.

Fix the `[ROUTE-MIGRATION]` errors in this order:

1. **Add `@ForbiddenRoute`** as the fix line of each `EXPOSURE` error says. This keeps the legacy behavior and
   documents it in the code. Add the route-registration-common version that contains `@ForbiddenRoute`.
2. **Set `<autoGenerateAuthorizationPolicies>true</autoGenerateAuthorizationPolicies>`** if you prefer not to
   annotate every case. The plugin then generates the DENY rules that keep the legacy behavior.
3. **Align the paths** for the errors that DENY rules can't fix (`CONFLICT`, `LEGACY_INVALID`,
   `LEGACY_PRECEDENCE_UNDEFINED`, and `EXPOSURE` of a gateway path with a partial-segment variable such as
   `/{name}.txt`): change the gateway paths or service paths so that routes cut to the same prefix share one rewrite,
   no two routes match the same request with different results, and every forbidden path uses whole-segment variables.
   Fix `LEGACY_INVALID` first: the conflicts of its routes are reported only after that.

Then check:

- `authorizationPolicyPorts`, if a gateway listener doesn't use `8080`;
- facade routes that relied on `@Gateway` paths, and paths called through the Service while a service-bound HTTPRoute
  exists;
- clients that expect `404` for forbidden paths.

Overlaps with routes of other services are not checked; review them separately.

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

### Build fails with `[ROUTE-MIGRATION]` errors

- Read the error blocks in the build log: each names the request, the legacy and Istio decisions, and the fixes.
- See [Route Validation](#route-validation) and [Migrating Existing Services](#migrating-existing-services).

### Expected gateway rewrite is missing

- Rewrite filters are only generated when `gatewayPath != servicePath`.
- Add `@Gateway` or `@GatewayRequestMapping` to provide gateway path.
- For facade routes, use `@FacadeGateway` or `@FacadeGatewayRequestMapping`.

### Superclass endpoints not discovered

- Scanner recursively processes superclasses, but those classes still must be
  available in compiled output and inside accepted packages.

### Helm template syntax issues

**Problem**: Generated YAML has invalid Helm templates

**Solution**: The plugin wraps output with:
```yaml
{{- if eq .Values.SERVICE_MESH_TYPE "Istio" }}
# ... routes and policies ...
{{- end }}
```

Ensure your Helm chart defines SERVICE_MESH_TYPE.


## Dependencies

The plugin uses:

- **ClassGraph**: For bytecode scanning and annotation discovery
- **Jackson**: For YAML serialization
- **Maven Plugin API**: For Maven integration

No runtime dependencies required in your application. `@ForbiddenRoute` is in route-registration-common, which
services already depend on for `@Route`.

## Related Resources
- [Kubernetes Gateway API](https://gateway-api.sigs.k8s.io/)
- [Istio AuthorizationPolicy](https://istio.io/latest/docs/reference/config/security/authorization-policy/)
- [Spring Web Annotations](https://docs.spring.io/spring-framework/docs/current/reference/html/web.html#mvc-ann-requestmapping)
- [Quarkus REST](https://quarkus.io/guides/rest-json)
- [Istio Gateway](https://istio.io/latest/docs/reference/config/networking/gateway/)
