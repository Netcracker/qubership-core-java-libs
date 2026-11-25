# AGENTS.md — core-springboot-starter

## Module

Parent POM for Spring Boot microservices. POM-only, no Java source. Inherits from `spring-boot-starter-parent` (v4.0.5), imports `cloud-core-java-bom`, and pulls in all `core-microservice-framework` artifacts as managed dependencies.

## Build

```bash
mvn install       # installs the POM to local repo for use by dependent services
```

## Extension Guide

- Version bumps to `spring-boot-starter-parent` here ripple to all consumer services — verify downstream compatibility before changing.
- Do not add Java source code here.
