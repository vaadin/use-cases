# Wake Lock API — gaps observed while building the use cases

Friction points hit while building the four use cases on the static
`com.vaadin.flow.component.wakelock.WakeLock` API: `request()` /
`request(SerializableConsumer<WakeLockError>)` / `release()`,
`activeSignal()` → `Signal<Boolean>`, and `availabilitySignal()` →
`Signal<WakeLockAvailability>` (`SUPPORTED / UNSUPPORTED / UNKNOWN`).

Nothing is *blocked* by missing API surface. The notes below are places where
the use cases needed a workaround or where the surface feels thinner than the
rest of the page-level facades in Flow.

## No test simulator / no way to drive the signal from a browserless test

**Where it bit us:** all four use cases, via
`wake-lock/src/test/java/com/example/WakeLockTestSupport.java`.
**Symptom:** no dedicated `WakeLockSimulator` ships. A browserless
test that wants to verify a status badge transitions to "Holding lock"
has no purpose-built helper to simulate the browser confirming the
lock without going through the client.
**Workaround used:** the test drives the signals through the **public**
`UI.getCurrent().getInternals().setWakeLockActive(boolean)` (and
`setWakeLockAvailability(WakeLockAvailability)`) on `UIInternals`;
`WakeLockTestSupport.simulateAcquired() / simulateReleased()` wrap
those. That public seam works, but is a framework-internal entry point
rather than a purpose-built testing helper. Mirrors
`PageVisibilityTestSupport` in the page-visibility module.
**Suggested API:** a `WakeLockSimulator` analogous to
`GeolocationSimulator` in flow-server-test — `simulateAcquired()`,
`simulateReleased()`, plus an `isLockRequested()` predicate so a test
can also verify that the server-side `request()` call reached the
client. Without an `isLockRequested()` hook, UC1's "click invokes
wakeLock" assertion has to settle for "after clicking, the next
simulated ACTIVE event is reflected on screen" — a cause-and-effect
chain, not a direct observation.

## No success result from `request()`; `release()` is fire-and-forget

**Where it bit us:** UC1, UC3 (slideshow), UC4 (workout timer).
**Symptom:** `request(onError)` reports a failure via `WakeLockError`
(`WakeLockErrorCode` `UNSUPPORTED / NOT_ALLOWED / UNKNOWN`), but there is
no success result — no `CompletableFuture<Boolean>`-style "true =
acquired" return — and `release()` is fully fire-and-forget (no callback,
no future, no confirmation that the release landed). A caller wanting to
key UI off *acquired vs. denied* has to observe `activeSignal()`
alongside the error callback rather than awaiting a single result.
**Workaround used:** views observe `activeSignal()` for the success
path and the `onError` callback for the failure path; "Released" is the
catch-all for "we asked but nothing came back".
**Suggested API:** a `request()` overload returning
`CompletableFuture<Boolean>` (true = acquired, false = denied), and a
completion callback / future on `release()` so callers can confirm it.

## No reason exposed when the browser drops the lock

**Where it bit us:** UC3 and UC4.
**Symptom:** when the tab hides, the browser drops the lock and the
client transparently re-acquires it on return. That's exactly what
UC3 / UC4 want — but the signal flips false-true-false-true and the
view has no way to distinguish "user revoked", "low battery", and
"transient hide" without correlating with `pageVisibilitySignal()`.
`WakeLockError` covers request-time failures, but an unsolicited release
carries no reason. Apps that want to give up after N revocations cannot.
**Workaround used:** UC3 / UC4 simply re-issue `request()` from a
Signal effect; the client's built-in auto-reacquire makes that mostly
redundant but harmless.
**Suggested API:** a `WakeLockReleaseReason` enum carried on the
`vaadin-wake-lock-change` event (`USER_RELEASE`, `VISIBILITY`,
`BATTERY_SAVER`, `OS_LOCK`, …), and exposed as a paired
`releaseReasonSignal()` on the facade.

## Client `wanted` flag is a module-level singleton

**Where it bit us:** not yet — but UC2 (recipe) and UC5/dashboard-style
scenarios would have hit this if the user opens two tabs of the same
app.
**Symptom:** in `flow-client/src/main/frontend/WakeLock.ts` the
`wanted`, `sentinel`, and `visibilityListenerInstalled` variables are
module-globals. A page can have multiple UIs (popout windows, embedded
iframes) — calling `release()` on one would drop the lock for the
other.
**Workaround used:** none needed for these use cases (single UI per
tab).
**Suggested API:** scope `wanted` / `sentinel` by the element the
facade is bound to (the UI element passed into `request(element)`),
not module-wide. The event already carries `element` so the server
side could distinguish them.

## No "lock anything other than the screen"

**Where it bit us:** considered for UC4 (workout timer) — the spec
allows `wakeLock.request('system')` in some browsers to keep CPU
awake too. Worth noting if Flow ever exposes a richer set of lock types.
**Suggested API:** `WakeLock#request(WakeLockType)` overload with an
enum (today only `SCREEN`; later `SYSTEM` or others if the spec
expands).
