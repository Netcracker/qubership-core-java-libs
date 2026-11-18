# Spec Delta

## Purpose

Generate Helm-ready Gateway API HTTPRoute manifests for Istio from annotated Spring and Quarkus controllers. Routes are collected exactly as the runtime route-registration libraries collect them. They are then reduced to a minimal set of prefix ("root") rules: one HTTPRoute for all API gateways and one separate facade HTTPRoute. Access restrictions are left to AuthorizationPolicy CRs.

## ADDED Requirements

### Requirement: Framework-specific plugins
Two Maven plugins SHALL be provided, both with the goal `generate-routes`, aggregator mode, default phase `process-classes`, and the same parameters (`packages`, `servicePort`, `outputFile`, `backendRefVal`, `labels`):
- `com.netcracker.cloud.plugins:httproutes-generator-maven-plugin` for Quarkus;
- `com.netcracker.cloud.plugins:httproutes-generator-spring-maven-plugin` for Spring.

Both SHALL produce output in the same format for equivalent routes. Across every reactor module, each plugin SHALL track framework markers only among scanned classes (reactor-module classes in the configured `packages`):
- **Spring markers:** class-level `@RestController` or `@Controller`;
- **Quarkus markers:** `jakarta.ws.rs.Path` or `javax.ws.rs.Path` on the class or any of its methods.

After scanning the full reactor, the Quarkus plugin SHALL fail the build when Spring markers are present and Quarkus markers are not, logging and surfacing: `Quarkus was not found. Please use httproutes-generator-spring-maven-plugin for Spring service.` The Spring plugin SHALL fail the build when Quarkus markers are present and Spring markers are not, logging and surfacing: `Spring was not found. Please use httproutes-generator-maven-plugin for Quarkus service.` When both marker kinds appear anywhere in the reactor, or only the plugin's own markers appear, the build SHALL continue. When neither appears, the build SHALL also continue (empty output is allowed).

#### Scenario: Quarkus plugin on a Spring-only reactor
- **WHEN** `httproutes-generator-maven-plugin` runs on a reactor whose scanned classes carry Spring markers and no Quarkus `@Path` markers
- **THEN** the build fails with `Quarkus was not found. Please use httproutes-generator-spring-maven-plugin for Spring service.`

#### Scenario: Spring plugin on a Quarkus-only reactor
- **WHEN** `httproutes-generator-spring-maven-plugin` runs on a reactor whose scanned classes carry Quarkus `@Path` markers and no Spring controller markers
- **THEN** the build fails with `Spring was not found. Please use httproutes-generator-maven-plugin for Quarkus service.`

#### Scenario: Mixed framework markers in one reactor
- **WHEN** either plugin runs on a reactor where scanned classes include both Spring controller markers and Quarkus `@Path` markers (for example a `quarkus-spring-web` module)
- **THEN** the build does not fail for framework mismatch

#### Scenario: Same goal and parameters
- **WHEN** a pom configures either plugin with `<goal>generate-routes</goal>` and any of `packages`, `servicePort`, `outputFile`, `backendRefVal`, `labels`
- **THEN** the configuration is accepted, and each parameter has the same meaning in both plugins

### Requirement: Spring route collection matches route-registration-common-spring
The Spring plugin SHALL scan the compiled classes of every reactor module, limited to the configured `packages`. Superclasses, interfaces and annotations SHALL be resolved against the module's compile classpath. A class SHALL be a candidate when all of the following hold:
- it is concrete (not abstract, not an interface);
- it carries `@Component` directly or as a meta-annotation (for example `@RestController`), as Spring component scanning would detect it;
- `route-registration-common-spring` considers it annotated with `@Route`, `@Routes` or `@FacadeRoute`, on the class, its hierarchy, or any method in its hierarchy.

For each candidate, the collected routes (from path, to path, route type, timeout, gateway) SHALL be exactly those that `route-registration-common-spring`'s `RouteAnnotationProcessor` produces for that class at runtime. This includes:
- superclass and interface lookup of class-level annotations;
- methods inherited from superclasses;
- `@Routes` and `@FacadeRoute`;
- `@GatewayRequestMapping` and `@FacadeGatewayRequestMapping`;
- `@Route(gateways)`.

`@Gateway` SHALL have no effect in the Spring plugin, because the library does not read it.

