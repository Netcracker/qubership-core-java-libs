# Tasks

Paths are relative to the repo root. "Build plugins" means `mvn -f core-maven-plugins/pom.xml verify`.

## 1. Library visibility and module scaffolding

- [x] 1.1 In `core-quarkus-extensions/routes-registrator/deployment/src/main/java/com/netcracker/cloud/quarkus/routesregistration/deployment/RoutesRegistrationProcessor.java`, change only the visibility of `collectClassesWithRoutes(IndexView)` and `collectRoutesForClass(IndexView, ClassInfo, String, RouteHostMapping)` from `private` to `public` (design D4). Verify:
  - `git diff` shows only those two modifier changes;
  - `mvn -f core-quarkus-extensions/routes-registrator/pom.xml install` passes, including `HierarchyRegistrationProcessorTest`.
- [x] 1.2 Create the module poms using existing BOMs only (design D11):
  - **`core-maven-plugins/httproutes-generator-common`** (`jar`, `1.2.0-SNAPSHOT`, package `com.netcracker.routes.gateway.plugin`):
    - properties `cloud-core.core-internal-boms.version` and `cloud-core.core-rest-libraries.version`;
    - imports `core-internal-bom` and `rest-libraries-bom`;
    - dependencies: `maven-plugin-api`, `maven-plugin-annotations` and `maven-core` (all `provided`), `route-registration-common`, `jackson-dataformat-yaml` and JUnit (the last three without a version).
  - **`core-maven-plugins/httproutes-generator-spring-maven-plugin`** (`maven-plugin`), empty for now:
    - the same properties as common;
    - imports `core-spring-internal-bom` and `rest-libraries-bom`;
    - dependencies: `httproutes-generator-common` (`${project.version}`); `route-registration-common-spring` and `spring-context` (no version); `maven-plugin-api`, `maven-plugin-annotations`, `maven-core` (`provided`); JUnit (test, no version);
    - build: `maven-plugin-plugin` with the `descriptor` goal, as in today's plugin pom.
  - **`core-maven-plugins/httproutes-generator-maven-plugin`**, repointed:
    - replace its properties with `cloud-core.core-quarkus-extensions.version` and `cloud-core.core-rest-libraries.version`;
    - import `com.netcracker.cloud.quarkus:cloud-core-quarkus-bom-internal`, then `rest-libraries-bom`;
    - add a managed entry `com.netcracker.cloud.quarkus:routes-registrator-deployment:${cloud-core.core-quarkus-extensions.version}`;
    - dependencies: `httproutes-generator-common` (`${project.version}`); `routes-registrator-deployment` and `io.smallrye:jandex` (no version); `maven-plugin-api`, `maven-plugin-annotations`, `maven-core` (`provided`); JUnit and `route-registration-common-spring` (test, no version);
    - remove ClassGraph, the compile-scope `route-registration-*` dependencies, `spring-web`, `jakarta.ws.rs-api` and the `core-spring-internal-bom` import;
    - keep `maven-plugin-plugin`;
    - delete the old main sources.
  - List the modules in `core-maven-plugins/pom.xml`, with common first. Add no new pom-only modules.

  Verify:
  - "Build plugins" compiles all modules;
  - in each module pom's `<dependencies>` section, no `com.netcracker.cloud*` dependency other than `httproutes-generator-common` has a `<version>`;
  - `mvn -f core-maven-plugins/httproutes-generator-maven-plugin/pom.xml dependency:tree` resolves `routes-registrator-deployment` and `route-registration-common` to the `core-quarkus-extensions`/`core-rest-libraries` SNAPSHOT versions;
  - every module is `1.2.0-SNAPSHOT`.
