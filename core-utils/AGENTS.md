# AGENTS.md — core-utils

## Module

Low-level utilities for TLS configuration and Kubernetes projected-volume token handling. No Spring or Quarkus dependency; purely plain Java 21.

## Sub-modules

- **`tls`** — `TlsConfig` (interface), `TlsUtils`, `DefaultTlsConfig`: build `SSLContext` from PEM/JKS; detect mTLS setup.
- **`k8s`** — `KubernetesAudienceToken`, `KubernetesTokenVerifier`, `TokenSource`: read and verify Kubernetes service-account projected tokens.

## Build

```bash
mvn verify                          # from this directory
mvn verify -pl core-utils -am       # from repo root (includes transitive build)
```

## Extension Guide

- New TLS helper → add to `tls/src/main/java/`.
- New K8s token utility → add to `k8s/src/main/java/`.
- No framework imports allowed; keep plain Java 21.

## Notes

- TLS utilities are consumed upstream by `core-rest-libraries` and `dbaas-client` — avoid breaking API changes.
