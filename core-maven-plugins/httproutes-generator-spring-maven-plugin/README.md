# httproutes-generator-spring-maven-plugin

Generates Helm-ready Gateway API `HTTPRoute` manifests for Istio from Spring controllers annotated with
`com.netcracker.cloud.routesregistration.common.annotation.Route`, `@Routes`, `@FacadeRoute`,
`@GatewayRequestMapping` and `@FacadeGatewayRequestMapping`. Routes are collected with
`route-registration-common-spring`'s own `RouteAnnotationProcessor`, so the generated manifest matches exactly
what the service registers at runtime.

## Coordinates and configuration

```xml
<plugin>
  <groupId>com.netcracker.cloud.plugins</groupId>
  <artifactId>httproutes-generator-spring-maven-plugin</artifactId>
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

## Route collection matches `route-registration-common-spring`

The plugin scans each reactor module's compiled classes (limited to `packages`) and loads them with a
classloader built from the module's own output directory plus its compile classpath, so superclasses,
interfaces and annotations resolve exactly as they would at runtime.

A class is a candidate when:
- it is concrete (not abstract, not an interface);
- it carries Spring's `@Component` directly or as a meta-annotation (for example `@RestController`), the same
  "get" semantics Spring component-scanning uses;
- `route-registration-common-spring` considers it annotated with `@Route`, `@Routes` or `@FacadeRoute`, on the
  class, its hierarchy, or any method in its hierarchy.

For each candidate, the collected routes come from calling `RouteAnnotationProcessor.scanForRoutes(Class)`
directly — the same method the library uses at application startup. This means:
- class-level annotations are resolved on superclasses and interfaces (`AnnotationUtils.findAnnotation`);
- methods inherited from superclasses are included (`ReflectionUtils.doWithMethods`);
- `@Routes` (repeated `@Route`) and `@FacadeRoute` are honored;
- `@GatewayRequestMapping` and `@FacadeGatewayRequestMapping` set the incoming ("from") path for gateway and
  facade routes respectively;
- `@Route(gateways = ...)` is resolved to PUBLIC/PRIVATE/INTERNAL for the three border gateway names
  (`public-gateway-service`, `private-gateway-service`, `internal-gateway-service`) and to FACADE for any other
  name.

**`@Gateway` has no effect in this plugin.** Only `route-registration-common-spring` is called, and it does not
read `@Gateway` — that annotation is read by the Quarkus plugin's library instead. Keep this plugin's version
and `route-registration-common-spring`'s version aligned (both come from this monorepo), so collected routes
never drift from what the service actually registers.

**One deliberate exception: the service's own name in `@Route(gateways = ...)`.** The library resolves a
route's incoming path differently depending on whether a named gateway equals the real microservice name
(resolved via `@FacadeGatewayRequestMapping`) or not (resolved via `@GatewayRequestMapping`). Since the plugin
runs at build time, it cannot know the real microservice name, so it always calls the library with a sentinel
that cannot match any real name — every explicitly named gateway other than the three border names is therefore
always resolved as a composite gateway, never as the library's legacy "own name" case. The route still lands in
the facade group either way (grouping is unaffected), but a service that still combines its own name with a
`@FacadeGatewayRequestMapping` override will get a different incoming path from the plugin than what it
registers at runtime. Switch such routes to a composite gateway name (not the service's own) to get matching
output.

### Candidate selection: non-goals

- Controllers defined in dependency jars are never scanned. Only reactor-module classes in `packages` are
  candidates; dependency jars are used only to resolve supertypes and annotations.
- Spring beans registered through `@Bean` factory methods, or excluded by component-scan filters, are not
  detected. Candidate selection approximates component scanning with a direct/meta `@Component` check; it does
  not evaluate `@ComponentScan` filters.

## Gateway and facade groups, and root reduction

Every collected route goes to exactly one group:
- **gateway group**: route type `PUBLIC`, `PRIVATE` or `INTERNAL`, including `@Route(gateways = ...)` naming one
  of the three border gateways;
- **facade group**: route type `FACADE`, including `@FacadeRoute`, `@Route(gateways = <microservice-name>)` and
  `@Route(gateways = <any other name>)`.

Each group is reduced independently to a minimal set of prefix ("root") rules. A route whose path has no `{}`
or `*` segment becomes its own prefix rule with no rewrite. A route with a templated segment is cut before the
first such segment; everything before the cut becomes the match, and the corresponding suffix of the target path
becomes the rewrite.

Within a group, a rule is dropped when a shorter existing root's rewrite, applied to the rule's own match,
already produces the rule's target — the root's timeout becomes the maximum of both. A rule under a shorter root
that would rewrite to a *different* target stays as its own, more specific root.

Example: the gateway group has `/api` → `/api` (no rewrite) and `/api/legacy` → `/v2/new`. Since
`/api`'s rewrite applied to `/api/legacy` would produce `/api/legacy`, not `/v2/new`, the second route is kept
as its own root. Both roots are rendered.

## All-or-nothing facade rule

After reduction, the facade group is rendered **entirely or not at all**:
- if at least one facade root rewrites its prefix, or has a timeout greater than the 2-minute default
  (120000 ms), every facade root is rendered, each with its own rewrite and timeout;
- otherwise, no facade root is rendered, and no facade `HTTPRoute` is generated.

This exists because, in Istio ambient mode, once a service has a waypoint `HTTPRoute` (i.e. an `HTTPRoute` whose
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
- a root's match reduces to `/` (a class-level `@Route` with no class-level path produces this).

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

## Migrating from `httproutes-generator-maven-plugin`

Spring services previously used `com.netcracker.cloud.plugins:httproutes-generator-maven-plugin`. That artifact
is now Quarkus-only and fails the build if it detects a Spring `@RestController`/`@Controller` class among the
scanned classes. Switch the plugin `artifactId`:

```diff
 <plugin>
   <groupId>com.netcracker.cloud.plugins</groupId>
-  <artifactId>httproutes-generator-maven-plugin</artifactId>
+  <artifactId>httproutes-generator-spring-maven-plugin</artifactId>
   <version>1.2.0</version>
   ...
 </plugin>
```

The shared parameters (`packages`, `servicePort`, `outputFile`, `backendRefVal`, `labels`) keep the same names
and meaning. After switching, regenerate and fix any build failures the new collection and reduction model
surfaces (`/` roots from a class with no path, placeholders, routes that cannot be expressed as a prefix
rewrite).
