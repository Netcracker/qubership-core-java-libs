# Proposal

## Why

`httproutes-generator-maven-plugin` copies the legacy Cloud-Core route model into Istio: one HTTPRoute per route type (PUBLIC/PRIVATE/INTERNAL/FACADE) and one rule per endpoint. Endpoints with `{var}` path segments become `RegularExpression` matches. Those matches conflict with sibling `PathPrefix` rules and cannot be used with `ReplacePrefixMatch` rewrites, so the plugin has to print a "MANUAL REVIEW REQUIRED" warning.

The plugin also reimplements annotation scanning on its own. Its handling of class inheritance, `@Gateway`/`@GatewayRequestMapping`, facade mappings, `@Routes`, `@FacadeRoute` and `@Route(gateways)` differs from the libraries that register routes at runtime: `route-registration-common-spring` for Spring and `routes-registrator` for Quarkus. As a result, the HTTPRoutes can disagree with what the same service registered in the legacy mesh.

The new Istio migration strategy drops the PUBLIC/PRIVATE/INTERNAL distinction, along with the legacy forbidden routes (`allowed: false`) and deny routes (`deny: true`) that returned 404. Under the new strategy:

1. Every non-FACADE route is registered on every API gateway: public, private, internal, and any gateway added later (e.g. internet-gateway).
2. Every route is allowed on the private and internal gateways.
3. Cloud-Core ships a deny-unless-permit AuthorizationPolicy for public-gateway: action `ALLOW` with no rules.
4. Each microservice ships its own hand-written AuthorizationPolicy for public-gateway that allows only its public subset of routes. The same mechanism can also narrow routes allowed by out-of-the-box policies, which replaces legacy deny routes.
5. FACADE routes stay in their own mesh-level CR.

Access to the public gateway is therefore controlled by AuthorizationPolicy CRs, not by which gateways a route is attached to. The plugin only needs a minimal set of prefix ("root") rules covering exactly the routes the libraries would register. It can compute those routes with the libraries' own code instead of reimplementing it.

## What Changes

- **BREAKING**: The plugin is split by framework:
  - `httproutes-generator-maven-plugin` (existing coordinates) becomes the **Quarkus** plugin.
  - A new `httproutes-generator-spring-maven-plugin` is added for Spring.
  - A new `httproutes-generator-common` jar holds the shared logic: parameters, root reduction, rendering and file writing.

  Spring services must switch to the Spring artifact. Each plugin fails the build when the reactor has only the other framework's markers among scanned classes, with an explicit Maven error line naming the correct `artifactId`.
- **BREAKING**: Routes are collected with the runtime libraries' own logic, so class inheritance, interfaces, `@Routes`, `@FacadeRoute`, `@Route(gateways)` and facade path mappings behave exactly as at runtime.
  - Spring uses `RouteAnnotationProcessor` (and its helpers) from `route-registration-common-spring`.
  - Quarkus uses `RoutesRegistrationProcessor` from `routes-registrator-deployment`.
  - As a consequence, the Spring plugin reads `@GatewayRequestMapping`/`@FacadeGatewayRequestMapping` but not `@Gateway`. The Quarkus plugin reads `@Gateway`/`@FacadeGateway` but not `@GatewayRequestMapping`. Facade routes take their incoming path from the facade mappings.
- Library change: in `RoutesRegistrationProcessor`, two methods, `collectClassesWithRoutes` and `collectRoutesForClass`, go from `private` to `public`. Nothing else changes in either library.
- **BREAKING**: Collected routes are split into two groups.
  - **Gateway group**: PUBLIC, PRIVATE and INTERNAL routes, including `@Route(gateways = …)` naming `public-gateway-service`, `private-gateway-service` or `internal-gateway-service`.
  - **Facade group**: FACADE routes, which also covers `@FacadeRoute`, `@Route(gateways = <microservice name>)` and `@Route(gateways = <any other gateway name>)`.

  Each group is reduced to its root routes. A route is dropped when a shorter root's prefix rewrite already produces the same target. Timeouts are merged by maximum.
- **BREAKING**: The build fails when:
  - a root would be `/`;
  - a route's path contains a `${...}` property placeholder;
  - a route cannot be expressed as a prefix rewrite;
  - the same prefix maps to different targets.
- **BREAKING**: No `RegularExpression` matches are ever generated. Every match is `PathPrefix`. A path is cut before its first segment containing `{`/`*`. The "MANUAL REVIEW REQUIRED" banner is removed.
- Gateway roots are rendered into one HTTPRoute, `{{ .Values.SERVICE_NAME }}-java-annotations`. Its parentRefs come from the optional Helm value `{{ .Values.HTTPROUTES_GENERATOR_GATEWAYS }}` (a list or a comma-separated string of gateway names), resolved at deploy time.
  - When the value is unset or empty, the gateways are `public-gateway`, `private-gateway` and `internal-gateway-service`.
  - `internal-gateway-service` is rendered as a `Service` parentRef (group `""`). Every other name is rendered as a `Gateway` parentRef (group `gateway.networking.k8s.io`).

  The per-type HTTPRoutes `-java-annotations-public`, `-private` and `-internal` are no longer generated.
