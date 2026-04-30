# AGENTS.md — core-junit-k8s-extension

## Module

JUnit 5 extension wiring Kubernetes infrastructure into integration/smoke tests: Fabric8 client injection, automatic port-forwarding, pod scaling, multi-cloud configuration.

## Sub-modules

- **`cloud-core-extension`** — the extension implementation.
- **`cloud-core-extension-bom`** — BOM for consumers.

## Build

```bash
mvn verify -DskipTests      # compile only; no cluster needed
# Full integration run requires Kubernetes 1.27+ (real cluster, Kind, or Minikube)
```

## Key Annotations

| Annotation | Effect |
|---|---|
| `@EnableExtension` | Activates the extension on a test class |
| `@SmokeTest` | Marks a test as a smoke/integration test (filtered in CI) |
| `@PortForward` | Requests port-forwarding to a pod/service before the test method |
| `@Cloud` | Selects the target cloud environment |

## Extension Guide

- New annotation → add to `cloud-core-extension/src/main/java/`; register the JUnit 5 callback in the extension class.
- Timeouts, retry counts, and watch durations are configured via JVM system properties at test runtime — see `cloud-core-extension/README.md` for the full property list.

## Notes

- Requires Java 21+ and valid `KUBECONFIG` or in-cluster credentials at test runtime.
