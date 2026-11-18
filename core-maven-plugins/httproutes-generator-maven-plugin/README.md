# httproutes-generator-maven-plugin

Generates Helm-ready Gateway API `HTTPRoute` manifests for Istio from Quarkus/JAX-RS resources annotated with
`com.netcracker.cloud.routesregistration.common.annotation.Route`, `@Routes`, `@FacadeRoute`, `@Gateway` and
`@FacadeGateway`. Routes are collected with `routes-registrator-deployment`'s own
`RoutesRegistrationProcessor`, so the generated manifest matches exactly what the service registers at runtime.

**This artifact is Quarkus-only.** Spring services must use
`com.netcracker.cloud.plugins:httproutes-generator-spring-maven-plugin` instead; this plugin fails the build
when it detects a Spring `@RestController`/`@Controller` class among the scanned classes.

## Coordinates and configuration

```xml
<plugin>
  <groupId>com.netcracker.cloud.plugins</groupId>
  <artifactId>httproutes-generator-maven-plugin</artifactId>
  <version>1.2.0</version>
  <executions>
    <execution>
      <goals>
        <goal>generate-routes</goal>
      </goals>
    </execution>
  </executions>
  <configuration>
    <packages>
      <package>com.netcracker</package>
    </packages>
    <servicePort>8080</servicePort>
    <outputFile>gateway-httproutes.yaml</outputFile>
    <backendRefVal>{{ .Values.SERVICE_NAME }}</backendRefVal>
    <labels>
      <label>
        <key>team</key>
        <value>platform</value>
      </label>
    </labels>
  </configuration>
</plugin>
```

The plugin runs in aggregator mode, bound to `process-classes`. It must run after every scanned module has
compiled, so bind it in the last module of the reactor (or a dedicated aggregator module).

### Parameters

| Parameter       | Type           | Default                        | Description                                                                                                                                       |
|-----------------|----------------|---------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------|
| `packages`      | `String[]`     | `com.netcracker`                | Package prefixes scanned in each reactor module's compiled classes.                                                                                 |
| `servicePort`   | `int`          | `8080`                           | Backend service port used in every generated `backendRef`.                                                                                        |
| `outputFile`    | `String`       | `gateway-httproutes.yaml`        | Output path, resolved against the executing project's base directory. Parent directories are created as needed.                                  |
| `backendRefVal` | `String`       | `{{ .Values.SERVICE_NAME }}`     | **BREAKING**: changed from `{{ .Values.DEPLOYMENT_RESOURCE_NAME }}`. Services relying on the old default must set `<backendRefVal>` explicitly.   |
| `labels`        | `List<Label>`  | empty (defaults below apply)     | Custom labels for every generated `HTTPRoute`'s `metadata.labels`. When set, they fully replace the default label set.                            |

Each `<label>` entry has a `<key>` and `<value>` child, which allows label names containing `/`:

```xml
<labels>
  <label>
    <key>app.kubernetes.io/part-of</key>
    <value>my-app</value>
  </label>
</labels>
```

## Route collection matches `routes-registrator`

For each reactor module, the plugin builds a Jandex index containing:
- the module's own compiled classes;
- every jar or directory on the module's compile classpath that carries `META-INF/jandex.idx` (its index is
  read directly) or `META-INF/beans.xml` (its classes are indexed). This mirrors Quarkus' default
  dependency-indexing rule, and it includes `route-registration-common`, which ships `META-INF/beans.xml` — the
  annotation classes themselves must be indexed, since the processor reads `@Route` defaults through the index.

Candidate classes are exactly those `RoutesRegistrationProcessor.collectClassesWithRoutes` collects: classes
declaring `@Route`, `@Routes` or `@FacadeRoute` on the class, or on one of their own methods — not inherited
from a superclass or interface. For each candidate, the collected routes come from calling
`collectRoutesForClass(...).build()` directly, the same call the library makes at build time. This means:
- class-level `@Route` and `@Gateway`/`@FacadeGateway` are read only where declared on the class itself;
- class-level `@Path` is resolved on the class first, then its interfaces, then its super-interfaces, then its
  superclasses — an interface's `@Path` wins over a superclass's;