- [x] 1.3 Spike the descriptor inheritance (design D2). Add `AbstractGenerateRoutesMojo` to common with all shared `@Parameter` fields, `backendRefVal` defaulting to `{{ .Values.SERVICE_NAME }}`, and the `Label` class. Add a trivial concrete `@Mojo(name = "generate-routes", aggregator = true, requiresDependencyResolution = COMPILE, defaultPhase = PROCESS_CLASSES)` subclass to each plugin. Verify that each plugin's `target/classes/META-INF/maven/plugin.xml` lists `packages`, `servicePort`, `outputFile`, `backendRefVal` (default `{{ .Values.SERVICE_NAME }}`) and `labels`, and no `gateways` parameter. If inheritance does not work, switch to the fallback in design D2 and record the switch in design.md.

## 2. Common: prefix rules, root reduction, facade filter

- [x] 2.1 Add `SourcedRoute(String className, RouteEntry entry)`, `Rule(match, target, timeout, Set<String> sources)` and the `classNames(outputDirectory, packages)` helper (design D2) to common. Implement the RouteEntry → Rule conversion per the spec requirement "Prefix rules without regex": normalize, cut at the first `{`/`*` segment, check the tail matches ignoring variable names, check the target has no templates, and fail naming the class and the route. Also implement the `${` check from the spec requirement "Unresolvable placeholders fail the build". Verify with `RulesTest`, which covers every scenario of those two requirements, the normalization of `api//x/` → `/api/x`, and `classNames` (package filtering, nested classes included, non-class files ignored).
- [x] 2.2 Implement the per-group reduction (design D5): ascending segment count, absorption by the longest ancestor root with max timeout, inconsistent nested rules kept as their own roots, a conflict error naming the match, and an error for a `/` root naming its source classes. Also implement the all-or-nothing facade filter (design D6): keep every facade root if any root rewrites or has a timeout > `120_000`, otherwise clear the group. Verify with `RootReductionTest`, which covers every scenario of the spec requirements "Root reduction" and "Facade group rendered all-or-nothing", including "One facade root that needs it renders the whole group" and "Timeout exactly two minutes".

## 3. Common: renderer and mojo pipeline

- [x] 3.1 Implement the renderer (design D7, D8): small records, Jackson with `NON_NULL`/`NON_EMPTY`, the gateway HTTPRoute `{{ .Values.SERVICE_NAME }}-java-annotations` whose parentRefs is the constant `HTTPROUTES_GENERATOR_GATEWAYS` Helm snippet, the facade HTTPRoute `-java-annotations-facade` with the single Service `{{ .Values.SERVICE_NAME }}` parentRef, backendRef `backendRefVal`, `formatDuration`, default/custom labels, and omission of an empty HTTPRoute. Verify with `HttpRouteRendererTest`. It asserts:
  - the exact snippet text, including the default list and the Service/Gateway mapping;
  - backendRef names for the default and a custom value;
  - facade rules only in the facade resource, and no `HTTPROUTES_GENERATOR_GATEWAYS` reference in it;
  - `URLRewrite` only when the match differs from the target;
  - `request: 5s` for 5000 ms;
  - no `RegularExpression`/`MANUAL REVIEW`;
  - label replacement;
  - byte-identical output when rendering twice.
- [x] 3.2 Implement `AbstractGenerateRoutesMojo.execute()` (design D2):
  1. collect per reactor module;
  2. split into the gateway and facade groups by `RouteType.FACADE`;
  3. convert to rules, reduce each group, apply the all-or-nothing facade filter;
  4. render, add the header and the Istio guard;
  5. write `outputFile`, creating parent directories.

  Wrap all failures in `MojoExecutionException`. Verify with `AbstractGenerateRoutesMojoTest`, using a stub subclass whose `collect` returns fixed `RouteEntry`s. It covers:
  - the empty output (header plus empty guard only);
  - one gateway and one facade route written correctly;
  - PUBLIC/PRIVATE/INTERNAL entries landing in the gateway group and FACADE entries in the facade group (spec "Route types share one HTTPRoute" and "Only facade routes");
  - a facade group with one rewrite entry and one plain entry rendering both, and a facade group with only plain entries rendering no facade HTTPRoute;
  - a `/` root failing with the class name.

