# Design

## Context

Today `httproutes-generator-maven-plugin` is a single plugin of about 970 lines. It scans `target/classes` with ClassGraph and reimplements Spring and JAX-RS annotation handling. The runtime libraries collect routes differently:

- **Spring (`route-registration-common-spring`)**:
  - `RouteAnnotationProcessor` takes Spring bean classes.
  - Annotations are looked up with `AnnotationUtils.findAnnotation`, which searches superclasses, interfaces and meta-annotations.
  - Methods are walked with `ReflectionUtils.doWithMethods`, which includes superclass methods.
  - Only `@GatewayRequestMapping`/`@FacadeGatewayRequestMapping` are read.
  - Path placeholders `${...}` are resolved from Spring properties.
- **Quarkus (`routes-registrator-deployment`)**:
  - `RoutesRegistrationProcessor` works on a Jandex index.
  - Candidates are classes declaring a route annotation on the class or on their own methods.
  - Class-level `@Route`/`@Gateway`/`@FacadeGateway` are read only where declared.
  - Class-level `@Path` is searched on the class, then interfaces, super-interfaces, superclasses.
  - Only `@Gateway`/`@FacadeGateway` are read.
- **Both** feed `ClassRoutesBuilder`/`ClassRoutesSet` from `route-registration-common`. These produce per-endpoint `RouteEntry` objects (`from`, `to`, `type`, `timeout`, `gateway`, `hosts`).

Constraints:
- Behavior SHALL be identical to the libraries (spec: Spring/Quarkus route collection).
- The only library change allowed is making methods `public`.
- Plugin code SHALL be as small as possible.
- Version stays `1.2.0-SNAPSHOT` (minor bump over the released `1.1.9`).

## Goals / Non-Goals

**Goals:**
- Collect routes by calling the libraries' own code. Add no annotation-reading logic beyond picking candidate classes.
- Keep a single shared implementation of root reduction, rendering and file writing.
- Keep each framework plugin around 60–80 lines and the common module around 200 lines.

**Non-Goals:**
- Routes from controllers that live in dependency jars. Candidates are reactor-module classes only. Dependency jars are used only to resolve supertypes and annotations.
- Spring beans registered through `@Bean` factory methods, or excluded by component-scan filters. Candidates approximate component scanning: a concrete class carrying `@Component` directly or as a meta-annotation.
- Quarkus `quarkus.index-dependency` configuration. Only compile-classpath jars and directories with `META-INF/jandex.idx` or `META-INF/beans.xml` are indexed.
- `RouteEntry.hosts` and the gateway name of facade/composite routes: they are silently dropped. No `spec.hostnames` is rendered and no warning is logged. Hostnames apply to a whole HTTPRoute, not a rule, and the facade HTTPRoute is attached to a Service. `RouteHostMapping` (`mesh.gateway.name`/`virtualHosts`) is left empty.
- Generating AuthorizationPolicy CRs.

## Decisions

### D1. Module layout
Under `core-maven-plugins`, all at `1.2.0-SNAPSHOT`:

| Module | Artifact | Packaging | Package |
|---|---|---|---|
| `httproutes-generator-common` | `httproutes-generator-common` | `jar` | `com.netcracker.routes.gateway.plugin` |
| `httproutes-generator-maven-plugin` | same as today | `maven-plugin` (Quarkus) | `com.netcracker.routes.gateway.plugin.quarkus` |
| `httproutes-generator-spring-maven-plugin` | new | `maven-plugin` | `com.netcracker.routes.gateway.plugin.spring` |

Dependency versions come from existing BOMs; see D11.

The existing artifact keeps its coordinates and becomes the Quarkus plugin, so Quarkus services only bump the version, and Spring services change the `artifactId`.
*Alternative:* three new artifacts plus a deprecated alias. Rejected: more artifacts to migrate and maintain.

