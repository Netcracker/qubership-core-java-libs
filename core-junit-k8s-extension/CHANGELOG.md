# This page contains notably changes of cloud-core-extension project.

## 8.9.10
* `Features`
  - `io.fabric8:kubernetes-client` 7.5.2 → 7.9.0, `io.fabric8:kubernetes-client-bom` 7.4.1 → 7.9.0 (versions aligned).
  - Transitive: Vert.x 4.5.24 → 4.5.31 (Netty CVE fix: `4.1.130.Final` → `4.1.136.Final`), Jackson 2.20 → 2.21, snakeyaml-engine 2 → 3.
  - Default HTTP client unchanged (Vert.x 4). Vert.x 5 is not enabled.
  - **Breaking:** potentially breaking for consumers (test code using Fabric8 model). Typical `KubernetesClient` + port-forward usage is unaffected.
    - Removed: `scheduling.k8s.io/v1alpha1` and `v1alpha2`, `ClusterTrustBundle`, `IPAddress`, `ServiceCIDR`, `VolumeAttributesClass`, gateway-api `v1beta1` `ReferenceGrant`.
    - `VolumeMount` constructor signature changed (Kubernetes 1.37 added `bindMountOptions`). Builder usage is unaffected.
    - TLS trust failures now fail fast (Fabric8 7.8.0) instead of retrying.
  - Quarkus 3.33.x manages Fabric8 7.5.2 — import `quarkus-bom` before `cloud-core-extension-bom` if this 7.9.0 pin should win.
  - Netty version for Spring/Quarkus consumers is governed by their platform BOM.
  - Upstream notes: Fabric8 [v7.6.0](https://github.com/fabric8io/kubernetes-client/releases/tag/v7.6.0), [v7.6.1](https://github.com/fabric8io/kubernetes-client/releases/tag/v7.6.1), [v7.7.0](https://github.com/fabric8io/kubernetes-client/releases/tag/v7.7.0), [v7.8.0](https://github.com/fabric8io/kubernetes-client/releases/tag/v7.8.0), [v7.9.0](https://github.com/fabric8io/kubernetes-client/releases/tag/v7.9.0).
