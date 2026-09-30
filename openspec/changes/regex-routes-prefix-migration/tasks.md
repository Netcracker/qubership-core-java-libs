# Tasks

## 1. ForbiddenRoute annotation (route-registration-common)

- [x] 1.1 Add `ForbiddenRoute.java` to `core-rest-libraries/route-registration/route-registration-common/src/main/java/com/netcracker/cloud/routesregistration/common/annotation/`. Use `@Retention(RUNTIME)`, `@Target({TYPE, METHOD})`, `@Documented`, and `RouteType[] value()` with no default, and add Javadoc saying each value is a single gateway. Verify with `mvn -q install` of route-registration-common: the build succeeds and the existing route-registration tests pass unchanged, which confirms the legacy runtime ignores the annotation.
- [x] 1.2 Document `@ForbiddenRoute` in `core-rest-libraries/route-registration/README.md`: it is for the Istio generator only, and the legacy runtime ignores it. Verify that the README section renders and names the annotation's fully qualified class name.

All plugin paths and `mvn` commands in sections 2–6 are relative to `core-maven-plugins/httproutes-generator-maven-plugin`.

## 2. Scanner

- [x] 2.1 Leave the `HttpRoute` record unchanged. Add `ForbiddenPath(gatewayPath, gateways)` and `Problems(errors, warnings)`. Add `RouteScanner.collect(reactorProjects)`, which returns `Declarations(routes, forbidden, errors)`, and keep `collectRoutes` returning `Set<HttpRoute>`. Include classes and methods that have only `@ForbiddenRoute`, and resolve its gateway path with the same helpers as `@Route`. Verify with test controllers for a class-level forbidden path without `@Route` and a method-level forbidden path with `@GatewayRequestMapping`, and with the existing `GenerateRoutesMojoTest` assertions still passing.
- [x] 2.2 Report an error for a `@ForbiddenRoute` with an empty value or `FACADE`, naming the element. Verify with unit tests for both cases.
- [x] 2.3 Read all legacy route annotation forms (D9): `@Routes`, `@FacadeRoute`, `gateways` (border names give border routes, every other name a composite route), facade gateway paths from `@FacadeGateway`/`@FacadeGatewayRequestMapping` or the service path, and a method-level list replacing the class-level one. Treat a `gateways` array that holds only empty strings as empty, and ignore `hosts`. Change the `SpringTestController8#method1` assertion in `GenerateRoutesMojoTest` to gateway path = service path. Verify with test controllers for every "Legacy route annotation forms" scenario.

## 3. HTTPRoute generation

- [x] 3.1 Add `RoutePaths` with `cut`, `segments`, `overlaps`, `template` and `expressible`. Verify with unit tests covering `/x/`, `/{id}`, partial-segment variables and `/a/lit/x` vs `/a/{id}/x/y`.
- [x] 3.2 In `HttpRouteRenderer`, remove `RegularExpression`, `REGEX_MANUAL_REVIEW_WARNING` and the `path == gatewayPath` skip for FACADE. Cut the gateway and service paths (D2), group the border routes by match across types, merge them into the widest type's resource with the largest timeout and a warning, apply the Exact split with `ReplaceFullPath`, and report an error for any other group with different rewrites (D3). Sort by specificity with `Exact` before `PathPrefix`. Verify with unit tests for the cut, the trailing-slash grouping, the cross-type merge, the timeout warning, both Exact split scenarios, the conflict error, and a byte-identical re-render.
- [x] 3.3 Plan facade and composite routes the same way, and render the `-facade` HTTPRoute with all their rules only when one of them has a rewrite; otherwise warn when a timeout isn't applied (D9). Verify with unit tests for the "No rewrite", "One route with a rewrite" and composite route scenarios, and the timeout warning.

## 4. AuthorizationPolicy generation

- [x] 4.1 Add `AuthorizationPolicyRenderer`: compute the needed paths per gateway (D4), take explicit and, with `autoGenerateAuthorizationPolicies`, needed paths as rules, and report an error per missing path, listing its gateways and both fixes (D5). Report an error for an explicit path that equals an exposed route's path. Probe every rule path and its intersection with every overlapping exposed route, and add the exposed route legacy routes a denied probe by to `notPaths` (D4). Build `paths` and `notPaths` (D6), and report an error for paths `{*}` can't express. Render one policy per gateway with port `8080` (D7). Verify with unit tests for the internal-api rule with the status `notPaths`, the order rule with the items `notPaths`, a covering shorter route, an INTERNAL-only route, the missing-annotation error, automatic rules equal to explicit ones, nested forbidden paths, the contradiction error, the probe `notPaths` (path below a shorter route, pattern over a deeper route, automatic rules next to a catch-all route), the partial-segment variable error and the YAML shape.

## 5. Mojo wiring and docs

- [x] 5.1 Wire the pipeline in `GenerateRoutesMojo` (D1): add `autoGenerateAuthorizationPolicies` (`@Parameter(defaultValue = "false")`), collect the problems of all steps, log them, and throw `MojoFailureException("<n> route migration errors, see log")` without writing the file when there are errors. Verify with a Mojo-level test using a temp base dir: a failing project leaves an existing output file untouched, and a passing project writes the combined file in the specified order.
- [x] 5.2 Update the plugin `README.md`: the cut and rewrite, merging and the Exact split, the errors and warnings with examples, `@ForbiddenRoute` and `autoGenerateAuthorizationPolicies`, the AuthorizationPolicy output on port 8080, facade and composite routes, the BWC notes (403 vs 404, path normalization, shared-gateway DENY rules) and a migration section. Remove the regex and "MANUAL REVIEW" references.

## 6. Integration check

- [x] 6.1 Add an end-to-end controller set that reproduces the migration document example (routes 1, 2, 5, 6 and 7) with `@ForbiddenRoute`, and assert the generated HTTPRoutes and policies. Use a second set without the annotations and assert that the build fails. Verify with `mvn -q verify` in the plugin module.
- [x] 6.2 Run the second set with `autoGenerateAuthorizationPolicies` set to `true`, and assert that the policies equal the explicit ones.
- [x] 6.3 Add an end-to-end controller set with facade and composite routes, and assert the `-facade` HTTPRoute with all their rules, that no resource references the composite gateway, and that no `-facade` HTTPRoute is generated without a rewrite.
