# Consul ACL token refresh: the contract

Issue: [Netcracker/qubership-core-java-libs#194](https://github.com/Netcracker/qubership-core-java-libs/issues/194)

This is the language-neutral contract behind the Java implementation. The Go property source in
`qubership-core-lib-go-rest-utils/consul-propertysource` carries the same defect and implements the same contract; this
page exists so that it does not have to be redesigned from the symptom.

## Problem

When the ACL state of Consul is reinitialized, every token a running pod holds stops resolving, and Consul answers
`403` with `ACL not found`. The refresh is scheduled from `ExpirationTime` alone, so an invalidated token whose
expiration is still in the future triggers nothing, and the pod keeps sending a dead token until it restarts. Where
the auth method carries no `MaxTokenTTL`, Consul omits `ExpirationTime` and no refresh is scheduled at all.

The delivery path already works: every consumer reads the token on each call. What is missing is the detection.

## Roles

Three, and keeping them apart is the design.

| Role | Does | Must not |
| --- | --- | --- |
| **Token provider** | Performs one login | Know that refusals exist |
| **Token source** | Owns the current token; decides when to log in again | Send requests of its own beyond the token reads below |
| **Transport** | Attaches the token, reads the response status, reports a `403` | Decide what a `403` means |

## The four decisions

### 1. The report carries no token and no diagnosis

A transport that reads `403` calls one no-argument method on the token source and passes the response through
unchanged. It does not parse the body, does not classify the failure, and does not retry.

This is what keeps the change small. A report that carried the refused token would have to be threaded through every
layer between the place that attaches the token and the place that reads the status.

### 2. The source verifies before it acts

The report is a trigger to check, not an instruction to log in. On a report, the source reads `/v1/acl/token/self`
with the token it holds. `403` means Consul no longer resolves the token, and a login follows. `200` means the token
is live and the report changes nothing.

The reason is that `403` covers two different situations. Consul answers it for `ACL not found`, where the token is
gone, and for `Permission denied: token with AccessorID ... lacks permission ...`, where the token is live but its
policy is short of a privilege. A login returns a token with the same policy, so acting on the second situation
produces a login loop that never converges: a misconfigured policy would turn a flood of KV requests into a flood of
logins. `/v1/acl/token/self` resolves for any token Consul still holds, whatever its policy, so one read separates the
two cases. Verifying once in the source is cheaper and safer than classifying independently in every transport.

Passing the refused token so that a stale report can be skipped is an optimization, not a requirement. Single-flight
already bounds the stampede, and the cost of not skipping is one extra self-read.

### 3. One login at a time, throttled and jittered

- **Single-flight.** Reports that arrive while a check or a login is under way are dropped, not queued.
- **Minimum interval** between two forced logins, so a consumer looping on `403` cannot drive a login loop.
- **Random delay** inside a fixed window before a forced login runs, so that a fleet meeting the same refusal at the
  same moment does not reach `/v1/acl/login` together.

Only a refusal of the token triggers a login. A `5xx`, a `429`, or a transport failure means Consul is unreachable,
not that the token is dead, and triggers nothing.

### 4. A backstop on a timer

The source reads `/v1/acl/token/self` on a schedule and takes the same path on `403`. The backstop covers what no
transport can report: a pod that reads nothing from Consul during the reinitialization, an auth method without
`MaxTokenTTL`, and any consumer outside the library.

The backstop runs whether or not the token carries an expiration. That is what closes the gap where nothing is
scheduled today.

The two mechanisms differ in latency, not in effect: the reported path reacts within one failed request, the backstop
within one interval.

## What a stack implements

1. One seam per HTTP client that talks to Consul: on `403`, report, then pass the response through unchanged.
2. A token source with two operations, `get` and `report a refusal`, carrying decisions 2 and 3.
3. The existing expiration-driven refresh, unchanged.
4. The backstop of decision 4, with its interval configurable and `0` disabling it.

Consumers receive the token source as one object. A pair of a getter and a callback lets the two be wired to different
owners and is not the shape to copy.

## Open decision

The backstop interval. `5m` assumes the reported path carries detection and the backstop covers the residue. A shorter
interval, `30s` to `60s`, would recover a pod fast enough on its own and would let the reported path be implemented
only where the flood happens. Decide before the Go port starts, because it sets that port's scope.

## Vocabulary

One word throughout, in identifiers and in prose: Consul **refuses** a token, and a transport reports a **refusal**.
The Java code has used `reject`, `invalidate`, and `refuse` for the same thing; a reader who greps for one finds a
third of the sites.
