# Wake Lock API — API gaps

Where the static `com.vaadin.flow.component.wakelock.WakeLock` API made a use
case in this module awkward. The API has `request()`,
`request(SerializableConsumer<WakeLockError>)`, `release()`, `activeSignal()`
and `availabilitySignal()`. Nothing is blocked; the entries below are places
where a use case needed a workaround or where the API is thinner than the other
page-level APIs in Flow.

## 1. No browserless simulator for the wake lock

**Where it bites:** every test of the module / `WakeLockTestSupport.java`
**Symptom:** browserless-test has no `WakeLockSimulator`. A test can not make
the browser "grant" or "release" the lock, and has no API to check that a
click sent `WakeLock#request()` or `release()` to the client.
**Workaround used:** `WakeLockTestSupport.answerWakeLockCalls()` reads the
pending JavaScript queue (`UIInternals#dumpPendingJavaScriptInvocations()`)
for the `WakeLockJs` `request`/`release` calls, and answers them through the
public but framework-internal `UIInternals#setWakeLockActive(boolean)`
(`setWakeLockAvailability` for the availability).
**Suggested API:** a `WakeLockSimulator` like `GeolocationSimulator`, with
`simulateAcquired()`, `simulateReleased()` and an `isLockRequested()` check.
Upstream: https://github.com/vaadin/browserless-test/issues/166

## 2. No success result from `request()`; `release()` gives no result

**Where it bites:** UC1 / `ManualToggleView.java`, UC3 / `SlideshowView.java`,
UC4 / `WorkoutTimerView.java`
**Symptom:** `WakeLock#request(SerializableConsumer<WakeLockError>)` reports
only a failure (`WakeLockErrorCode` `UNSUPPORTED`, `NOT_ALLOWED`, `UNKNOWN`).
There is no success result, and `WakeLock#release()` has no callback or future
at all. To show "acquired" vs. "denied", a view has to combine
`activeSignal()` with the error callback instead of waiting for one result.
**Workaround used:** the views do not pass an error callback; they show
`activeSignal()` (UC1 and UC2 also `availabilitySignal()`). In UC1 and UC2,
"Requested — waiting for the browser" also covers "we asked, but the browser
refused"; in UC3 and UC4, "Released" does.
**Suggested API:** a `request()` overload that returns
`CompletableFuture<Boolean>` (true = acquired, false = denied), and a callback
or future on `release()`.

## 3. No reason when the browser drops the lock

**Where it bites:** UC3 / `SlideshowView.java`, UC4 / `WorkoutTimerView.java`
**Symptom:** when the tab is hidden, the browser drops the lock, and the client
gets it again when the tab is visible. `activeSignal()` goes false, true,
false, true. The view can not tell "user released it", "low battery" and "tab
hidden" apart without also reading `pageVisibilitySignal()`. `WakeLockError`
covers only failed requests; a release the app did not ask for has no reason.
An app that wants to stop after N releases can not do it.
**Workaround used:** none. UC3 and UC4 rely on the client (`WakeLock.ts`)
getting the lock again when the tab is visible.
**Suggested API:** a `WakeLockReleaseReason` enum (`USER_RELEASE`,
`VISIBILITY`, `BATTERY_SAVER`, `OS_LOCK`, …) sent with the
`vaadin-wake-lock-change` event and exposed as `releaseReasonSignal()` on
`WakeLock`.

## 4. The client state is shared by all UIs on the page

**Where it bites:** not hit yet. UC2 / `RecipeView.java` would hit it in a page
with more than one UI.
**Symptom:** in `flow-client/src/main/frontend/WakeLock.ts`, `wanted`,
`sentinel` and `visibilityListenerInstalled` are module-level variables. A page
can have more than one UI (for example, two embedded applications). Calling
`WakeLock#release(UI)` for one UI drops the lock for the others too.
**Workaround used:** none needed; each use case has one UI per tab.
**Suggested API:** keep `wanted` and `sentinel` per UI element (the element
passed to `request(element)`), not per module.

## 5. Only the screen lock type

**Where it bites:** considered for UC4 / `WorkoutTimerView.java`
**Symptom:** `WakeLock#request()` always asks for `wakeLock.request('screen')`.
The spec leaves room for other types (such as `'system'`, to keep the CPU
awake), and a timer would use one if browsers add it.
**Workaround used:** none; UC4 holds the screen lock.
**Suggested API:** a `WakeLock#request(WakeLockType)` overload with an enum
(today only `SCREEN`).

## 6. The lock belongs to the tab, not to a view

**Where it bites:** UC1 / `ManualToggleView.java`, UC2 / `RecipeView.java`,
UC3 / `SlideshowView.java`, UC4 / `WorkoutTimerView.java`
**Symptom:** `WakeLock#request()` takes a `UI`, not a component, and the lock
stays requested until someone calls `WakeLock#release()`. A view that forgets
to release when the user navigates away keeps the screen on in every other
view of the tab. There is no owner-scoped form like `Signal.effect(owner, …)`.
**Workaround used:** every view releases the lock in a `whenAttached` cleanup.
**Suggested API:** `WakeLock.request(Component owner)` that holds the lock
while the owner is attached and releases it on detach.

## 7. The server can not see whether the lock is requested

**Where it bites:** UC1 / `ManualToggleView.java`
**Symptom:** the "lock wanted" flag lives only in the browser (`wanted` in
`flow-client`'s `WakeLock.ts`). `activeSignal()` is `false` both before a
request and while a request waits for the browser (or for the tab to be
visible again), so a toggle bound to it offers "Keep screen awake" again
while the request is still pending.
**Workaround used:** UC1 keeps its own `wanted` `ValueSignal`, sets it on
every `request()`/`release()`, and binds the button to it.
**Suggested API:** a read-only `WakeLock#requestedSignal()` next to
`activeSignal()`.
