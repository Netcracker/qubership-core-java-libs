[![Coverage](https://sonarcloud.io/api/project_badges/measure?metric=coverage&project=Netcracker_qubership-core-quarkus-extensions)](https://sonarcloud.io/summary/overall?id=Netcracker_qubership-core-quarkus-extensions)
[![duplicated_lines_density](https://sonarcloud.io/api/project_badges/measure?metric=duplicated_lines_density&project=Netcracker_qubership-core-quarkus-extensions)](https://sonarcloud.io/summary/overall?id=Netcracker_qubership-core-quarkus-extensions)
[![vulnerabilities](https://sonarcloud.io/api/project_badges/measure?metric=vulnerabilities&project=Netcracker_qubership-core-quarkus-extensions)](https://sonarcloud.io/summary/overall?id=Netcracker_qubership-core-quarkus-extensions)
[![bugs](https://sonarcloud.io/api/project_badges/measure?metric=bugs&project=Netcracker_qubership-core-quarkus-extensions)](https://sonarcloud.io/summary/overall?id=Netcracker_qubership-core-quarkus-extensions)
[![code_smells](https://sonarcloud.io/api/project_badges/measure?metric=code_smells&project=Netcracker_qubership-core-quarkus-extensions)](https://sonarcloud.io/summary/overall?id=Netcracker_qubership-core-quarkus-extensions)

# Cloud Core Quarkus

**cloud-core-quarkus-extensions** library provides common functionality for Quarkus based microservices

## Modules

| Module                    | Description                                                                  | Documentation                                 |
| ------------------------- | ---------------------------------------------------------------------------- | --------------------------------------------- |
| build-parent              | Cloud Core Quarkus based build parent                                        |                                               |
| context                   | Context propagation modules                                                  | [Readme](context\/README.md)                  |
| dbaas-client              | Postgres and mongo dbaas-clients                                             | [Readme](dbaas-client/README.md)              |
| routes-registrator        | Sending requests to control plane for route registration                     | [Readme](routes-registrator/README.md)        |
| springcloud-config-source | Cloud Core Quarkus config springcloud                                        | [Readme](springcloud-config-source/README.md) |
| stomp-ws-server           | Allows to run a STOMP server over sockJs or over standard websocket protocol | [Readme](stomp-ws-server/README.md)           |
| maas-client               | Tiny client library to MaaS                                                  | [Readme](maas-client/README.md)               |
| consul-config-source      | Properties source for Consul                                                 | [Readme](consul-config-source/README.md)      |
