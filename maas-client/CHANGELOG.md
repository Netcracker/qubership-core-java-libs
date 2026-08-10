# This page contains notably changes of maas-client project.

## Unreleased
* `Features`
  - HTTP calls to maas-agent are now retried on retryable status codes, not only on `IOException`.
    Retryable: 5xx, 429, **405** (only when the body carries a maas-service error) and **401**
    (once). See "Retry behaviour and configuration" in README for why the two 4xx codes are
    included — without them the client does not survive a Postgres leader switchover.
  - Backoff is exponential with jitter instead of a fixed 1s delay.
  - New configuration: `maas.http.retry.max-total-duration-ms` (`60s` by default) — a single
    setting bounding the whole call. The attempt count and the backoff growth are derived from
    it, so there are no separate knobs to keep consistent. Every attempt is bounded by what is
    left of it, so with the defaults the worst case a caller can see is ~60s, not 60s plus one
    `maas.http.timeout`.
  - The Kafka topic `watch-create` long poll no longer goes through the retry policy
    (`HttpExecution.noRetry()`); its own loop got a linear capped backoff instead, so a
    down maas-agent is no longer polled in a hot loop.
* `Behaviour changes`
  - **A call that fails with a retryable status now takes longer before failing.** Previously an
    unexpected 5xx/405/401 threw immediately; it is now retried within the configured limits.
  - Interrupting a thread during a retry wait now restores the interrupt flag and aborts the loop,
    instead of swallowing `InterruptedException`.
  - `KafkaMaaSClient.watchTopicCreate` throws `IllegalStateException` after `close()`, instead of
    registering a callback that can never fire.
  - Failed calls to maas-agent now throw `MaaSHttpException` instead of a bare `RuntimeException`.
    It extends `MaaSException`, which is a `RuntimeException`, so existing `catch` blocks keep working.

## 10.0.0
* `Features`
  - **Breaking:** Removed _MaaSAPIClient.loadConfiguration_ from public API.
