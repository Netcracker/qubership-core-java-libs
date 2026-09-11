# Consul config provider

Obtains a Consul ACL token for a microservice and keeps it fresh. The token is exchanged through
`POST /v1/acl/login` and published as a `TokenStorage` bean.

Two ways to obtain the token are supported:

- `kubernetes` — the projected Kubernetes service account token of the pod is sent to the auth method the platform
  registers for it. Whether Consul reviews that token at the API server or validates its signature on its own is a
  property of the auth method, and the login request is the same either way.
- `m2m` — an M2M token issued by `M2MManager` is sent to a Consul auth method named after the namespace.

## Login properties

The four properties below are read at startup, in the ConfigData phase and again when the `TokenStorage` bean is
built. In `kubernetes-with-m2m-fallback` mode the first login picks the way the pod starts on, a recheck can move it
to the `kubernetes` way later, and nothing ever moves it back.

| Property name                           | Environment variable                    | Type                          | Default                        | Read when                                         |
|-----------------------------------------|-----------------------------------------|-------------------------------|--------------------------------|---------------------------------------------------|
| `consul.auth.mode`                      | `CONSUL_AUTH_MODE`                      | `kubernetes-with-m2m-fallback`, `kubernetes` or `m2m` | `kubernetes-with-m2m-fallback` | ConfigData phase and `TokenStorage` bean creation |
| `consul.auth.method`                    | `CONSUL_AUTH_METHOD`                    | string                        | `applications-k8s-m2m`         | same; not used in `m2m` mode                      |
| `consul.auth.audience`                  | `CONSUL_AUTH_AUDIENCE`                  | string                        | `netcracker`                   | same; read only when the `kubernetes` way logs in |
| `consul.auth.fallback-recheck-interval` | `CONSUL_AUTH_FALLBACK_RECHECK_INTERVAL` | duration                      | `5h`                           | same, and only in the fallback mode               |

In `kubernetes-with-m2m-fallback` mode the `kubernetes` way is tried first. If it fails, the pod falls back to the
`m2m` way and logs the reason, the Consul response code, and a truncated response body in a single `INFO` record. That
record marks the decision, so it appears once rather than on every retry; each login attempt logs an `INFO` record of
its own, naming the auth method it went to. In `kubernetes` mode there is no probe and no fallback, and neither
`M2MManager` nor the namespace is needed. In `m2m` mode the auth method name and the audience are not used at all.

The fallback is temporary. Once `fallback-recheck-interval` has passed, the next scheduled relogin tries the
`kubernetes` way again, and the first success switches the pod over for good, with a second `INFO` record. Going back
to `m2m` never happens. The choice survives the two phases of the startup: the `TokenStorage` bean reads the auth
method of the token the ConfigData phase already obtained, from `/v1/acl/token/self`, and picks up from there instead
of probing again. The recheck rides on the scheduled relogin instead of a timer of its own, so it needs
`MaxTokenTTL` on the auth method: without it Consul issues a token that never expires, nothing is scheduled, and
nothing is rechecked.

`fallback-recheck-interval` therefore sets the lower bound on how often the pod retries, not the actual period. The
relogin runs at 80% of `MaxTokenTTL`, and the recheck waits for the first relogin past the interval, so with a
`MaxTokenTTL` of 24 hours a pod retries about every 19 hours whatever the interval says. Plan the migration of a fleet
against `MaxTokenTTL`, and lower it on the auth method if the pods have to move over sooner.

An unknown `consul.auth.mode`, or no namespace in the `m2m` and `kubernetes-with-m2m-fallback`
modes (`NAMESPACE` or `cloud.microservice.namespace`), ends the startup before the first login attempt. A failed login
does not: the ConfigData phase logs one `ERROR` record and the application starts without an ACL token, so Consul reads
fail until the `TokenStorage` bean obtains one. The bean is stricter — a login failure its retries do not fix ends the
startup.

The default is the auth method the platform registers. Set `consul.auth.method` only if your Consul names it
differently.

To turn the ACL token exchange off altogether — for local runs and tests — set
`spring.cloud.consul.config.m2m.enabled=false`. None of the login properties are read then.

## Reading the projected token outside a pod

The `kubernetes` way reads the token from `/var/run/secrets/tokens/<audience>/token`. To run it outside a cluster,
point the token directory elsewhere:

```properties
com.netcracker.cloud.security.kubernetes.tokens.dir=/path/to/tokens
```
