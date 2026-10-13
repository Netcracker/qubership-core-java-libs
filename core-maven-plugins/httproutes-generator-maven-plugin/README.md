# HTTPRoute Generator Maven Plugin

This module contains `com.netcracker.cloud.plugins:httproutes-generator-maven-plugin`.
It scans compiled Java classes and generates Gateway API `HTTPRoute` manifests
and Istio `AuthorizationPolicy` manifests for Istio deployments.

## What It Does

- Scans Spring MVC and Quarkus/JAX-RS endpoints from compiled classes.
- Includes only classes/methods annotated with `@Route`, `@Routes`, `@FacadeRoute` or `@ForbiddenRoute`.
- Supports gateway path remapping via `@Gateway` and `@GatewayRequestMapping`, and facade gateway paths via
  `@FacadeGateway` and `@FacadeGatewayRequestMapping`.
- Generates only `PathPrefix` path matches (see [How Paths Are Matched](#how-paths-are-matched)).
- Groups generated routes by route type (`PUBLIC`, `PRIVATE`, `INTERNAL`, `FACADE`).
- Generates `AuthorizationPolicy` resources with `DENY` rules for paths that must not be reachable
  through a gateway (see [Forbidden Routes and AuthorizationPolicies](#forbidden-routes-and-authorizationpolicies)).
- Fails the build before writing anything when the generated routing can't keep the legacy behavior
  (see [Route Errors](#route-errors)).
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
        <backendRefVal>{{ .Values.SERVICE_NAME }}</backendRefVal>
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
| `backendRefVal`                     | `String`              | `{{ .Values.SERVICE_NAME }}`             | Backend service name in generated routes.                                               |
| `labels`                            | `List<Label>`         | empty list                               | Custom labels for the metadata of generated HTTPRoutes and AuthorizationPolicies. When set, they replace default labels. Each `<label>` entry has a `<key>` and `<value>` child element, which allows label names containing `/`. |
| `autoGenerateAuthorizationPolicies` | `boolean`             | `false`                                  | When `true`, the plugin generates the `DENY` rules that keep the legacy behavior, so you don't have to declare them with `@ForbiddenRoute`. See [Automatic DENY rules](#automatic-deny-rules). |

Example that enables automatic DENY rules:

```xml
<configuration>
  <packages>
    <package>com.example.service</package>
  </packages>
  <autoGenerateAuthorizationPolicies>true</autoGenerateAuthorizationPolicies>
</configuration>
```

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
  Several `@Route` annotations on one element (the `@Routes` container) are all read. As in legacy, `value` wins over
  `type` unless it is `INTERNAL`, and a `@Route` without a type is `INTERNAL`.
- `@FacadeRoute`: the same as `@Route(RouteType.FACADE)`, with its own `gateways`.
- `@ForbiddenRoute`: gateways on which the gateway path of the element must not be reachable
  (see [Forbidden Routes and AuthorizationPolicies](#forbidden-routes-and-authorizationpolicies)).

A method-level route list replaces the class-level list, as in the legacy route registration, so a method `@Route`
without a type is `INTERNAL` whatever the class `@Route` says. Each `@Route`/`@FacadeRoute` entry becomes these routes:

- no `gateways`, type `PUBLIC`/`PRIVATE`/`INTERNAL`: a route of that type, with the gateway path from
  `@Gateway`/`@GatewayRequestMapping`;
- no `gateways`, type `FACADE`: a **facade route**, with the gateway path from
  `@FacadeGateway`/`@FacadeGatewayRequestMapping`, or the service path when there is none;
- every name in `gateways`: `public-gateway-service`, `private-gateway-service` and `internal-gateway-service`
  give a route of type `PUBLIC`, `PRIVATE` and `INTERNAL`. Every other name is a composite gateway and gives a
  **composite route**, with the gateway path from `@Gateway`/`@GatewayRequestMapping`.

`hosts` are ignored: Istio has no composite gateways and no virtual hosts on a Service-bound HTTPRoute.

### Inheritance

Superclasses and interfaces are handled as in the legacy route registration libs:

- **Spring**: each annotation is looked up in the class or method first, then in its interfaces, then in its
  superclass, like Spring's `AnnotationUtils.findAnnotation`, and an annotation of the subclass replaces the same
  annotation of the parent. Only concrete classes are controllers: a controller maps all its methods, the inherited
  ones too, with its class annotations, inherited or its own. Abstract classes and interfaces give no routes of their
  own.
- **Quarkus**: only `@Path` and the HTTP method annotations (`@GET`, ...) are inherited. `@Route`, `@Gateway` and the
  other route annotations are read from the class or method itself. A class or interface with a route annotation of its
  own maps the methods it declares.

Superclasses and interfaces must be in the compiled output of the scanned module.

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

- match `PathPrefix /api/users`: only the class has `@Route`, so it gives the only route, and the methods are reached below it,
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
the longest matching gateway path won. The generated HTTPRoutes use only `PathPrefix` matches, which Istio evaluates
natively.

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
did. DENY rules keep the legacy behavior for them (see
[Forbidden Routes and AuthorizationPolicies](#forbidden-routes-and-authorizationpolicies)).

### Rewrite derivation

When the gateway path and the service path differ, the service path is cut at its first variable too, and the result
becomes the `ReplacePrefixMatch` rewrite. For example, `/api/v1/{id}/items` → `/items/{id}/items` gives match
`PathPrefix /api/v1` and rewrite `ReplacePrefixMatch /items`.

`ReplacePrefixMatch` keeps the part of the request after the match unchanged, so this is correct only when the part
after the cut is the same in both paths (variables are compared by position). This holds for the routes services
declare in practice, and **the plugin doesn't check it**.

### Merging

Routes cut to the same match are grouped together, whatever their route types, because Istio can't tell apart two
`PathPrefix` rules with the same value, even in different HTTPRoutes. They become one rule:

- with the rewrite of the first route (by gateway path, then service path). The plugin doesn't compare the rewrites:
  routes cut to one match share one rewrite in practice;
- with the largest timeout, where a route without a timeout counts as the 2-minute gateway default (see
  [Timeout Generation](#timeout-generation)). A warning is logged when the timeouts differ.

A merged rule goes into the HTTPRoute of the widest route type, because `PUBLIC` routes are exposed on every gateway
that `PRIVATE` and `INTERNAL` routes are exposed on. On the wider gateways, the narrower routes of the group must be
forbidden by DENY rules.

## Route Errors

Before it writes any file, the plugin logs every problem it finds. When there is at least one error, the build fails
with `<n> route migration errors, see log`, and the output file is left unchanged.

Errors:

- A path that legacy didn't route on a gateway, and Istio would, has no DENY rule
  (see [Forbidden Routes and AuthorizationPolicies](#forbidden-routes-and-authorizationpolicies)):

  ```
  [ERROR] /api/v1/my-service/resource/{var1}/internal-api is forbidden by legacy, as its route type is narrower, but Istio routes it by PathPrefix /api/v1/my-service/resource on public-gateway, private-gateway: add @ForbiddenRoute({PUBLIC, PRIVATE}) to the element mapped to /api/v1/my-service/resource/{var1}/internal-api
  [ERROR] /api/v1/my-service/order is not routed by legacy, but Istio routes it by PathPrefix /api/v1/my-service/order cut from /api/v1/my-service/order/{var1}/items on public-gateway, private-gateway: add @ForbiddenRoute({PUBLIC, PRIVATE}) to the element mapped to /api/v1/my-service/order
  ```

- A DENY rule can't be expressed, because a forbidden path or a route that overlaps it has a variable that takes up
  only part of a segment:

  ```
  [ERROR] /api/v1/my-service/files/{name}.txt is a forbidden path or overlaps one, and an AuthorizationPolicy can't express it: variables must take up whole path segments
  ```

- `@ForbiddenRoute` forbids the gateway path of a route on a gateway where the route is exposed.
- `@ForbiddenRoute` lists no gateway, or lists `INTERNAL` or `FACADE`.

Warnings:

- Routes with different timeouts were merged into one rule with the largest timeout.
- Facade or composite routes have timeouts, and no service-bound HTTPRoute is generated
  (see [Facade and Composite Routes](#facade-and-composite-routes)).

Not checked:

- Overlaps with routes of **other services**. The plugin sends every rule to the single backend
  (`backendRefVal`:`servicePort`) and doesn't know the routes of other services on the same gateways.
- Whether the part after the cut is the same in the gateway path and the service path
  (see [Rewrite derivation](#rewrite-derivation)).
- Whether the routes cut to one match have the same rewrite (see [Merging](#merging)).

## Forbidden Routes and AuthorizationPolicies

### `@ForbiddenRoute`

`@ForbiddenRoute` (in `com.netcracker.cloud.routesregistration.common.annotation`, route-registration-common) marks
the gateway path of a class or method as forbidden on the listed gateways: `PUBLIC` and/or `PRIVATE`. The internal
gateway needs no DENY rules.
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
@ForbiddenRoute({RouteType.PUBLIC, RouteType.PRIVATE})
public class OrderController {

    @GetMapping("/{var1}/items")
    @GatewayRequestMapping("/{var1}/items")
    @Route(RouteType.PUBLIC)
    public void items() {
    }
}
```

`@ForbiddenRoute` must not forbid a route's own gateway path on a gateway where that route is allowed.

### AuthorizationPolicy output

For each gateway with at least one DENY rule, the plugin generates one `AuthorizationPolicy` named
`{{ .Values.SERVICE_NAME }}-java-annotations-deny-<public|private>`, with `action: DENY`, bound to Gateway
`public-gateway` or `private-gateway`. No policy is generated
when there are no DENY rules. Each forbidden path `F` gives one rule:

- `paths`: `F` and everything below it, with each variable replaced by `{*}`;
- `notPaths`: every longer route allowed on that gateway that matches some of the same requests as `F`, and
  everything below it, so these routes stay reachable. The paths are compared segment by segment, and a variable
  segment in one of them matches a literal segment in the other: for `F` = `/a/lit/x`, the `PUBLIC` route
  `/a/{id}/x/y` gives the `notPaths` `/a/{*}/x/y` and `/a/{*}/x/y/{**}`. Every route allowed on that gateway that
  legacy routes some of these requests by is excluded too, even if it is shorter: for `F` = `/api/{version}`, the
  `PUBLIC` route `/api/v1/x/y` gives the `notPaths` `/api/v1/x/y` and `/api/v1/x/y/{**}`. An explicit `@ForbiddenRoute`
  path is denied even where legacy routed it by a shorter exposed route: for `F` = `/api/v1/svc/admin`, the `PUBLIC`
  route `/api/v1/svc` gives no `notPaths`, so `/api/v1/svc/admin` returns 403, and the rest of `/api/v1/svc` stays
  routed;
- `ports`: `8080`, the listener port of the public and private gateways.

For the controllers above, the generated file has the `PUBLIC` HTTPRoute with the rules
`PathPrefix /api/v1/my-service/resource` → `/resource` and `PathPrefix /api/v1/my-service/order` → `/order`, followed
by two policies. The public one is:

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

`deny-private` has the same rules, bound to `private-gateway`.

### Which paths need DENY rules

Legacy registered every route on all border gateways, and forbade it on the gateways wider than its type. On the
public and private gateways, a path needs a DENY rule when Istio routes it and legacy didn't:

- the gateway path of a route of a narrower type that lies below the cut match of a route exposed on the gateway, such
  as `internalApi` above, below `PathPrefix /api/v1/my-service/resource`. A narrower route that legacy routed by a
  longer exposed route needs no rule: `/api/v1/svc/orders` of type `INTERNAL` next to the `PUBLIC` route
  `/api/v1/svc/{tenantIdentifier}`;
- the cut match `P` of an exposed route with a variable, when no route exposed on the gateway matches `P` itself in
  legacy, such as `/api/v1/my-service/order` above.

By default, each of these paths must be declared with `@ForbiddenRoute`, or the build fails
(see [Route Errors](#route-errors)).

### Automatic DENY rules

With `autoGenerateAuthorizationPolicies` set to `true`, the plugin generates the DENY rules for these paths itself.
For the example above without any `@ForbiddenRoute`, it generates the same policies. `@ForbiddenRoute` still adds rules
for any other paths it lists.

The default is `false`, because DENY rules on the shared public and private gateways are a security-relevant change
that service owners should opt into explicitly.

### DENY rules on shared gateways

The public and private gateways are shared by all services. A DENY rule on a gateway also applies to paths of other
services under the same prefix. This is safe as long as gateway paths are namespaced per service
(`/api/<version>/<service>/...`).

## Facade and Composite Routes

Istio has no facade or composite gateways. Clients call the Service `{{ .Values.SERVICE_NAME }}` directly, and
Istio sends every request for it to the service unchanged, unless an HTTPRoute is bound to the Service. So the
facade and composite routes (see [Route metadata](#route-metadata-both-frameworks)) are planned together, with the
same cut and merging as a gateway, but no AuthorizationPolicies are generated for them.

- If **no** facade or composite rule has a rewrite, no service-bound HTTPRoute is generated. If any of these routes has
  a timeout, the timeout is not applied, and a warning says so:

  ```
  [WARNING] No facade or composite route has a rewrite, so no service-bound HTTPRoute is generated, and the timeouts of the facade and composite routes are not applied
  ```

- If **any** of them has a rewrite, one HTTPRoute `{{ .Values.SERVICE_NAME }}-java-annotations-facade` is generated,
  bound only to Service `{{ .Values.SERVICE_NAME }}`, and it lists **all** facade and composite rules, including those
  without a rewrite. While it exists, Istio returns **404 for every other path** through the Service. To fix that,
  declare those endpoints as facade routes, or call them through a border gateway.

Legacy facade and composite gateways were separate gateways, so two such routes with the same gateway path and
different service paths were valid there. In the service-bound HTTPRoute they are merged into one rule with the
rewrite of the first one, and this isn't checked.

Example: a method with `@FacadeRoute` and `@FacadeGatewayRequestMapping("/facade/items")` on `/items`, a method with
`@Route(RouteType.FACADE)` on `/orders`, and a method on `/shared` with `@GatewayRequestMapping("/api/v1/svc/shared")`,
`@Route(RouteType.PUBLIC)` and `@Route(gateways = "composite-gw", hosts = "example.com")` give the `PUBLIC` HTTPRoute
with the rule `PathPrefix /api/v1/svc/shared` → `/shared`, and the service-bound HTTPRoute
`{{ .Values.SERVICE_NAME }}-java-annotations-facade` with the parentRef Service `{{ .Values.SERVICE_NAME }}` and these
rules:

| Match                           | Rewrite                        |
|---------------------------------|--------------------------------|
| `PathPrefix /api/v1/svc/shared` | `ReplacePrefixMatch /shared`   |
| `PathPrefix /facade/items`      | `ReplacePrefixMatch /items`    |
| `PathPrefix /orders`            | none (`filters: []`)           |

## Generated YAML Shape

The plugin writes:

1. Header comment (`DO NOT EDIT`),
2. Istio conditional guard:
   `{{- if eq .Values.SERVICE_MESH_TYPE "Istio" }}`,
3. `HTTPRoute` resources grouped by route type, in the order public, private, internal, facade,
4. `AuthorizationPolicy` resources, in the order public, private.

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
      name: "{{ .Values.SERVICE_NAME }}"
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
- When routes with different timeouts are merged into one rule (see [Merging](#merging)), the largest timeout is
  used, where a route without a timeout counts as the 2-minute gateway default. When the default wins, the
  `timeouts` block is omitted:

  | Merged timeouts | Rule timeout          |
  |-----------------|-----------------------|
  | unset + `5s`    | none (the 2m default) |
  | unset + `2m`    | `2m`                  |
  | unset + `5m`    | `5m`                  |
  | `5s` + `10s`    | `10s`                 |

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
      name: "{{ .Values.SERVICE_NAME }}"
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

Istio picks the longest `PathPrefix`, so the order doesn't change routing; it makes the generated YAML stable between
builds. Each match value appears in only one HTTPRoute (see [Merging](#merging)).

## Differences from Legacy Routing

- **403 instead of 404.** A request denied by an `AuthorizationPolicy` returns `403 Forbidden` with the body
  `RBAC: access denied`, where the legacy mesh returned `404 Not Found`.
- **Path normalization.** DENY rules match request paths, so `%2F`, `..` and duplicate slashes can bypass them unless
  `meshConfig.pathNormalization.normalization` is at least `MERGE_SLASHES`
  (see [Istio path normalization](https://istio.io/latest/docs/ops/best-practices/security/#understand-path-normalization)).
- **Port 8080.** DENY rules apply only to port `8080` of the public and private gateways.
- **Shared gateways.** DENY rules also apply to other services' paths under the same prefix
  (see [DENY rules on shared gateways](#deny-rules-on-shared-gateways)).
- **Facade gateway paths.** Facade routes take their gateway path from `@FacadeGateway`/`@FacadeGatewayRequestMapping`,
  or use the service path, as in legacy. Earlier plugin versions used `@Gateway`/`@GatewayRequestMapping` paths for them.
- **Service-bound HTTPRoute.** While it exists, other paths through the Service return 404
  (see [Facade and Composite Routes](#facade-and-composite-routes)).
- **Internal gateway.** No DENY rules. Paths that legacy forbade there (404), and subtrees widened by the cut, are
  routed to the service.

## Migrating Existing Services

An existing service can fail the build after the upgrade, for example when a narrower route lies below a wider one, or
when a cut prefix routes a path that legacy didn't. Fix the errors in this order:

1. **Add `@ForbiddenRoute`** where the error says. This keeps the legacy behavior and documents it in the code. Add
   the route-registration-common version that contains `@ForbiddenRoute`.
2. **Or set `<autoGenerateAuthorizationPolicies>true</autoGenerateAuthorizationPolicies>`** to generate these DENY
   rules instead.
3. **Align the paths** for the errors that DENY rules can't fix: forbidden paths with partial-segment variables such
   as `/{name}.txt`.

Then check facade routes that relied on `@Gateway` paths, paths called through the Service while a service-bound
HTTPRoute exists, and clients that expect `404` for forbidden paths. Overlaps with routes of other services are not
checked; review them separately.

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

### Build fails with route migration errors

- Each error in the build log names the paths and gateways involved and the fix.
- See [Route Errors](#route-errors) and [Migrating Existing Services](#migrating-existing-services).

### Expected gateway rewrite is missing

- Rewrite filters are only generated when `gatewayPath != servicePath`.
- Add `@Gateway` or `@GatewayRequestMapping` to provide gateway path.
- For facade routes, use `@FacadeGateway` or `@FacadeGatewayRequestMapping`.

### Superclass endpoints not discovered

- See [Inheritance](#inheritance): a Spring base class must have a concrete subclass, and a Quarkus resource doesn't
  inherit `@Route`.
- Superclasses and interfaces must be available in the compiled output of the scanned module.

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
