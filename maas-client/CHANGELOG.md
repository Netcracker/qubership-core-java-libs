# This page contains notably changes of maas-client project.

## Unreleased
* `Features`
  - Calls to maas-agent survive a database leader switchover. Retryable: `IOException`, 5xx, 429,
    and 405 when the body carries a maas-service error — maas-service reports a read-only database
    that way, so the usual "fail fast on 4xx" rule does not hold here. 401 is not retried, because
    the token source refreshes on its own schedule. See "Retry behaviour and configuration" in
    README.
  - New configuration: `maas.http.retry.max-total-duration-ms`, 60s by default, bounding a whole
    call including retries. Attempt count and backoff growth derive from it, and each attempt is
    capped by what is left, so the worst case a caller sees is that duration rather than the
    duration plus one `maas.http.timeout`. `0` disables retries; an unreadable or negative value
    warns and falls back to the default.
* `Behaviour changes`
  - **A call that fails with a retryable status now takes longer before failing.** It used to throw
    on the first unexpected 5xx or 405; it is now retried within the configured duration.
  - Failed calls to maas-agent throw `MaaSHttpException` instead of a bare `RuntimeException`. It
    extends `MaaSException`, so existing `catch` blocks keep working — note the widening:
    `catch (MaaSException)` used to mean a MaaS business error and now also catches transport
    failures.
  - `deleteTopic` and `getOrCreateTopic` with `OnTopicExists.FAIL` are not retried: neither is
    idempotent, and a lost response after the server completed the operation would make the retry
    report a failure that did not happen.
  - The Kafka `watch-create` long poll is paced. A down maas-agent used to be re-polled as fast as
    the socket could refuse the connection; the poll now backs off exponentially up to 30s with
    jitter, so instances that lose the same agent do not all return at the same moment. It keeps
    retrying for the life of the client.
  - The watch poll window derives from `maas.http.timeout`, 25s with the defaults, instead of a
    fixed 60s that outlasted the read timeout and made every quiet poll fail locally. Tuning
    `maas.http.timeout` now moves it; below 2s the watch is not usable.
  - `KafkaMaaSClient.watchTopicCreate` throws `IllegalStateException` after `close()`, and after the
    watch thread has stopped on its own, instead of registering a callback that can never fire.
  - Interrupting a thread during a retry wait restores the interrupt flag and aborts, instead of
    swallowing `InterruptedException`.
* `Fixed`
  - `deleteTopic` threw `NullPointerException` and `search` threw `NoSuchElementException` when
    maas-agent answered 200 with an empty body. They now report nothing deleted and no topics found.
  - A response without a body no longer throws while the client reads it, in `getOrCreateTopic`,
    `deleteTopic` and the error paths.
* `Dependencies`
  - New runtime dependency `dev.failsafe:failsafe`, which carries the retry policies. It has no
    transitive dependencies, but services with dependency convergence rules will see it appear.

## 10.0.0
* `Features`
  - **Breaking:** Removed _MaaSAPIClient.loadConfiguration_ from public API.