### D2. Common module: abstract mojo plus pipeline
`AbstractGenerateRoutesMojo extends AbstractMojo`:
- **Parameters:** `reactorProjects`, `project`, `packages`, `servicePort`, `outputFile`, `backendRefVal` (default `{{ .Values.SERVICE_NAME }}`), `labels` (and the `Label` class).
- **One abstract method:** `List<SourcedRoute> collect(MavenProject module) throws Exception`, where `SourcedRoute(String className, RouteEntry entry)`.
- **One helper:** `classNames(File outputDirectory, String[] packages)` walks the `.class` files of a module's output directory and returns the names in `packages`. Both collectors use it, so no classpath-scanning library is needed (ClassGraph is dropped).
- **`execute()`:**
  1. For each reactor module, call `collect`.
  2. Check for `${`.
  3. Group by `entry.getType() == FACADE`.
  4. Turn each route into a prefix rule (D5).
  5. Reduce each group (D5), then filter facade roots (D6).
  6. Render (D7, D8), add the header and the Istio guard, and write the file.

  Library and validation errors become `MojoExecutionException`.

Concrete mojos add `@Mojo(name = "generate-routes", defaultPhase = PROCESS_CLASSES, aggregator = true, requiresDependencyResolution = ResolutionScope.COMPILE, threadSafe = true)`. They inherit the parameters. maven-plugin-plugin generates the descriptor from parent-class annotations in a dependency jar when the common jar is a dependency. Task 1.3 checks this first.
*Alternative:* duplicate the parameters in each mojo. Kept as a fallback if descriptor inheritance fails.

### D3. Spring collector: reuse `RouteAnnotationProcessor` through its public API
For each module:
1. **List class names** with the common `classNames` helper over `build.outputDirectory`.
2. **Build a class loader:** a `URLClassLoader` over the output directory and `getCompileClasspathElements()`. Its parent is the plugin class loader, so `Route`, `RequestMapping`, `Component` etc. are the same `Class` objects the library checks. Parent-first loading means the plugin's Spring and library versions win.
3. **Select candidates:** load each class with `Class.forName(name, false, loader)`, so no static initializers run. A load failure fails the build, naming the class. A class is a candidate when all of the following hold:
   - `!isInterface && !isAbstract`;
   - `AnnotatedElementUtils.isAnnotated(c, Component.class)`. This uses "get" semantics, i.e. direct or meta-annotation only, like component scanning;
   - `RouteAnnotationProcessorUtil.isAnnotatedWithRoute(c) || isAnnotatedWithFacadeRoute(c)`, the library's own public check.
4. **Collect:** `new RouteAnnotationProcessor(formatter, null, new RouteHostMapping())`, then `scanForRoutes(c)`.
   - `scanForRoutes(Class)` is public and does not use the `ApplicationContext`.
   - `formatter` is a `RouteFormatter` whose `StringValueResolver` throws, naming the placeholder, so any `${...}` fails the build (spec: placeholders).
   - The private `microserviceName` field, normally injected through `@Value`, is set once by reflection to a non-empty sentinel (D9). This is the only reflection in the plugin. It needs no library change.
   - Exceptions from `scanForRoutes` are rethrown with the class name added, so library errors (including the placeholder error) name the class.

### D4. Quarkus collector: reuse `RoutesRegistrationProcessor`
Library change (only this): in `core-quarkus-extensions/routes-registrator/deployment/.../RoutesRegistrationProcessor.java`, make two methods `public` instead of `private`:
- `collectClassesWithRoutes(IndexView)`;
- `collectRoutesForClass(IndexView, ClassInfo, String, RouteHostMapping)`.

For each module:
1. **Build the index:**
   - A Jandex `Indexer` over every `.class` file in the output directory gives the module index.
   - For each compile-classpath element (jar or directory): if it has `META-INF/jandex.idx`, read it with `IndexReader`; otherwise, if it has `META-INF/beans.xml`, index its classes.
   - This is required, not just a parity detail: the processor reads `@Route` defaults through `valueWithDefault(index)`, so the annotation classes themselves must be indexed. `route-registration-common` ships `META-INF/beans.xml`, which covers them.
   - Combine them with `CompositeIndex.create(moduleIndex, depIndexes…)`.