The plugin calls the library with a build-time sentinel in place of the real microservice name (`${cloud.microservice.name}`), since that value is only known at runtime. `@Route(gateways)` naming the microservice's own name is therefore always resolved as a composite gateway (its "from" path comes from `@GatewayRequestMapping`, or the route's own path if that is absent), never as the library's legacy "own name" case (whose "from" path would instead come from `@FacadeGatewayRequestMapping`). A route still lands in the facade group either way, but the "from" path can differ for a service that combines its own name with a facade-specific mapping.

#### Scenario: Concrete subclass inherits annotations
- **WHEN** an abstract class has `@RequestMapping("/api/v1/base")` and a method `@PostMapping("/items")` with `@Route`, and a concrete subclass is annotated only with `@RestController`
- **THEN** the subclass contributes the route `/api/v1/base/items`, and the abstract class contributes nothing by itself

#### Scenario: Subclass overrides the class-level path
- **WHEN** a `@RestController` base class has `@RequestMapping("/old")` and `@Route`, and a `@RestController` subclass declares `@RequestMapping("/new")`
- **THEN** the subclass's routes use `/new`, and the base class's routes use `/old`, exactly as the library produces them for each bean class

#### Scenario: @Gateway ignored for Spring
- **WHEN** a Spring controller has `@RequestMapping("/svc")`, `@Gateway("/gw")` and `@Route`
- **THEN** its route matches `/svc` with no rewrite

#### Scenario: Repeated @Route
- **WHEN** a Spring controller method is annotated with both `@Route(RouteType.PUBLIC)` and `@Route(RouteType.FACADE)`
- **THEN** the method yields one gateway-group route and one facade-group route

### Requirement: Quarkus route collection matches routes-registrator
For each reactor module, the Quarkus plugin SHALL build a Jandex index containing:
- the module's compiled classes;
- the jars and directories on the module's compile classpath that contain `META-INF/jandex.idx` (whose index is loaded) or `META-INF/beans.xml` (which are indexed). This follows Quarkus' default dependency-indexing rule, and it includes `route-registration-common`, which ships `META-INF/beans.xml`.

Candidate classes SHALL be those that `routes-registrator-deployment`'s `RoutesRegistrationProcessor` collects: classes declaring `@Route`, `@Routes` or `@FacadeRoute` on the class or on one of their own methods. Candidates SHALL be limited to reactor-module classes in the configured `packages`. For each candidate, the collected routes SHALL be exactly those the processor builds for that class. This includes:
- class-level `@Route` and `@Gateway`/`@FacadeGateway` read only where declared;
- class-level `@Path` resolved on the class first, then interfaces, super-interfaces and superclasses;
- method `@Path` resolved on the method or the same method in the hierarchy.

`@GatewayRequestMapping` SHALL have no effect in the Quarkus plugin, because the library does not read it.

The plugin calls the library with a build-time sentinel in place of the real microservice name, for the same reason and with the same consequence as the Spring plugin (see the Spring requirement above): `@Route(gateways)` naming the microservice's own name is always resolved as a composite gateway, using `@Gateway`/the route's own path rather than `@FacadeGateway`.

#### Scenario: Interface path wins over superclass path
- **WHEN** `Case1 extends Case3 implements ICase1`, where `Case3` has `@Path("/x-case3")`, `ICase1` has `@Path("/x-icase1")`, and `Case1` declares `@Route` and no `@Path` of its own
- **THEN** `Case1`'s class-level routes use `/x-icase1`

#### Scenario: Undeclared subclass is not a candidate
- **WHEN** a resource class with `@Path("/base")` declares `@Route` on a method, and a subclass declares no `@Route` anywhere
- **THEN** only the base class contributes routes, and the subclass contributes none

#### Scenario: @GatewayRequestMapping ignored for Quarkus
- **WHEN** a Quarkus resource has `@Path("/svc")`, `@GatewayRequestMapping("/gw")` and `@Route`
- **THEN** its route matches `/svc` with no rewrite

### Requirement: Unresolvable placeholders fail the build
The build SHALL fail, naming the class and the placeholder, when a route path contains a `${...}` property placeholder. The plugins cannot know runtime property values.

#### Scenario: Spring placeholder in class path
- **WHEN** a Spring controller with `@Route` has `@RequestMapping("${api.prefix}/users")`
- **THEN** the build fails with an error naming the class and `${api.prefix}`