## 4. Spring plugin

- [x] 4.1 Implement `GenerateSpringRoutesMojo` in `core-maven-plugins/httproutes-generator-spring-maven-plugin` (design D3):
  - list class names with the common `classNames` helper;
  - load them with `Class.forName(name, false, loader)` through a `URLClassLoader` over the output directory plus `getCompileClasspathElements()`, with the plugin class loader as parent. A load failure fails the build, naming the class;
  - record reactor-wide Spring and Quarkus framework markers for every scanned class; after all modules, fail when Quarkus markers are present and Spring markers are absent with `Spring was not found. Please use httproutes-generator-maven-plugin for Quarkus service.`;
  - select candidates (concrete, `AnnotatedElementUtils.isAnnotated(c, Component.class)`, and `RouteAnnotationProcessorUtil.isAnnotatedWithRoute || isAnnotatedWithFacadeRoute`);
  - collect with `new RouteAnnotationProcessor(throwingFormatter, null, new RouteHostMapping())`, after setting `microserviceName` to the sentinel `"{{ .Values.SERVICE_NAME }}"` by reflection, then `scanForRoutes(c)`. Rethrow its exceptions with the class name.

  Verify with task 4.3.
- [x] 4.2 Move the existing `SpringTestController*`/`SpringTestControllerBaseFor*`/`RoutesTestConfiguration` fixtures from the old plugin's `src/test` into the Spring plugin's `src/test`. Add fixtures:
  - an abstract base plus a concrete subclass annotated only with `@RestController`;
  - a base/subclass `@RequestMapping` override;
  - `@Gateway` on a Spring controller;
  - a method with repeated `@Route(PUBLIC)` + `@Route(FACADE)`;
  - `@Route(gateways = "private-gateway-service")` and `@Route(gateways = "my-composite-gateway", hosts = "example.com")`;
  - `@FacadeGatewayRequestMapping("/facade")` + `@Route(FACADE)`;
  - `@GatewayRequestMapping("/gw")` + `@Route(FACADE)` without a timeout;
  - `@RequestMapping("${api.prefix}/users")`;
  - a class-level `@Route` without a class path.

  Verify that the test sources compile in the next test run.
- [x] 4.3 Write `GenerateSpringRoutesMojoTest`. It runs the collector and the common pipeline on `target/test-classes` with the test classpath. For the fixtures from 4.2, it asserts every scenario of the spec requirement "Spring route collection matches route-registration-common-spring", plus every scenario of "Route groups", and "Facade path mapping", "Gateway mapping does not apply to facade", "Spring placeholder in class path" and "Root slash fails". It also asserts the reference roots from design D10:
  - Spring1 → `/api/v1/test1`, `/api/v1/test2` at 250s;
  - Spring2 → `/test1`, `/test2`;
  - Spring3 → `/class/path/from` and `/class/path/from2` → `/class/path/to`, plus the four nested roots `/class/path/from{,2}/method/path/from{,2}` → `/class/path/to/method/path/to`;
  - Spring8 scanned on its own → gateway `/class/path/from` → `/class/path/to` at 20s, and no facade HTTPRoute;
  - Spring8 together with the `@FacadeGatewayRequestMapping("/facade")` fixture → the facade HTTPRoute contains `/facade` → `/svc` and also Spring8's plain facade root at 10s;
  - the Spring6 base → `/custom/test1` → `/api/v1/test4/test1`, and nothing from the un-annotated subclass.

  For each fixture class, the test also checks parity: the routes the plugin collects equal `new RouteAnnotationProcessor(...).scanForRoutes(c)` called directly. The test also writes the full output for the successful fixtures to `target/httproutes-test/annotations-httproutes.yaml`, which task 6.2 uses. Verify that the tests pass.