2. **Select candidates:** `new RoutesRegistrationProcessor().collectClassesWithRoutes(index)`, keeping only the names `classNames` returned for the module.
3. **Collect:** for each candidate, `collectRoutesForClass(index, ci, SENTINEL, new RouteHostMapping()).build()`. `ClassRoutesBuilder.build()` is already public.
4. **Spring guard:** if any class from `classNames` (module classes in `packages`) is annotated with `org.springframework.stereotype.Controller` or `org.springframework.web.bind.annotation.RestController` in the module index, fail with "use httproutes-generator-spring-maven-plugin". Only scanned classes are checked, so a Spring jar that a Quarkus service pulls in transitively does not cause a false failure.

The plugin depends on `com.netcracker.cloud.quarkus:routes-registrator-deployment`. This brings Quarkus deployment artifacts and Jandex into the plugin class path only.
*Alternative:* copy the about 40 lines of Jandex lookup into the plugin to avoid the dependency and the visibility change. Rejected because behavior must come from the library itself.

### D5. Prefix rules and root reduction (common)
- **Prefix rule:** `Rule(String match, String target, long timeout, Set<String> sources)`. `sources` holds the class names, so errors on merged roots can name every class involved. Converting a `RouteEntry` follows the spec (normalize, cut at the first `{`/`*` segment of `from`, the tail of `to` must match ignoring variable names, the target must have no templates). It is about 15 lines, using `{[^/]*}` → `{}` for the tail comparison. That regex is internal only and never written out.
- **Reduction:** for each group, sort rules by segment count, then lexically. Keep roots in a `TreeMap<String, Rule>`. For each rule, walk the match itself and then its ancestor prefixes, from longest to shortest, and take the first one present in `roots`:
  - if that root's target plus the remaining suffix equals the rule's target, absorb the rule (max timeout);
  - otherwise the rule becomes a root. If a root with an equal match already exists with a different target, fail (spec: conflicts).

  After reduction, any root equal to `/` fails, naming its sources. This is about 20 lines.
- **Why longest-ancestor:** Gateway API picks the longest `PathPrefix` match, so a rule is only redundant when the root that would actually serve it produces the same target. Keeping inconsistent nested rules preserves the routing the library registered, without regex.

### D6. Groups and facade filter
Group by `RouteEntry.getType() == RouteType.FACADE`. The libraries already map:
- `gateways` = `public-gateway-service`, `private-gateway-service`, `internal-gateway-service` to PUBLIC, PRIVATE, INTERNAL;
- `gateways` = the microservice name or any composite gateway name to FACADE.

So the plugin never inspects `gateways` itself.

After reduction, the facade filter is all-or-nothing: if `anyMatch(r -> !r.match().equals(r.target()) || r.timeout() > 120_000)`, every facade root is kept; otherwise the facade group is cleared. The constant `DEFAULT_FACADE_TIMEOUT_MS = 120_000` lives next to the filter.

*Why not filter per root:* in Istio ambient, an HTTPRoute whose parentRef is the service is applied by the service's waypoint, and the waypoint returns 404 for every request that matches no rule. If only the facade roots that rewrite or extend the timeout were rendered, every other facade route of the service would start returning 404. When no root needs anything, dropping the whole group keeps the service free of a waypoint route, as before. Reduction inside the facade group is still safe: a rule is only absorbed when the root's prefix serves it with the same target, so no facade request loses its match.

*Alternative:* render the facade group whenever it is non-empty. Rejected because most services would then get a waypoint HTTPRoute that changes nothing.

### D7. Gateway parentRefs resolved by Helm
The gateway HTTPRoute's `parentRefs` is a constant Helm snippet emitted by the renderer, not data computed at build time. There is no `gateways` Maven parameter. The snippet:
1. reads `.Values.HTTPROUTES_GENERATOR_GATEWAYS`, applying `default "public-gateway,private-gateway,internal-gateway-service"`. `default` treats an unset value, `""` and an empty list as empty;
2. splits a string value with `splitList ","` and leaves a list value as is;
3. loops with `range`, `trim`s each name and skips blank names;
4. emits `group: ""`/`kind: Service` for `internal-gateway-service`, and `group: gateway.networking.k8s.io`/`kind: Gateway` for every other name.

