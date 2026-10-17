# Screen Orientation API — API gaps

Where the `com.vaadin.flow.component.screenorientation` API made a use case
in this module awkward. The API:

- `ScreenOrientation.orientationSignal()` returns
  `Signal<ScreenOrientationData>`, where `ScreenOrientationData` is a record
  `(ScreenOrientationType type, int angle)`; `ScreenOrientationType` has
  `isLandscape()` / `isPortrait()`.
- `ScreenOrientation.lock(ScreenOrientationType, onSuccess, onError)` and
  `ScreenOrientation.unlock()` / `unlock(SerializableRunnable onComplete)`.
  The error is a `ScreenOrientationLockError` with a
  `ScreenOrientationLockErrorCode` (`NOT_SUPPORTED`, `SECURITY`, `ABORT`,
  `UNKNOWN`).

## 1. No browserless simulator for the screen orientation

**Where it bites:** the tests of every view, `ScreenOrientationTestSupport.java`
**Symptom:** browserless-test has no simulator for
`ScreenOrientation.orientationSignal()` or for the answer to
`ScreenOrientation.lock(...)`. Geolocation has `GeolocationSimulator`; screen
orientation has nothing like it. Without help, a lock request in a test never
gets an answer, so neither callback runs.
**Workaround used:** `ScreenOrientationTestSupport` (in `src/test/java`)
calls the public
`UIInternals#setScreenOrientationFromClient(String type, String angle)` to
set the orientation. To answer a lock, it empties the pending JavaScript
queue with `UIInternals#dumpPendingJavaScriptInvocations()`, picks the
`ScreenOrientationJs.lock` calls and completes them with the JSON the client
bridge would send. Both are raw internals, not typed testing helpers, and
emptying the queue drops any other pending JavaScript.
**Suggested API:** a `ScreenOrientationSimulator` next to
`GeolocationSimulator`, e.g.
`ScreenOrientationSimulator.current().setOrientation(ScreenOrientationType, int angle)`
and `pendingLocks()` returning requests a test can `succeed()` or
`fail(ScreenOrientationLockErrorCode)`.
Upstream: https://github.com/vaadin/browserless-test/issues/166

## 2. No `Signal<Boolean>` for landscape / portrait

**Where it bites:** UC1 / `AdaptiveLayoutView.java`, UC3 /
`RotatePromptView.java`
**Symptom:** `ScreenOrientation` offers only `orientationSignal()`. To bind
to "is the device in landscape?", a view has to map it:
`orientationSignal().map(d -> d.type().isLandscape())`.
**Workaround used:** a `.map(...)` in every consumer. It works, but it is
boilerplate.
**Suggested API:** derived signals `ScreenOrientation.isLandscapeSignal()`
and `ScreenOrientation.isPortraitSignal()`. Both are `false` when the type is
`UNKNOWN` or `UNSUPPORTED`, like `ScreenOrientationType#isLandscape()` and
`#isPortrait()`.

## 3. The server is not told when a lock ends

**Where it bites:** UC4 / `LockForVideoView.java`
**Symptom:** `ScreenOrientation` has `lock(...)` and `unlock(...)`, but no
state. The browser drops the lock by itself when the page leaves fullscreen
(Esc, the back gesture), and the server is not told. A view that shows
"Locked" keeps showing it.
**Workaround used:** UC4 follows `Fullscreen.stateSignal()` and treats
leaving fullscreen as the end of the lock.
**Suggested API:** a read-only `ScreenOrientation.lockSignal()` (the locked
`ScreenOrientationType`, or empty when not locked) that the client updates
when the lock is applied, released or dropped.

## 4. `NOT_SUPPORTED` does not say whether fullscreen would help

**Where it bites:** UC4 / `LockForVideoView.java`, UC5 /
`LockErrorView.java`
**Symptom:** a lock outside fullscreen and a lock on a device that can never
lock (any desktop) both fail with `ScreenOrientationLockErrorCode.NOT_SUPPORTED`
(see its Javadoc). There is also no way to ask before trying. So a view can
not tell the user "enter fullscreen first" or hide the lock button on a
desktop; it can only give one vague message after the attempt.
**Workaround used:** UC5's message for `NOT_SUPPORTED` covers both cases
("Try fullscreen on a phone").
**Suggested API:** a separate error code for the missing fullscreen, and a
support signal (like `Fullscreen.stateSignal()` reports `UNSUPPORTED`) that
says whether this device can lock at all.
