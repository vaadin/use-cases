# Geolocation API — API gaps

Where the `Geolocation` API made a use case in this module awkward. The
browserless tests drive every view with `GeolocationSimulator` from
browserless-test, so there is no simulator gap.

## 1. A watch stays "active" after the permission is denied

**Where it bites:** UC2 / `TrackingView.java`, UC8 / `DbTrackingView.java`
**Symptom:** when the browser reports `PERMISSION_DENIED` to a running watch,
`GeolocationWatcher#positionSignal()` holds the error, but
`GeolocationWatcher#activeSignal()` stays `true`: only `stop()`, `resume()`
and a detach change it. The views keep showing "Stop tracking" for a watch
that will never deliver again. If the user then allows location again, the
browser does not restart the existing watch either; the Javadoc of
`Geolocation#watchPosition` asks the application to call `stop()` and
`resume()` itself, for example from an effect on
`Geolocation#availabilityHintSignal()`.
**Workaround used:** none. The status line shows the error, and the user has
to click Stop and Start again.
**Suggested API:** let `activeSignal()` turn `false` when the watch ends with
`PERMISSION_DENIED`, and let the watcher resume by itself when the
availability goes back to `GRANTED` (or offer that as an option).

## 2. A one-shot request is not tied to a component

**Where it bites:** UC3 / `AutoFetchView.java`, UC6 / `OptionsView.java`,
UC7 / `FormFieldView.java`
**Symptom:** `Geolocation#getPosition` takes two callbacks and returns
nothing. Unlike `watchPosition(Component)`, it has no owner: the request can
not be cancelled, and its callbacks still run after the user has left the
view (in UC6 an answer can take 10 s or more). There is also no signal for
the answer, while the watcher has `positionSignal()`, so UC7 copies the
position into its own `ValueSignal` to bind the form to it.
**Workaround used:** the callbacks write to components or to a view-owned
signal, and a late answer updates a view that is no longer shown.
**Suggested API:** an owner-based variant such as
`Geolocation.getPosition(Component owner, GeolocationOptions options)` that
returns a `Signal<GeolocationResult>` (starting as `GeolocationPending`) and
drops the answer when the owner is detached.