The facade HTTPRoute is a separate resource. It keeps a plain Jackson-rendered single `ParentRef("", "Service", "{{ .Values.SERVICE_NAME }}")` and never reads the value.

### D8. Renderer: small records plus Jackson
- Keep Jackson YAML with `NON_NULL` and `NON_EMPTY` inclusion, so that empty `filters` and absent `timeouts` disappear.
- Use about six tiny records, in field order: `Resource`, `Meta`, `Spec`, `ParentRef`, `Rule`, `Backend`.
- For the gateway HTTPRoute, `Spec.parentRefs` holds a placeholder string. After serialization, the placeholder line is replaced with the D7 snippet, indented the way Jackson indents sequences.
- The backendRef name is `backendRefVal`.
- Use `Map.of` only for single-key maps, to keep key order deterministic.
- Keep `formatDuration` as is. Rule order comes from the `TreeMap`.
- Removed: the regex conversion, the banner, the specificity sort, and the per-type grouping.

### D9. Microservice name sentinel
Both libraries require a non-empty microservice name. `ClassRoutesSet.addRoutesBasedOnGateways` branches on it: when a `gateways` value equals the real microservice name, it resolves the route's incoming ("from") path from the facade mapping (`@FacadeGatewayRequestMapping`/`@FacadeGateway`, `classWideFacadePathsFrom`); otherwise — border name or not — it resolves the path from the ordinary gateway mapping (`@GatewayRequestMapping`/`@Gateway`, `classWideGatewayPathsFrom`). Either way the route is typed `FACADE` (or the matching border type), so grouping (D6) never depends on this branch. The plugins pass the constant `"{{ .Values.SERVICE_NAME }}"`. No real gateway can carry that name, so it never matches a user value.