### Requirement: Route groups
Every collected route SHALL go to exactly one group:
- the **gateway group**, when its route type is `PUBLIC`, `PRIVATE` or `INTERNAL`. This includes routes whose `@Route(gateways)` names `public-gateway-service`, `private-gateway-service` or `internal-gateway-service`, which the libraries map to those types;
- the **facade group**, when its route type is `FACADE`. This includes `@Route(RouteType.FACADE)`, `@FacadeRoute`, and `@Route(gateways)` naming any other gateway (the microservice's own name or a composite gateway).

The gateway name of a facade-group route SHALL NOT affect the output. Route `hosts` SHALL be ignored: no HTTPRoute gets `spec.hostnames`, and no warning is logged.

#### Scenario: Border gateway name is not facade
- **WHEN** a route is annotated `@Route(gateways = "private-gateway-service")`
- **THEN** it is in the gateway group

#### Scenario: Composite gateway name is facade
- **WHEN** a route is annotated `@Route(gateways = "my-composite-gateway")`
- **THEN** it is in the facade group

#### Scenario: Hosts ignored
- **WHEN** a route is annotated `@Route(gateways = "my-composite-gateway", hosts = "example.com")`
- **THEN** it is in the facade group, and the generated file contains no `hostnames` and no `example.com`

### Requirement: Prefix rules without regex
Each collected route SHALL be turned into a prefix rule as follows:
1. Normalize both the from path and the to path: add a leading `/`, collapse repeated `/`, and remove a trailing `/`.
2. Cut the from path before its first segment containing `{` or `*`. The cut-off part is the tail. The match is the remaining prefix.
3. The to path SHALL end with the same tail, with variable names ignored. The rewrite target is the to path without that tail.
4. If the tails differ, or if the rewrite target itself contains `{` or `*`, the build SHALL fail with an error naming the route.

A rule has a `URLRewrite` filter of type `ReplacePrefixMatch` only when its match differs from its rewrite target. Every generated match SHALL be `PathPrefix`. The plugins SHALL NOT generate `RegularExpression` or `Exact` matches, and SHALL NOT add any manual-review banner.

#### Scenario: Templated route without rewrite
- **WHEN** a route goes from `/api/v1/users/{id}` to `/api/v1/users/{id}`
- **THEN** its rule matches `PathPrefix` `/api/v1/users` with no filter

#### Scenario: Templated route with rewrite
- **WHEN** a route goes from `/gw/{tenant}/items` to `/svc/{id}/items`
- **THEN** its rule matches `/gw` and rewrites to `/svc`

#### Scenario: Inexpressible rewrite
- **WHEN** a route goes from `/gw/{id}/a` to `/svc/{id}/b`
- **THEN** the build fails with an error naming the route

#### Scenario: No regex in output
- **WHEN** collected routes contain path templates
- **THEN** the generated file contains no `RegularExpression` match type and no `MANUAL REVIEW REQUIRED` text

### Requirement: Root reduction
Within each group separately, prefix rules SHALL be reduced to roots:
1. Rules SHALL be processed from fewest to most path segments in the match.
2. A rule SHALL be absorbed by the existing root with the longest match that is a segment-wise prefix of the rule's match, if that root's rewrite applied to the rule's match yields the rule's rewrite target. The root's timeout becomes the maximum of both.
3. A rule that no root absorbs SHALL become a root itself. This includes a rule under a shorter root whose rewrite would produce a different target; it then stays as its own, more specific rule.
4. Two roots with the same match and different rewrite targets SHALL fail the build with an error naming the match.
5. A root whose match is `/` SHALL fail the build with an error naming the class or classes that produced it.

A root's timeout SHALL be the maximum positive timeout among the rules it absorbed, including itself. Timeouts SHALL be rendered as `timeouts.request`, formatted as `<N>h` when divisible by 3600000 ms, otherwise `<N>m` when divisible by 60000, otherwise `<N>s` when divisible by 1000, otherwise `<N>ms`. Without a positive timeout, the `timeouts` block SHALL be omitted.

#### Scenario: Endpoints absorbed by the controller root
- **WHEN** the gateway group has `/api/v1/test1` (no timeout), `/api/v1/test1/test1` (250000 ms) and `/api/v1/test1/test1/test2` (150000 ms), all without rewrite
- **THEN** a single root `/api/v1/test1` remains, with `timeouts.request: 250s`

#### Scenario: Consistent rewrite absorbed
- **WHEN** the gateway group has `/class/path/from` → `/class/path/to` and `/class/path/from/method/path/to2` → `/class/path/to/method/path/to2`
- **THEN** a single root `/class/path/from` → `/class/path/to` remains

#### Scenario: Different nested rewrite kept
- **WHEN** the gateway group has `/api` → `/api` and `/api/legacy` → `/v2/new`
- **THEN** both remain as roots: `/api` with no filter, and `/api/legacy` rewritten to `/v2/new`

#### Scenario: Endpoints without a controller root
- **WHEN** a controller with no class-level path has method routes `/test1/{id}`, `/test1/test2` and `/test2`, all without rewrite
- **THEN** the roots are `/test1` and `/test2`

#### Scenario: Conflicting targets
- **WHEN** the gateway group has `/gw` → `/svc-a` and `/gw` → `/svc-b`
- **THEN** the build fails with an error naming `/gw`

#### Scenario: Root slash fails
- **WHEN** a class-level `@Route` without any class-level path produces a gateway-group route from `/`
- **THEN** the build fails with an error naming that class

#### Scenario: Groups reduced independently
- **WHEN** the gateway group has `/gw` → `/gw` and the facade group has `/gw` → `/svc`
- **THEN** the build succeeds, and each group keeps its own `/gw` root

### Requirement: Facade group rendered all-or-nothing
After reduction, the facade group SHALL be rendered as a whole or not at all:
- if at least one facade root has a match that differs from its rewrite target, or a timeout greater than 120000 ms (the 2-minute default), then **every** facade root SHALL be rendered, each with its own rewrite and timeout;
- otherwise no facade root SHALL be rendered.

Reason: in Istio ambient mode, once an HTTPRoute is attached to a service's waypoint, every request to that service that matches no rule gets a 404. Rendering only some facade roots would break the facade routes that were left out.

#### Scenario: Facade root with rewrite
- **WHEN** the only facade root matches `/test/sleep` and rewrites to `/sleep`
- **THEN** it is rendered in the facade HTTPRoute

#### Scenario: Facade root with long timeout
- **WHEN** the only facade root `/api/v1/reports` has no rewrite and a timeout of 300000 ms
- **THEN** it is rendered in the facade HTTPRoute with `timeouts.request: 5m`

#### Scenario: One facade root that needs it renders the whole group
- **WHEN** the facade roots are `/test/sleep` → `/sleep` and `/api/v1/plain` with no rewrite and a timeout of 10000 ms
- **THEN** both are rendered in the facade HTTPRoute: `/test/sleep` rewritten to `/sleep`, and `/api/v1/plain` with no filter and `timeouts.request: 10s`

#### Scenario: Facade group with nothing to change
- **WHEN** every facade root has no rewrite and a timeout of at most 120000 ms, for example the only facade root `/class/path/to/method/path/to` at 10000 ms
- **THEN** no facade root is rendered, and no facade HTTPRoute is generated

#### Scenario: Timeout exactly two minutes
- **WHEN** the only facade root has no rewrite and a timeout of exactly 120000 ms
- **THEN** no facade HTTPRoute is generated

### Requirement: Gateway HTTPRoute
The plugins SHALL render the gateway-group roots into one HTTPRoute (`apiVersion: gateway.networking.k8s.io/v1`) named `{{ .Values.SERVICE_NAME }}-java-annotations`.

Its `parentRefs` SHALL be resolved by Helm at deploy time from the optional value `HTTPROUTES_GENERATOR_GATEWAYS`, which may be a YAML list of gateway names or a comma-separated string. Names SHALL be trimmed, and blank entries skipped. When the value is unset or empty, the gateways SHALL be `public-gateway`, `private-gateway`, `internal-gateway-service`, in that order. The name `internal-gateway-service` SHALL be rendered as `group: ""`, `kind: Service`. Every other name SHALL be rendered as `group: gateway.networking.k8s.io`, `kind: Gateway`.

Every rule SHALL have one backendRef: `group: ""`, `kind: Service`, `name` = the `backendRefVal` parameter, `port` = `servicePort`, `weight: 1`. The `backendRefVal` default SHALL be `{{ .Values.SERVICE_NAME }}`. Rules SHALL be ordered by match path in lexical order. The HTTPRoute SHALL be omitted when it has no rules.

#### Scenario: Default backendRef
- **WHEN** the plugin runs without a `backendRefVal` parameter
- **THEN** every rule in every generated HTTPRoute has a backendRef with `name: {{ .Values.SERVICE_NAME }}`

#### Scenario: Custom backendRef
- **WHEN** `backendRefVal` is configured as `{{ .Values.DEPLOYMENT_RESOURCE_NAME }}`
- **THEN** every backendRef in every generated HTTPRoute has `name: {{ .Values.DEPLOYMENT_RESOURCE_NAME }}`

#### Scenario: Gateways value not set
- **WHEN** the generated chart is rendered without `HTTPROUTES_GENERATOR_GATEWAYS` and at least one gateway rule exists
- **THEN** the HTTPRoute has parentRefs Gateway `public-gateway`, Gateway `private-gateway`, and Service `internal-gateway-service`, in that order

#### Scenario: Gateways value empty
- **WHEN** the generated chart is rendered with `HTTPROUTES_GENERATOR_GATEWAYS: ""` or an empty list
- **THEN** the parentRefs are the same as when the value is not set

#### Scenario: Gateways value as list
- **WHEN** the generated chart is rendered with `HTTPROUTES_GENERATOR_GATEWAYS: [internet-gateway, internal-gateway-service]`
- **THEN** the parentRefs are Gateway `internet-gateway` (group `gateway.networking.k8s.io`) and Service `internal-gateway-service` (group `""`)

#### Scenario: Gateways value as comma-separated string
- **WHEN** the generated chart is rendered with `HTTPROUTES_GENERATOR_GATEWAYS: "internet-gateway, public-gateway"`
- **THEN** the parentRefs are Gateway `internet-gateway` and Gateway `public-gateway`

#### Scenario: Route types share one HTTPRoute
- **WHEN** one controller has `@Route(RouteType.INTERNAL)` and another has `@Route(RouteType.PUBLIC)`
- **THEN** both controllers' roots appear in the `-java-annotations` HTTPRoute, and no `-java-annotations-public`, `-private` or `-internal` HTTPRoute is generated

#### Scenario: Only facade routes
- **WHEN** every collected route is in the facade group
- **THEN** no `-java-annotations` gateway HTTPRoute is generated

### Requirement: Facade HTTPRoute
When the "all-or-nothing" rule renders the facade group, the plugins SHALL render all its roots into a separate HTTPRoute named `{{ .Values.SERVICE_NAME }}-java-annotations-facade`. It SHALL have a single parentRef: `group: ""`, `kind: Service`, `name: {{ .Values.SERVICE_NAME }}`. The `HTTPROUTES_GENERATOR_GATEWAYS` value SHALL NOT affect it. Each rule SHALL have the same backendRef as gateway rules, and rules SHALL be ordered by match path in lexical order. Facade routes SHALL never appear in the gateway HTTPRoute. The facade HTTPRoute SHALL be omitted when it has no rules.

#### Scenario: Facade parentRef
- **WHEN** at least one facade rule is rendered, and the chart is rendered with `HTTPROUTES_GENERATOR_GATEWAYS: internet-gateway`
- **THEN** the facade HTTPRoute has exactly one parentRef, Service `{{ .Values.SERVICE_NAME }}` with group `""`

#### Scenario: Facade path mapping
- **WHEN** a Spring controller has `@RequestMapping("/svc")`, `@FacadeGatewayRequestMapping("/facade")` and `@Route(RouteType.FACADE)`
- **THEN** the facade HTTPRoute has a rule matching `/facade` rewritten to `/svc`, and the gateway HTTPRoute has no rule for it

#### Scenario: Gateway mapping does not apply to facade
- **WHEN** a Spring controller has `@RequestMapping("/svc")`, `@GatewayRequestMapping("/gw")` and `@Route(RouteType.FACADE)` with no timeout, and it is the only facade route
- **THEN** its facade root `/svc` has no rewrite, so no facade HTTPRoute is generated

### Requirement: Output file format
The plugins SHALL write the file at `outputFile`, resolved against the base directory of the executing project, creating parent directories as needed. The file SHALL start with the "THIS FILE WAS AUTOMATICALLY GENERATED — DO NOT EDIT" header. All resources SHALL be wrapped in `{{- if eq .Values.SERVICE_MESH_TYPE "Istio" }}` … `{{- end }}`. Each HTTPRoute's labels SHALL be the configured `labels` when any are set. Otherwise they SHALL be the default set: `app.kubernetes.io/name`, `app.kubernetes.io/part-of`, `app.kubernetes.io/managed-by`, `deployment.netcracker.com/sessionId`, `deployer.cleanup/allow: "true"`, `app.kubernetes.io/processed-by-operator: istiod`. Running a plugin twice on the same classes SHALL produce byte-identical output.

#### Scenario: No routes
- **WHEN** no route is collected in the reactor
- **THEN** the output file contains only the header and the empty Istio guard

#### Scenario: Custom labels replace defaults
- **WHEN** `labels` contains `team: platform`
- **THEN** every generated HTTPRoute has exactly the label `team: platform`

#### Scenario: Deterministic output
- **WHEN** a plugin runs twice on unchanged compiled classes
- **THEN** both output files are byte-identical
