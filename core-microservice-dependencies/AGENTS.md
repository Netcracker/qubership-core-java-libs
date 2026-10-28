# AGENTS.md — core-microservice-dependencies

## Module

Produces `cloud-core-java-bom` — the central Bill of Materials pinning dependency versions for the entire platform. POM-only, no Java source.

## Build

```bash
mvn install       # installs BOM to local repo
```

## Usage in Consumers

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>com.netcracker.cloud</groupId>
      <artifactId>cloud-core-java-bom</artifactId>
      <version>${cloud-core-java-bom.version}</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>
```

## Extension Guide

- Bumping a transitive library version platform-wide → change it in `cloud-core-java-bom/pom.xml`.
- Adding a library used by 2+ modules → declare it here; consuming modules omit the `<version>` tag.

## Notes

- No Java source code belongs here.
