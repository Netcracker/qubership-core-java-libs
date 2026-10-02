[![Maven build](https://github.com/Netcracker/qubership-core-java-libs/actions/workflows/maven-deploy.yml/badge.svg)](https://github.com/Netcracker/qubership-core-java-libs/actions/workflows/maven-deploy.yml)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?metric=coverage&project=Netcracker_qubership-core-java-libs)](https://sonarcloud.io/summary/overall?id=Netcracker_qubership-core-java-libs)
[![duplicated_lines_density](https://sonarcloud.io/api/project_badges/measure?metric=duplicated_lines_density&project=Netcracker_qubership-core-java-libs)](https://sonarcloud.io/summary/overall?id=Netcracker_qubership-core-java-libs)
[![vulnerabilities](https://sonarcloud.io/api/project_badges/measure?metric=vulnerabilities&project=Netcracker_qubership-core-java-libs)](https://sonarcloud.io/summary/overall?id=Netcracker_qubership-core-java-libs)
[![bugs](https://sonarcloud.io/api/project_badges/measure?metric=bugs&project=Netcracker_qubership-core-java-libs)](https://sonarcloud.io/summary/overall?id=Netcracker_qubership-core-java-libs)
[![code_smells](https://sonarcloud.io/api/project_badges/measure?metric=code_smells&project=Netcracker_qubership-core-java-libs)](https://sonarcloud.io/summary/overall?id=Netcracker_qubership-core-java-libs)

# Overview

This is monorepo for all core java libraries.


## Modules

| Module | Description |
|--------|-------------|
| `core-internal-boms` | Internal BOMs (pure Java, Spring, Quarkus) managing common 3rd-party dependency versions for use inside the monorepo. |
| `core-external-boms` | External (published) BOMs (pure Java, Spring, Quarkus) exposing versions of qubership-core-java-libs modules. |
| [core-utils](core-utils/README.md) | Common utilities for Java based microservices (Kubernetes, TLS, etc.). |
| [core-error-handling](core-error-handling/README.md) | Base exception classes supporting error codes and REST error handling. |
| [core-process-orchestrator](core-process-orchestrator/README.md) | Process orchestration framework: task scheduling, execution management and process flow control with DB persistence. |
| [core-context-propagation](core-context-propagation/README.md) | Framework for propagating context values between microservices (REST, messaging) and storing custom request data. |
| [core-context-propagation-quarkus](core-context-propagation-quarkus/README.md) | Quarkus integration of the context propagation framework. |
| [core-microservice-framework-extensions](core-microservice-framework-extensions/README.md) | Libraries extending microservice-framework functionality (health indicators, springdoc/swagger). Deprecated. |
| [core-mongo-evolution](core-mongo-evolution/README.md) | Migration tool for microservice or tenant Mongo databases. |
| [core-junit-k8s-extension](core-junit-k8s-extension/README.md) | JUnit 5 extension to connect to Kubernetes in integration tests. |
| [core-restclient](core-restclient/README.md) | Microservice REST client implementation based on RestTemplate or WebClient. |
| [core-rest-libraries](core-rest-libraries/README.md) | Spring REST related libraries: route registration, config server loader, Consul config provider, pod secrets provider, log manager, REST API deprecation switcher, security, etc. |
| [core-blue-green-state-monitor](core-blue-green-state-monitor/README.md) | Listens for Blue-Green state changes in Consul and acquires/releases global and microservice locks (Java and Spring). |
| [core-blue-green-state-monitor-quarkus](core-blue-green-state-monitor-quarkus/README.md) | Quarkus extension providing the `BlueGreenStatePublisher` bean. |
| [dbaas-client](dbaas-client/README.md) | DBaaS client libraries and starters for Postgres, Mongo, Cassandra, ClickHouse, OpenSearch, Redis, ArangoDB. |
| [maas-client](maas-client/README.md) | Pure Java MaaS (Messaging as a Service) client for Kafka and RabbitMQ. |
| [maas-client-spring](maas-client-spring/README.md) | Spring integration of the MaaS client, including RabbitMQ support. |
| [maas-client-quarkus](maas-client-quarkus/README.md) | Quarkus extension based on the plain Java MaaS client. |
| [maas-declarative-client-commons](maas-declarative-client-commons/README.md) | Common part of the framework for declarative creation of Kafka consumers and producers with connection properties from MaaS. |
| [maas-declarative-client-spring](maas-declarative-client-spring/README.md) | Spring declarative Kafka client based on MaaS. |
| [maas-declarative-client-quarkus](maas-declarative-client-quarkus/README.md) | Quarkus declarative Kafka client based on MaaS. |
| [core-microservice-dependencies](core-microservice-dependencies/README.md) | BOM aggregating cloud-core libraries (except microservice-framework) to avoid version conflicts. |
| [core-quarkus-extensions](core-quarkus-extensions/README.md) | Quarkus extensions with common functionality for Quarkus based microservices (config sources, context, DBaaS, MaaS, routes registration, log manager, etc.). |
| [core-microservice-framework](core-microservice-framework/README.md) | Cloud-Core framework for rapid development of Spring based microservices. |
| [core-springboot-starter](core-springboot-starter/README.md) | `spring-boot-starter-parent` wrapper bringing essential cloud-core libraries through `dependencyManagement`. |
| `core-maven-plugins` | Maven plugins: [httproutes-generator-maven-plugin](core-maven-plugins/httproutes-generator-maven-plugin/README.md) generates Gateway API `HTTPRoute` manifests from compiled classes. |