- [x] 4.4 Write `core-maven-plugins/httproutes-generator-spring-maven-plugin/README.md`. It covers:
  - coordinates and configuration;
  - the shared parameters, with `backendRefVal` defaulting to `{{ .Values.SERVICE_NAME }}`, plus a BREAKING note for services that need `{{ .Values.DEPLOYMENT_RESOURCE_NAME }}`;
  - that routes follow `route-registration-common-spring` exactly (inheritance, `@GatewayRequestMapping`/`@FacadeGatewayRequestMapping`, `@Gateway` not read), and that plugin and library versions should be kept aligned;
  - candidate rules and their non-goals;
  - the gateway/facade groups and the root reduction, with an example;
  - the all-or-nothing facade rule: if any facade root rewrites or has a timeout > 2m, all facade roots are rendered, otherwise none. Include the reason: an Istio ambient waypoint HTTPRoute returns 404 for every request to the service that matches no rule;
  - build failures (`/` root, `${}`, inexpressible rewrite, conflicts);
  - `@Route(hosts)` is ignored (no `hostnames` in the HTTPRoutes);
  - `HTTPROUTES_GENERATOR_GATEWAYS` (list or comma string; unset/empty means `public-gateway`, `private-gateway`, `internal-gateway-service`, with Service/Gateway kinds), with an example of each form;
  - no regex matches;
  - a self-contained "Access control on Istio" section, with no links to other markdown files, covering:
    - the legacy PUBLIC/PRIVATE/INTERNAL model with 404 forbidden routes, `allowed: false` and `deny: true`;
    - every route on every gateway, all routes allowed on private and internal;
    - the Cloud-Core deny-unless-permit AuthorizationPolicy (`ALLOW` with empty rules) on public-gateway;
    - per-service hand-written allow and narrowing policies replacing deny routes;
  - migration from `httproutes-generator-maven-plugin` (change the `artifactId`).

  Verify that every YAML example matches the output of the renderer tests (3.1), that the parameters match the plugin descriptor (1.3), and that `grep -nE "\]\([^)]*\.md\)" README.md` finds nothing.

## 5. Quarkus plugin (existing artifact)

- [x] 5.1 Implement `GenerateQuarkusRoutesMojo` in `core-maven-plugins/httproutes-generator-maven-plugin` (design D4):
  - build the Jandex index from the module output plus the compile-classpath jars and directories that contain `META-INF/jandex.idx` (read) or `META-INF/beans.xml` (indexed), combined with `CompositeIndex`;
  - record reactor-wide Spring and Quarkus framework markers from `classNames`; after all modules, fail when Spring markers are present and Quarkus markers are absent with `Quarkus was not found. Please use httproutes-generator-spring-maven-plugin for Spring service.`;
  - take candidates from `new RoutesRegistrationProcessor().collectClassesWithRoutes(index)`, filtered to the module's `classNames`;
  - collect with `collectRoutesForClass(index, ci, "{{ .Values.SERVICE_NAME }}", new RouteHostMapping()).build()`.

  Verify with task 5.3.
- [x] 5.2 Keep the existing `QuarkusTestController*` fixtures. Copy the library's `resource/hierarchy` fixtures (`Case1`–`Case3`, `ICase1`–`ICase3`) from `core-quarkus-extensions/routes-registrator/deployment/src/test/...`. Add fixtures:
  - a `@Path("/base")` base with a method `@Route` and a subclass without `@Route`;
  - `@GatewayRequestMapping("/gw")` on a Quarkus resource;
  - `@FacadeGateway` + `@Route(FACADE)`;
  - a facade resource without a rewrite and with a 300000 ms timeout, in its own package, so it can be scanned without other facade fixtures;
  - a `@RestController` class in its own package (e.g. `...plugin.quarkus.springguard`), which the other tests leave out of `packages`, for the Spring guard.

  The `@GatewayRequestMapping` fixture needs `route-registration-common-spring` on the test classpath (task 1.2). Verify that the test sources compile in the next test run.