Assumption: the plugins cannot know the real microservice name at build time, so a `gateways` value equal to it is always treated as a composite gateway, never as the legacy "own name" case. Consequences:
- **Grouping is unaffected**: the route still lands in the facade group either way, since both branches produce type `FACADE` (D6).
- **The incoming path can differ** for a service that still uses the legacy "own name" convention together with a facade-specific mapping: at runtime such a route resolves its "from" path via `@FacadeGatewayRequestMapping`/`@FacadeGateway`; the plugins always resolve it via the ordinary gateway mapping (or the route's own path, if no gateway mapping is set) instead. A service relying on this specific combination gets a different "from" path out of the plugin than what it registers at runtime, and must switch to a composite gateway name (not its own) or drop the facade-specific mapping to get matching output.
- The library's check "`hosts` is not allowed with the microservice's own gateway name" is not repeated by the plugins for this reason. `hosts` combined with a border gateway is still rejected, because that check runs inside the library code the plugins call.

This is a deliberate, inherent limitation — not a defect to fix in code — because the real microservice name is a runtime property value, unavailable during the Maven build.

### D10. Tests
- **common:**
  - unit tests for prefix-rule conversion, reduction (every spec scenario under "Prefix rules" and "Root reduction"), the all-or-nothing facade filter (including a mixed group where one rewrite root brings in the plain roots), and the renderer (parentRefs snippet, backendRef, facade isolation, labels, determinism, no regex);
  - a mojo test with a stub `collect` for the empty-output case and error wrapping.
- **spring plugin:**
  - Move the existing `SpringTestController*` fixtures. Add fixtures for the spec scenarios: abstract base plus concrete subclass, path override, `@Gateway` ignored, repeated `@Route`, `@FacadeGatewayRequestMapping`, `@Route(gateways = "private-gateway-service")`, `@Route(gateways = "my-composite-gateway")`, a `${}` placeholder, and a class-level `@Route` without a path (fails).
  - Run the collector on `target/test-classes` with the test classpath.
  - Expected roots, using library semantics:
    - Spring1 → `/api/v1/test1`, `/api/v1/test2` at 250s;
    - Spring2 → `/test1`, `/test2`;
    - Spring3 → `/class/path/from` and `/class/path/from2` → `/class/path/to`, plus four nested roots, because the method-level `@GatewayRequestMapping` changes the suffix: `/class/path/from{,2}/method/path/from{,2}` → `/class/path/to/method/path/to`;
    - Spring8 scanned on its own → gateway `/class/path/from` → `/class/path/to` at 20s, and no facade HTTPRoute (its only facade root has no rewrite, 10s ≤ 2m);
    - Spring8 together with the `@FacadeGatewayRequestMapping("/facade")` fixture → the facade HTTPRoute has `/facade` → `/svc` and also Spring8's plain facade root at 10s;
    - the Spring6 base → `/custom/test1` → `/api/v1/test4/test1`.
- **quarkus plugin:**
  - Move the existing `QuarkusTestController*` fixtures. Copy the library's `resource/hierarchy` fixtures (`Case1`–`Case3`, `ICase1`–`ICase3`) and assert the roots match the runtime processor's output for the same classes. Add `@GatewayRequestMapping` ignored, `@FacadeGateway`, and the Spring guard.
  - Quarkus3 → `/class/path/from` → `/class/path/to`, plus two nested roots `/class/path/from/method/path/from{,2}` → `/class/path/to/method/path/to` (method-level `@Gateway`).
  - Quarkus4 (`@Gateway("test/sleep")`, `@Path("/sleep")`, class-level `@Route(PUBLIC)`) → gateway root `/test/sleep` → `/sleep`.
- **integration:** a descriptor check (inherited parameters present in both plugins), and `helm template` of the generated snippet.

### D11. Dependency management (existing BOMs only)
No new BOM or parent module. Each module pom declares its own `cloud-core.*.version` properties, as today's plugin pom already does. The release automation rewrites them wherever they appear. Each module then imports existing BOMs in `<dependencyManagement>` and lists repo dependencies without `<version>`.

| Module | Properties | Imported BOMs | Repo dependencies (no version) |
|---|---|---|---|
| `httproutes-generator-common` | `cloud-core.core-internal-boms.version`, `cloud-core.core-rest-libraries.version` | `com.netcracker.cloud:core-internal-bom` (Jackson, JUnit, Jandex), `com.netcracker.cloud:rest-libraries-bom` | `route-registration-common` |
| `httproutes-generator-spring-maven-plugin` | same as common | `core-spring-internal-bom`, `rest-libraries-bom` | `route-registration-common-spring`, `spring-context`, `httproutes-generator-common` (`${project.version}`, the usual sibling form) |
| `httproutes-generator-maven-plugin` (Quarkus) | `cloud-core.core-quarkus-extensions.version`, `cloud-core.core-rest-libraries.version` | `com.netcracker.cloud.quarkus:cloud-core-quarkus-bom-internal` (manages `route-registration-common` and imports `core-quarkus-internal-bom` / `quarkus-bom`), then `rest-libraries-bom` | `routes-registrator-deployment`, `io.smallrye:jandex`, `httproutes-generator-common` (`${project.version}`); test scope: `route-registration-common-spring`, only to compile the "`@GatewayRequestMapping` ignored" fixture |

Notes:
- **`rest-libraries-bom`** only imports `rest-legacy-bom`, which manages repo artifacts. It therefore adds no Spring third-party versions to common.
- **`routes-registrator-deployment` has no existing internal BOM entry.** `cloud-core-quarkus-bom-publish` lists only the runtime `routes-registrator`. The Quarkus plugin pom therefore adds one managed entry in its own `<dependencyManagement>` with `${cloud-core.core-quarkus-extensions.version}`, and the `<dependency>` has no version. This is the same pattern `maas-client-quarkus` uses for Quarkus-extension artifacts.
- **Framework alignment:** importing `cloud-core-quarkus-bom-internal` in the Quarkus plugin keeps the transitive Quarkus deployment artifacts on `quarkus-bom` versions. Importing `core-spring-internal-bom` in the Spring plugin does the same for Spring.
- **Third-party versions no internal BOM manages** (`maven-plugin-api`, `maven-plugin-annotations`, `maven-core`, `maven-plugin-plugin`) stay declared in the module poms that use them, as today. `provided` scope is not transitive, so each module that compiles mojo code declares the Maven APIs itself. The Java 21 compiler setting is repeated per module, also as today.
- **Aggregator:** `core-maven-plugins/pom.xml` stays a pure aggregator with `maven.deploy.skip=true`. It is not a parent, because children would inherit the skip.

*Alternatives:*
- Importing the umbrella `core-quarkus-external-bom`. Rejected because it ties the plugins to the consumer-facing external-BOM release and adds unrelated version management.
- Adding `routes-registrator-deployment` to `cloud-core-quarkus-bom-publish`. Rejected because it would be a library change beyond making methods public.
- New `maven-plugins-bom-internal`/`maven-plugins-parent` modules. Rejected because existing BOMs suffice.

## Risks / Trade-offs

- [The plugin bundles a specific library version, while the service may depend on a different `route-registration`/`routes-registrator` version] → Output follows the plugin's library version. Both are released from this monorepo with aligned versions. The README tells services to keep the plugin and library versions in step.
- [Reflection sets `RouteAnnotationProcessor.microserviceName`; a rename breaks the Spring plugin] → It sits in the same repo, and the Spring plugin tests fail immediately.
- [The Quarkus plugin pulls Quarkus deployment dependencies, which means a large download for the plugin] → Build-time only, nothing reaches the service artifact.
- [Class loading needs the module's compile classpath; aggregator dependency resolution makes builds slower] → This is the price of exact Spring semantics (meta-annotations, interfaces, bridge methods).
- [Bean detection approximates component scanning] → Documented under non-goals.
- [Routes that the old plugin produced from class-level paths now come from all endpoints] → Reduction keeps the output small. Inconsistent nested rewrites become extra rules rather than regexes.
- [Services with a class-level `@Route` and no class-level path, or with paths starting with a template, now fail the build with `/`] → BREAKING, listed in the proposal. The error names the class. The README explains how to add a class-level path.
- [Spring services keep using `httproutes-generator-maven-plugin` after upgrading] → The Quarkus plugin fails when module classes carry `@RestController`/`@Controller`, with a message pointing to the Spring artifact.
- [A Quarkus service using the `quarkus-spring-web` compatibility layer (Spring `@RestController` on Quarkus) is rejected by the Spring guard] → `routes-registrator` reads only `@Path`, so such classes yield `/` roots and would fail anyway. The error message names both plugins, so the owner can see which one applies.
- [The aggregator mojo now resolves every reactor module's compile dependencies. Sibling modules that are not yet built in the same session cannot be resolved] → This is the same constraint as today: the plugin must run after the scanned modules compile, e.g. bound in the last module of the reactor. The README states it.
- [A misspelled name in `HTTPROUTES_GENERATOR_GATEWAYS`] → Gateway API reports it in the HTTPRoute `status.parents` conditions. The README lists the default names.
- [Services that relied on the old `{{ .Values.DEPLOYMENT_RESOURCE_NAME }}` default] → They set `<backendRefVal>` explicitly. The README calls this out.
- [The public gateway exposes everything until a deny-unless-permit AuthorizationPolicy exists] → Out of the plugins' scope. The README states this prerequisite.

## Migration Plan

1. Merge the visibility change in `routes-registrator-deployment`, and release `core-quarkus-extensions`.
2. Release `core-maven-plugins` `1.2.0`, containing `httproutes-generator-common`, `httproutes-generator-maven-plugin` (Quarkus) and `httproutes-generator-spring-maven-plugin`. This is a minor bump; the pom versions do not change.
3. Services upgrade:
   - Quarkus services bump the version.
   - Spring services switch the `artifactId` to `httproutes-generator-spring-maven-plugin`.
   - Both regenerate, and fix any build failures (`/` roots, placeholders, inexpressible rewrites).
4. Deploy the Cloud-Core deny-unless-permit AuthorizationPolicy and the per-service allow policies before or together with the rollout.
5. Rollback: pin `httproutes-generator-maven-plugin` `1.1.9` (Spring services also switch back to that `artifactId`) and rebuild. The old per-type HTTPRoutes are regenerated.