- FACADE stays a separate CR. Facade roots are rendered into `{{ .Values.SERVICE_NAME }}-java-annotations-facade`, with the single parentRef Service `{{ .Values.SERVICE_NAME }}`. The facade group is all-or-nothing:
  - if no facade root rewrites its prefix and none has a timeout longer than the 2-minute default, no facade HTTPRoute is generated;
  - otherwise **all** facade roots are rendered.

  In Istio ambient, once a service has a waypoint HTTPRoute, every request to that service that matches no rule gets a 404. A facade HTTPRoute with only some of the routes would therefore break the rest.
- **BREAKING**: The default of `backendRefVal` changes from `{{ .Values.DEPLOYMENT_RESOURCE_NAME }}` to `{{ .Values.SERVICE_NAME }}`. Services can still override it with `<backendRefVal>`.
- Unchanged:
  - the parameters `packages`, `servicePort`, `outputFile` and `labels`;
  - the goal name `generate-routes`;
  - aggregator mode;
  - the Helm guard and the file header;
  - the timeout format.
- READMEs are rewritten for the new model, including a self-contained section on the Istio access-control strategy described above.

## Capabilities

### New Capabilities
- `httproutes-generator`: How the HTTPRoute generator Maven plugins turn annotated Spring and Quarkus controllers into Gateway API HTTPRoute manifests for Istio. Covers:
  - collecting routes with the runtime libraries' logic, framework per plugin;
  - splitting routes into the gateway and facade groups, and reducing each group to roots;
  - build failures;
  - the Helm-resolved gateway parentRefs, the backendRef and timeouts;
  - the output file format.

### Modified Capabilities
<!-- none: no existing specs in openspec/specs/ -->

## Impact

- Code:
  - `core-maven-plugins/httproutes-generator-maven-plugin` is rewritten as the Quarkus plugin.
  - New modules `core-maven-plugins/httproutes-generator-common` and `core-maven-plugins/httproutes-generator-spring-maven-plugin`.
  - No new BOM or parent modules. The plugin modules import existing BOMs (`rest-libraries-bom`, `cloud-core-quarkus-bom-internal`, `core-*-internal-bom`) and declare `route-registration-*`, `routes-registrator-deployment` and the other repo dependencies without `<version>`.
  - `core-maven-plugins/pom.xml` lists the new modules.
  - In `core-quarkus-extensions/routes-registrator/deployment`, two methods of `RoutesRegistrationProcessor` become `public`.
- Release order: `core-quarkus-extensions` (for the visibility change) must be released before the plugins.
- Generated manifests: services that regenerate get at most two HTTPRoutes (gateway and facade) instead of up to four. Helm upgrade removes the old `-public`/`-private`/`-internal` HTTPRoutes because they are no longer in the chart. Rule sets now follow the runtime libraries exactly. Routes the old plugin derived differently (inheritance, facade mappings, `@Gateway` vs `@GatewayRequestMapping`) change accordingly.
- Operational dependency: public-gateway exposure now relies on the Cloud-Core deny-unless-permit AuthorizationPolicy and on per-service allow policies written by hand. The plugins do not generate either.
- Plugin API: `backendRefVal` keeps working but now defaults to `{{ .Values.SERVICE_NAME }}`. No Maven parameter is added or removed. Charts get a new optional value, `HTTPROUTES_GENERATOR_GATEWAYS`. Spring services change the plugin `artifactId`.
- Version: minor bump only. All plugin modules are `1.2.0-SNAPSHOT`, the next minor after the released `1.1.9`.
- Dependencies:
  - Repo artifact versions come from existing BOMs, selected by the `cloud-core.core-internal-boms.version`, `cloud-core.core-rest-libraries.version` and `cloud-core.core-quarkus-extensions.version` properties in each plugin module pom. The release automation already updates those properties. The only exception is `routes-registrator-deployment`, which no existing internal BOM manages: it is managed in the Quarkus plugin pom's own `<dependencyManagement>` with `${cloud-core.core-quarkus-extensions.version}`, as `maas-client-quarkus` does for Quarkus-extension artifacts.
  - The Spring plugin depends on `route-registration-common-spring`.
  - The Quarkus plugin depends on `routes-registrator-deployment`, which brings Quarkus deployment dependencies, used at build time only, plus Jandex.
  - Both plugins resolve the scanned modules' compile dependencies.
