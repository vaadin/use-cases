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
the browser "grant" or "release" the lock, and can not check that `request()`
reached the client. UC1's test can only check that a simulated `ACTIVE` state
shows on screen, not that the click sent a request.
**Workaround used:** `WakeLockTestSupport.simulateAcquired()` /
`simulateReleased()` call the public but framework-internal
`UIInternals#setWakeLockActive(boolean)` (and `setWakeLockAvailability`).
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
**Workaround used:** the views show only `activeSignal()`. "Released" also
covers "we asked, but the lock never came".
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