- method `@Path` is resolved on the method itself, or the same method in the class hierarchy.

**`@GatewayRequestMapping` has no effect in this plugin.** Only `routes-registrator-deployment` is called, and
it does not read that Spring-specific annotation — it is read by the Spring plugin's library instead. Keep this
plugin's version and `routes-registrator-deployment`'s version aligned (both come from this monorepo), so
collected routes never drift from what the service actually registers.

### Candidate selection: non-goals

- Routes from controllers that live in dependency jars are never collected; only reactor-module classes in
  `packages` are candidates. Dependency jars/directories are indexed only to resolve supertypes and annotations.
- `quarkus.index-dependency` configuration is not read. Only compile-classpath jars/directories with
  `META-INF/jandex.idx` or `META-INF/beans.xml` are indexed, per Quarkus' default rule.

## Spring guard

If any scanned class (a reactor-module class in `packages`) carries Spring's `@RestController` or
`@Controller`, the build fails with a message pointing to
`httproutes-generator-spring-maven-plugin`. Only classes this plugin actually scans are checked, so a Spring
jar pulled in transitively by a Quarkus service does not trigger a false failure.

A Quarkus service using the `quarkus-spring-web` compatibility layer (Spring `@RestController` annotations on
Quarkus resources) is also rejected by this guard. `routes-registrator` reads only `@Path`, so such classes
would produce a `/` root and fail the build anyway — the error message names both plugins so the owner can see
which one actually applies.

## Gateway and facade groups, and root reduction

Every collected route goes to exactly one group:
- **gateway group**: route type `PUBLIC`, `PRIVATE` or `INTERNAL`, including `@Route(gateways = ...)` naming one
  of the three border gateways (`public-gateway-service`, `private-gateway-service`, `internal-gateway-service`);
- **facade group**: route type `FACADE`, including `@FacadeRoute`, `@Route(gateways = <microservice-name>)` and
  `@Route(gateways = <any other name>)`.

Each group is reduced independently to a minimal set of prefix ("root") rules. A route whose path has no `{}`
or `*` segment becomes its own prefix rule with no rewrite. A route with a templated segment is cut before the
first such segment; everything before the cut becomes the match, and the corresponding suffix of the target path
becomes the rewrite.

Within a group, a rule is dropped when a shorter existing root's rewrite, applied to the rule's own match,
already produces the rule's target — the root's timeout becomes the maximum of both. A rule under a shorter root
that would rewrite to a *different* target stays as its own, more specific root.

## All-or-nothing facade rule

After reduction, the facade group is rendered **entirely or not at all**:
- if at least one facade root rewrites its prefix, or has a timeout greater than the 2-minute default
  (120000 ms), every facade root is rendered, each with its own rewrite and timeout;
- otherwise, no facade root is rendered, and no facade `HTTPRoute` is generated.

This exists because, in Istio ambient mode, once a service has a waypoint `HTTPRoute` (an `HTTPRoute` whose
parentRef is the service itself), every request to that service that matches no rule gets a 404. Rendering only
the facade roots that need a rewrite or a longer timeout would silently 404 every other facade route that was
left out, so the plugin either renders the whole group or none of it.

## Build failures

The build fails, naming the offending class or route, when:
- a collected route's path contains an unresolved `${...}` property placeholder (the plugin cannot know runtime
  property values);
- a route cannot be expressed as a `PathPrefix` match plus an optional `ReplacePrefixMatch` rewrite — the tail
  after the first templated segment must match between the "from" and "to" paths (ignoring variable names), and
  the rewrite target must not itself contain `{}` or `*`;
- two roots in the same group end up with the same match but different rewrite targets;
- a root's match reduces to `/` (a class-level `@Route` with no class-level `@Path` produces this).

## `@Route(hosts)` is ignored

`RouteEntry.hosts` is collected by the library but dropped by the renderer: no generated `HTTPRoute` gets
`spec.hostnames`, and no warning is logged. Hostnames apply to a whole `HTTPRoute`, not to an individual rule,
and the facade `HTTPRoute` is attached to a `Service`, not a `Gateway`.

## Gateway `HTTPRoute` and `HTTPROUTES_GENERATOR_GATEWAYS`

