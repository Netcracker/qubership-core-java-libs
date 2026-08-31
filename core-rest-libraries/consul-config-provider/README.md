# Consul config provider

Obtains a Consul ACL token for a microservice and keeps it fresh. The token is exchanged through
`POST /v1/acl/login` and published as a `TokenStorage` bean.

Two ways to obtain the token are supported:

- `kubernetes` — the projected Kubernetes service account token of the pod is sent to a Consul auth method of type
  `kubernetes`.
- `m2m` — an M2M token issued by `M2MManager` is sent to a Consul auth method named after the namespace.

## Login properties

The four properties below are read at startup, in the ConfigData phase and again when the `TokenStorage` bean is
built. In `kubernetes-with-m2m-fallback` mode the first login picks the way, and a later failure never switches it
back on its own.

| Property name                                       | Type                          | Default                              | Read when                                                    |
|-----------------------------------------------------|-------------------------------|--------------------------------------|--------------------------------------------------------------|
| `spring.cloud.consul.config.login.mode`             | `kubernetes-with-m2m-fallback`, `kubernetes` or `m2m` | `kubernetes-with-m2m-fallback`       | ConfigData phase and `TokenStorage` bean creation            |
| `spring.cloud.consul.config.login.auth-method`      | string                        | `applications-k8s-m2m`               | same, and only when the `kubernetes` way is used             |
| `spring.cloud.consul.config.login.audience`         | string                        | `netcracker`                         | same, and only when the `kubernetes` way is used             |
| `spring.cloud.consul.config.login.fallback-recheck-interval` | duration          | `5h`                                 | same, and only in the fallback mode                          |

In `kubernetes-with-m2m-fallback` mode the `kubernetes` way is tried first. If it fails, the pod falls back to the
`m2m` way and logs one `INFO` record with the reason, the Consul response code, and a truncated response body. In
`kubernetes` mode there is no probe and no fallback, and neither `M2MManager` nor the namespace is needed. In `m2m`
mode the auth method name and the audience are not used at all.

The fallback is temporary. Once `fallback-recheck-interval` has passed, the next scheduled relogin tries the
`kubernetes` way again, and the first success switches the pod over for good, with a second `INFO` record. Going back
to `m2m` never happens. The choice survives the two phases of the startup: the `TokenStorage` bean reads the auth
method of the token the ConfigData phase already obtained, from `/v1/acl/token/self`, and picks up from there instead
of probing again. The recheck rides on the scheduled relogin instead of a timer of its own, so it needs
`MaxTokenTTL` on the auth method: without it Consul issues a token that never expires, nothing is scheduled, and
nothing is rechecked.

An unknown value of `spring.cloud.consul.config.login.mode` fails the startup.

The default is the auth method the platform registers. Set `spring.cloud.consul.config.login.auth-method` only if
your Consul names it differently.

To turn the ACL token exchange off altogether — for local runs and tests — set
`spring.cloud.consul.config.m2m.enabled=false`. None of the login properties are read then.

## Reading the projected token outside a pod

The `kubernetes` way reads the token from `/var/run/secrets/tokens/<audience>/token`. To run it outside a cluster,
point the token directory elsewhere:

```properties
com.netcracker.cloud.security.kubernetes.tokens.dir=/path/to/tokens
```
