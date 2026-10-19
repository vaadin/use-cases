# Fullscreen API — API gaps

Where the `com.vaadin.flow.component.fullscreen` API made a use case in this
module awkward. The API is a small static facade:

- `Fullscreen.onClick(button).enter()` / `.enter(component)` binds a
  fullscreen request to a click (the browser only allows the request during a
  user gesture). The `enter(..., onSuccess, onError)` form reports the outcome.
- `Fullscreen.exit()` leaves fullscreen (no gesture needed).
- `Fullscreen.stateSignal()` is one global `Signal<FullscreenState>`
  (`UNKNOWN`, `UNSUPPORTED`, `NOT_FULLSCREEN`, `FULLSCREEN`).

## 1. `UNSUPPORTED` mixes "not supported" with "not permitted"

**Where it bites:** no use case yet. It hits any app that runs in an iframe
without the `fullscreen` permissions policy (`allow="fullscreen"`).
**Symptom:** `FullscreenState.UNSUPPORTED` means both "the browser can not go
fullscreen" and "this page is not allowed to go fullscreen". The app can not
choose the right message: "Your browser does not support fullscreen" or "This
page is not allowed to go fullscreen — check the iframe `allow` attribute".
The `onError` callback of `FullscreenBinding.enter(..., onSuccess, onError)`
gets `NotAllowedError`, but only after a failed request. Nothing tells the app
up front whether fullscreen is allowed.
**Workaround used:** none. Both cases are treated as `UNSUPPORTED`.
**Suggested API:** a separate value in `FullscreenState` (for example
`NOT_PERMITTED`), or a permission signal next to `stateSignal()`.

## 2. No per-request handle: which component is fullscreen, and why it exited

**Where it bites:** UC5 / `KioskExitDetectionView.java`, UC6 /
`ChartExpandView.java`
**Symptom:** `Fullscreen.stateSignal()` holds only the global state.
`FullscreenBinding.enter(Component)` returns nothing, so the app can not ask
which component is fullscreen now. The state change does not say whether the
exit came from `Fullscreen.exit()` or from the user (Escape).
**Workaround used:** UC6 keeps its own `activeOwner` signal: it is set on the
Expand click and cleared when the state leaves `FULLSCREEN`. UC5 sets an
`expectingExit` flag right before `Fullscreen.exit()` and reads it on the next
`FULLSCREEN` → `NOT_FULLSCREEN` change, to tell a staff PIN exit from Escape.
**Suggested API:** a handle returned from `enter(...)` with `owner()` and an
exit cause, or on the facade a signal of the fullscreen component and an exit
cause on the state change.

## 3. Browserless tests can not resolve or reject a request

**Where it bites:** every test of the module / `FullscreenTestSupport.java`
**Symptom:** the tests set the state with
`Fullscreen.setStateFromClient(ui, state.name())`, wrapped by
`FullscreenTestSupport`. That moves the global signal, but a test can not make
a single request succeed or fail. So the `onSuccess` / `onError` callbacks of
`FullscreenBinding.enter(..., onSuccess, onError)`, for example the
"Request REJECTED" log line in UC5, are not tested.
**Workaround used:** the tests check only the UI that follows the signal. The
request callbacks are left to manual testing in a browser.
**Suggested API:** a browserless fullscreen simulator that can resolve or
reject the last request, next to a way to set the state.
Upstream: https://github.com/vaadin/browserless-test/issues/166
