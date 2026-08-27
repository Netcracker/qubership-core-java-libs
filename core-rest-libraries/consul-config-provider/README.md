# Consul config provider

Obtains a Consul ACL token for a microservice and keeps it fresh. The token is exchanged through
`POST /v1/acl/login` and published as a `TokenStorage` bean.

Two ways to obtain the token are supported:

- `kubernetes` — the projected Kubernetes service account token of the pod is sent to a Consul auth method of type
  `kubernetes`.
- `m2m` — an M2M token issued by `M2MManager` is sent to a Consul auth method named after the namespace.

## Login properties

The three properties below are read at startup, in the ConfigData phase and again when the `TokenStorage` bean is
built. A pod keeps one way of obtaining the token for its whole life: in `kubernetes-with-m2m-fallback` mode the first login decides, and a
failure of a later login does not switch the way back.

| Property name                                       | Type                          | Default                              | Read when                                                    |
|-----------------------------------------------------|-------------------------------|--------------------------------------|--------------------------------------------------------------|
| `spring.cloud.consul.config.login.mode`             | `kubernetes-with-m2m-fallback`, `kubernetes` or `m2m` | `kubernetes-with-m2m-fallback`       | ConfigData phase and `TokenStorage` bean creation            |
| `spring.cloud.consul.config.login.auth-method`      | string                        | `kubernetes-auth-method-placeholder` | same, and only when the `kubernetes` way is used             |
| `spring.cloud.consul.config.login.audience`         | string                        | `netcracker`                         | same, and only when the `kubernetes` way is used             |

In `kubernetes-with-m2m-fallback` mode the `kubernetes` way is tried first. If it fails, the pod falls back to the `m2m` way and logs one
`INFO` record with the reason, the Consul response code, and a truncated response body. In `kubernetes` mode there is
no probe and no fallback, and neither `M2MManager` nor the namespace is needed. In `m2m` mode the auth method name and
the audience are not read at all.

An unknown value of `spring.cloud.consul.config.login.mode` fails the startup.

The default auth method name is a placeholder. Set
`spring.cloud.consul.config.login.auth-method` to the name of the auth method registered in your Consul.

To turn the ACL token exchange off altogether — for local runs and tests — set
`spring.cloud.consul.config.m2m.enabled=false`. None of the login properties are read then.

## Reading the projected token outside a pod

The `kubernetes` way reads the token from `/var/run/secrets/tokens/<audience>/token`. To run it outside a cluster,
point the token directory elsewhere:

```properties
com.netcracker.cloud.security.kubernetes.tokens.dir=/path/to/tokens
```
