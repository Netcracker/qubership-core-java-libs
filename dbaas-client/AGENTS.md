# AGENTS.md — dbaas-client

## Module

Database-as-a-Service client. Obtains connection strings and credentials from the `dbaas-agent` sidecar (default `http://dbaas-agent:8080`) so services never hold static DB credentials.

## Sub-modules

```
dbaas-client-java/
  ├── dbaas-client-postgres-starter    # PostgreSQL DataSource via DBaaS
  ├── dbaas-client-mongo-starter       # MongoDB MongoClient via DBaaS
  ├── dbaas-client-cassandra-starter   # Cassandra CqlSession via DBaaS
  ├── dbaas-client-opensearch-starter  # OpenSearch RestClient via DBaaS
  ├── dbaas-client-clickhouse-starter  # ClickHouse client via DBaaS
  ├── dbaas-client-arangodb-starter    # ArangoDB client via DBaaS
  └── dbaas-client-redis-starter       # Redis client via DBaaS
dbaas-client-bom-parent/              # BOM
dbaas-client-parent/                  # shared build POM
dbaas-client-report-aggregate/        # JaCoCo aggregate
```

## Build

```bash
mvn verify                  # requires Docker (Testcontainers)
mvn verify -DskipTests      # skip integration tests when Docker is unavailable
```

## Configuration

Agent address via `application.yml` property `dbaas.api.url` (default `http://dbaas-agent:8080`).

## Extension Guide

- New database type → add `*-base` + `*-starter` sub-modules under `dbaas-client-java/`, register both in `dbaas-client-java/pom.xml` and in the BOM.
- Each starter must use `@ConditionalOnClass` so it auto-configures only when the driver is on the classpath.

## Notes

- Do not hardcode database credentials — all credentials must flow through the DBaaS agent response.
