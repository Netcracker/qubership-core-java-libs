# Dependency Gate Maven Plugin

This module contains `com.netcracker.cloud.plugins:dependency-gate-maven-plugin`, the Maven adapter `maven-pins` of
the [dependency gate](https://github.com/Netcracker/qubership-core-infra/tree/main/tools/dependency-gate) of
qubership-core-infra. It finds dependency versions that go past the Renovate pins of qubership-core-infra and writes
them as gate findings (`findings.v1`), so that Renovate updates can be merged automatically without drifting away
from the Spring Boot and Quarkus platforms.
It is released with the other plugins of `core-maven-plugins` (one version and tag for all of them), starting with
1.2.0.

The plugin only finds. Levels, exceptions, the baseline of a pull request, the override label and the report are
the gate's: a finding never fails the build.

Pins are the package rules of `renovate-core-config.json` that list exact GAs with an `allowedVersions` pattern,
generated as max(Spring Boot BOM, Quarkus BOM) from `dependencies/*-dependencies` of qubership-core-infra.
Pattern-only rules (such as the global SNAPSHOT ban) and rules with conditions that cannot be evaluated for a GA
(`matchUpdateTypes`, `matchCurrentValue`, ...) are not pins. When several rules match, the last one wins, as in
Renovate. A version matches a pin when the `allowedVersions` regex matches it, as in Renovate (`/^5.30.0$/` does not
match `5.30`); above or below an exact pin is decided with Maven's `ComparableVersion`. Package name globs follow
minimatch (`*` and `?` do not match `/`); of the regex flags only `i` is supported, a rule with another flag is skipped
and listed in the debug log.

## What It Checks

The `check` goal runs once per reactor (an **aggregator** mojo) and looks at:

- **declared** versions: what the repository poms write, profiles included, with the property chain behind
  each version, so the finding points at the line to change;
- **effective** versions: dependencies, dependency management with imported BOMs, build plugins, plugin management,
  plugin dependencies, `annotationProcessorPaths` and extensions of each project's effective model;
- optionally **resolved** versions: the dependency tree, transitive dependencies included (`transitive`).

| Check | Finds |
|---|---|
| `pin-exceeded` | the version is above the pin |
| `pin-mismatch` | the version does not match a pin that is not an exact version, or is written differently from an exact one (`5.30` for `/^5.30.0$/`) |
| `pin-conflict` | `pin-exceeded` or `pin-mismatch` for a version managed by a BOM that its own rule allows |
| `pin-lagging` | a declared version is below the pin |
| `shared-property` | one property versions both pinned and unpinned artifacts: a bump of the unpinned one bypasses the pin |
| `feature-snapshot` | `X.Y.Z-<branch>-SNAPSHOT` of the branch snapshot deploy (a branch name of at least two words, such as `feature-foo`): Renovate cannot offer the release, Maven orders it above `X.Y.Z` |
| `snapshot` | any other SNAPSHOT that is not built by the repository, such as `1.0.0-RC1-SNAPSHOT` |

Levels of the checks are not decided here: see the check catalog of the
[dependency gate](https://github.com/Netcracker/qubership-core-infra/tree/main/tools/dependency-gate).

Findings that only the dependency tree shows are marked `transitive`; the gate caps them with
`ecosystems.maven.transitive` (infra default `warn`).

Details:

- Versions of the repository's own modules are not checked against pins.
- A BOM or parent that exceeds or mismatches its pin, or is a feature SNAPSHOT, is reported once; the managed versions
  it brings (its own and those of the BOMs it imports) that fail the same check are listed in its note instead of
  being reported one by one. Findings of other checks are reported as usual.
- Going past the Spring Boot or Quarkus platform BOM gets a hint to bump `dependencies/*-dependencies` in
  qubership-core-infra first.
- `pin-conflict` (for example `opentelemetry-instrumentation-bom` allowed as `/^2\.28\./` while its modules are
  pinned lower): the pins disagree with each other and the repository cannot fix that.
- Below-pin versions that come from BOMs or the dependency tree are not reported: a platform BOM legitimately sits
  below max(Spring Boot, Quarkus). For a pin that is a pattern, such a version is reported only when its leading
  numbers are above those the pattern starts with (`2.29.0` for `/^2\.28\./`); when the direction cannot be told, as
  for a negated pattern (`!/^3\.6\.0$/`), only declared versions are reported.

The `lint` goal logs inconsistencies inside the pins themselves (`pin-bom-drift`: a BOM pinned below its own
modules). It needs no project and is not part of the gate.

## Findings

`check` writes one `findings.v1` file per revision:

```json
{
  "schema": "dependency-gate-findings/v1",
  "adapter": { "name": "maven-pins", "ecosystem": "maven", "tool": "dependency-gate-maven-plugin 1.2.0", "status": "ok",
               "checks": ["pin-exceeded", "pin-mismatch", "pin-conflict", "pin-lagging", "feature-snapshot", "snapshot", "shared-property"] },
  "revision": "head",
  "findings": [
    { "check": "pin-exceeded", "package": "com.rabbitmq:amqp-client", "version": "5.37.0",
      "message": "5.37.0 is above the pin 5.30.0 (allowedVersions /^5.30.0$/)",
      "locations": [{ "file": "core-internal-boms/core-internal-bom/pom.xml", "line": 120 }] }
  ]
}
```

- `locations` are repository poms: first the lines to change (the property definition rather than the dependency that
  uses it), then, for versions from external BOMs or the dependency tree, the poms of the modules they show up in.
- When the check cannot finish (the pins cannot be read or contain no pin, an exclude is not a valid regex, a pom
  cannot be built or sets a parameter of the gate, see [Parameters](#parameters)), the file says
  `"status": "error"` with the reason, and the build fails: the gate reports a `tool-error`.
- When the dependency tree of a project cannot be fully resolved, the file still says `"status": "ok"`, and
  `adapter.reason` names the projects: transitive findings may be missing, and the gate shows the reason.

## Usage

In CI the plugin runs in the `maven` job of the reusable workflow `dependency-gate.yml` of qubership-core-infra,
on the pull request merge commit and on its base. The caller repository only adds the workflow:

```yaml
jobs:
  dependency-gate:
    uses: Netcracker/qubership-core-infra/.github/workflows/dependency-gate.yml@<sha>
```

Maven reads GitHub Packages with the workflow token (`packages: read` in the caller's `permissions`). Only if that is not enough, pass a read-only token
(`read:packages`) as `MAVEN_TOKEN`, never one that can write or delete packages: see the header of the workflow.

Exceptions, levels and excludes are set in `.github/dependency-gate.json` of the repository; the pins belong to
infra. See the gate's README.

Locally, from the repository root (the plugin is published to GitHub Packages, so the `github` repository must be
configured in `settings.xml` as a plugin repository), find, then evaluate with the gate:

```bash
mvn com.netcracker.cloud.plugins:dependency-gate-maven-plugin:<version>:check -Ddependency-gate.transitive=true
```

```bash
go run github.com/Netcracker/qubership-core-infra/tools/dependency-gate@<sha> evaluate -findings target/dependency-gate
```

Without a root pom, or for several independent roots, list them:

```bash
mvn com.netcracker.cloud.plugins:dependency-gate-maven-plugin:<version>:check -Ddependency-gate.poms=service-a,service-b
```

## Parameters

All parameters are set with `-Ddependency-gate.<name>` only. The checked pom belongs to the change under review, so
it must not set them: a plugin `<configuration>` (or the `default-cli` execution) would win over `-D`, and a
`dependency-gate.*` property in the pom `<properties>` would fill a parameter that the gate does not pass (for example
`excludesFile` when the repository has no excludes). The check then fails with `"status": "error"`, whether or not
the parameter is given with `-D`.

The only exception is `maxLocations`: it changes the build log alone, while the findings file and the gate's decision
stay the same. Every other parameter changes what is checked (`pins`, `poms`, `excludesFile`, `transitive`, `skip`),
how locations are matched to the gate's exceptions and baseline (`root`), or what the gate reads (`findings`,
`revision`).

| Property | Default | Description |
|---|---|---|
| `pins` | infra `main` `renovate-core-config.json` (URL) | pins: URL or file; a URL is read through the proxy of `settings.xml`, without proxy authentication |
| `findings` | `${project.build.directory}/dependency-gate/maven-pins.json`; without a pom `target/dependency-gate/maven-pins.json` | where to write the findings |
| `revision` | `head` | `head` (the change) or `base` (its base, the gate's baseline) |
| `transitive` | `false` | also check the resolved dependency tree |
| `poms` | | root poms or directories to check instead of the current reactor |
| `excludesFile` | | file with regular expressions of repository-relative pom paths not to check, one per line (Java `java.util.regex` syntax, matching any part of the path; blank lines are ignored) |
| `root` | closest directory with `.git` | repository root that locations are relative to |
| `maxLocations` | `5` | locations printed per finding in the build log |
| `skip` | `false` | skip the goal |