Gateway-group roots are rendered into a single `HTTPRoute` named `{{ .Values.SERVICE_NAME }}-java-annotations`.
Its `parentRefs` are resolved by Helm at deploy time from the optional chart value
`HTTPROUTES_GENERATOR_GATEWAYS`:

- when unset or empty, the gateways are `public-gateway`, `private-gateway` and `internal-gateway-service`, in
  that order;
- the value may be a YAML list:
  ```yaml
  HTTPROUTES_GENERATOR_GATEWAYS:
    - internet-gateway
    - internal-gateway-service
  ```
- or a comma-separated string:
  ```yaml
  HTTPROUTES_GENERATOR_GATEWAYS: "internet-gateway, public-gateway"
  ```
- names are trimmed, and blank entries are skipped;
- `internal-gateway-service` is rendered as a `Service` parentRef (`group: ""`); every other name is rendered
  as a `Gateway` parentRef (`group: gateway.networking.k8s.io`).

The per-type `HTTPRoute`s `-java-annotations-public`, `-private` and `-internal` from the legacy plugin are no
longer generated: every non-facade route shares the single `-java-annotations` `HTTPRoute`.

## Facade `HTTPRoute`

When rendered, the facade group becomes its own `HTTPRoute`, `{{ .Values.SERVICE_NAME }}-java-annotations-facade`,
with a single `parentRef`: `group: ""`, `kind: Service`, `name: {{ .Values.SERVICE_NAME }}`.
`HTTPROUTES_GENERATOR_GATEWAYS` has no effect on it.

## No regex matches

Every generated match is `PathPrefix`. The plugin never generates a `RegularExpression` or `Exact` match, and
never prints a "MANUAL REVIEW REQUIRED" banner — a path that cannot be expressed as a prefix-plus-rewrite fails
the build instead, naming the route.

## Access control on Istio

The legacy Cloud-Core service mesh let a route declare `@Route(type = PUBLIC | PRIVATE | INTERNAL)`:

- `PUBLIC` was reachable from the public, private and internal gateways;
- `PRIVATE` was reachable from the private and internal gateways;
- `INTERNAL` was reachable only from the internal gateway.

This was implemented by registering **three** routes per type per endpoint: the allowed route itself, plus
"forbidden" routes on the gateways that type should not reach, each returning a hardcoded 404
(`directResponse#statusCode: 404`). The same forbidden-route mechanism was also exposed directly through the
declarative route API via `allowed: false`, and a separate `deny: true` flag let a route override an
already-allowed route (e.g. one brought in by an out-of-the-box configuration) with another forced 404.

Istio's Gateway API has no equivalent of a route "type" or a per-gateway deny rule, and replicating the
three-routes-per-type model would mean generating a `RegularExpression` match for every templated path (to carry
the right `allowed`/`forbidden` distinction), which conflicts with sibling `PathPrefix` rules and cannot be
combined with `ReplacePrefixMatch` rewrites. The Istio migration strategy therefore drops the distinction
entirely and moves access control to a separate layer:

1. Every non-facade route is registered on every API gateway — public, private, internal, and any gateway added
   later (for example `internet-gateway`).
2. Every route is allowed on the private and internal gateways; there is no "internal-only" registration anymore.
3. Cloud-Core ships a deny-unless-permit `AuthorizationPolicy` for `public-gateway`: `action: ALLOW` with no
   `rules`, so by default nothing is reachable from the public gateway.
4. Each microservice ships its own hand-written `AuthorizationPolicy` for `public-gateway` that allows only its
   public subset of routes. The same mechanism can narrow routes an out-of-the-box policy would otherwise allow,
   replacing the legacy `deny: true` / `allowed: false` mechanism.
5. Facade routes stay outside this model entirely: they are attached directly to the service (not to a gateway),
   in their own `HTTPRoute`.

Access to the public gateway is therefore controlled by `AuthorizationPolicy` custom resources, not by which
gateways an `HTTPRoute` rule is attached to. This plugin only computes the minimal `PathPrefix` rules the
runtime library would register; it does not generate any `AuthorizationPolicy`. Until a service's
deny-unless-permit and allow policies are in place, every route reachable from the gateway `HTTPRoute` is
reachable from the public gateway.
