# API gaps surfaced by the Fullscreen use cases

Rough edges hit while building the use cases on the
`com.vaadin.flow.component.fullscreen` API. The API is a small static facade:

- `Fullscreen.onClick(button).enter()` / `.enter(component)` binds a
  fullscreen request to a click trigger (the browser requires transient user
  activation, so the request only fires inside the DOM click event); an
  observed `enter(..., onSuccess, onError)` form reports the outcome.
- `Fullscreen.exit()` leaves fullscreen (no gesture required).
- `Fullscreen.stateSignal()` exposes a single global `Signal<FullscreenState>`
  (`UNKNOWN`, `UNSUPPORTED`, `NOT_FULLSCREEN`, `FULLSCREEN`).

## `FullscreenState.UNSUPPORTED` conflates "unsupported" with "not permitted"

**Where it bit us:** no specific UC — a latent gap that hits embedded
iframes (e.g. a Flow app inside a host page that didn't grant the
`fullscreen` permissions policy). `FullscreenState.UNSUPPORTED` conflates
"browser doesn't support fullscreen at all" with "this document is not
permitted to enter fullscreen". The distinction matters for the message we
show the user: "Your browser doesn't support fullscreen" vs "This page isn't
allowed to enter fullscreen — check the iframe `allow` attribute".
**Workaround used:** treat both as `UNSUPPORTED`.
**Suggested API:** either a finer-grained `FullscreenState`
(`UNSUPPORTED`, `PERMISSION_DENIED`) or an additional permission signal
mirroring how the Permissions API exposes other capabilities. The observed
`enter(..., onSuccess, onError)` form surfaces the browser error (e.g.
`NotAllowedError`) for the *request* path — but the steady-state "is it
allowed?" question has no answer.

## No per-request handle: which component is fullscreen, and why it exited

**Where it bit us:** uc6 / `ChartExpandView`, uc5 / `KioskExitDetectionView`.
**Symptom:** the API exposes only the global `stateSignal()`. There is no
per-request handle, so nothing reports *which* component is currently
fullscreen, nor whether a given exit was programmatic (`Fullscreen.exit()`)
or user-initiated (Escape). Both UCs re-derive this in app code: UC6 mirrors
the active card into its own `ValueSignal<Optional<Component>>` on the button
click and clears it when the signal leaves `FULLSCREEN`; UC5 sets an
`expectingExit` flag right before `Fullscreen.exit()` and reads it on the
next `FULLSCREEN → NOT_FULLSCREEN` transition to tell a staff-PIN exit apart
from an Escape press.
**Workaround used:** the app-side `activeOwner` mirror (UC6) and
`expectingExit` flag (UC5) described above.
**Suggested API:** either a handle returned from `enter(...)` exposing
`owner()` and an exit cause, or convenience accessors on the facade
(an "active component" signal, and an exit-cause carried on the state
transition) so applications don't each re-implement this.

## Testing drives the global signal but not a request lifecycle

**Where it bit us:** every `uc*ViewTest`.
**Symptom:** browserless tests seed the state with
`Fullscreen.setStateFromClient(ui, state.name())` (wrapped by this module's
`FullscreenTestSupport`). That covers the global signal, but there is no way
to simulate an individual *request* succeeding or being rejected, so the
observed `enter(..., onSuccess, onError)` callbacks — e.g. UC5's
"Request REJECTED" log branch — cannot be exercised browserless.
**Workaround used:** assert only the signal-driven UI; the request callbacks
are left to manual/browser testing.
**Suggested API:** a test simulator that can also resolve or reject the most
recent request, alongside the global-state seam that already exists.