- [x] 5.3 Rewrite `GenerateRoutesMojoTest` as `GenerateQuarkusRoutesMojoTest`. It indexes `target/test-classes` and runs the collector and the common pipeline. It asserts every scenario of the spec requirement "Quarkus route collection matches routes-registrator", plus "Facade root with long timeout", and the Spring guard (running with `packages` set to the guard fixture's package fails with the Spring plugin's name, while the other tests exclude that package). Reference roots:
  - Quarkus1 → `/api/v1/test1` at 250s;
  - Quarkus2 → `/test1`;
  - Quarkus3 → `/class/path/from` → `/class/path/to`, plus the nested roots `/class/path/from/method/path/from` and `/class/path/from/method/path/from2`, both → `/class/path/to/method/path/to` (the method-level `@Gateway` changes the suffix, so they are not absorbed);
  - Quarkus4 → `/test/sleep` → `/sleep`;
  - the `@FacadeGateway` + `@Route(FACADE)` fixture scanned on its own renders its rewritten facade root, and scanned together with a plain facade fixture (no rewrite, timeout ≤ 2m) renders both facade roots.

  For each fixture class, the test also checks parity: the plugin's collected routes equal the routes from calling `collectRoutesForClass(...).build()` directly. Verify that the tests pass.
- [x] 5.4 Rewrite `core-maven-plugins/httproutes-generator-maven-plugin/README.md` the same way as task 4.4, adapted for Quarkus:
  - routes follow `routes-registrator` exactly: interface-first `@Path` lookup, class-level `@Route`/`@Gateway` read only where declared, `@GatewayRequestMapping` not read;
  - which compile-classpath jars and directories are indexed (`META-INF/jandex.idx` or `META-INF/beans.xml`);
  - the Spring guard;
  - a note that this artifact is now Quarkus-only.

  Remove the old sections on route-type-to-parentRefs mapping, specificity sorting and regex. Verify with the same checks as task 4.4.

## 6. Integration checks

- [x] 6.1 Run "Build plugins" (`mvn -f core-maven-plugins/pom.xml clean verify`) and confirm the build succeeds. Then run `grep -rn "RegularExpression\|pathSpecificity\|MANUAL REVIEW" core-maven-plugins/*/src/main` and confirm there are no matches.
- [x] 6.2 Put `core-maven-plugins/httproutes-generator-spring-maven-plugin/target/httproutes-test/annotations-httproutes.yaml` (written by task 4.3, and containing both a gateway and a facade HTTPRoute) into a minimal Helm chart (`SERVICE_MESH_TYPE: Istio`, `SERVICE_NAME: svc`). Run `helm template` four times: with `HTTPROUTES_GENERATOR_GATEWAYS` unset, `""`, `[internet-gateway, internal-gateway-service]`, and `"internet-gateway, public-gateway"`. Confirm:
  - the parentRefs match the spec scenarios;
  - the output parses as YAML (e.g. `kubectl apply --dry-run=client -f -`);
  - the facade HTTPRoute is unchanged across all four renders.
- [x] 6.3 Confirm the versions: `git diff main -- core-maven-plugins/pom.xml core-maven-plugins/httproutes-generator-maven-plugin/pom.xml` shows no project `<version>` change, and the new modules use `1.2.0-SNAPSHOT`. Confirm that the plugin modules declare no dependency `<version>` (the same check as task 1.2). Confirm the only change outside `core-maven-plugins` and `openspec` is the two-modifier diff from task 1.1: `git diff --stat main -- core-rest-libraries core-quarkus-extensions` lists only `RoutesRegistrationProcessor.java`.
- [x] 6.4 Run `find core-maven-plugins/*/src/main/java -name '*.java' | xargs wc -l` and confirm the sizes against the design goals: common around 200 lines, each plugin around 60–80. If the total is clearly above that, simplify before finishing.
