# Screen Orientation API — gaps surfaced while building use cases

Rough edges hit while building the five use cases on the
`com.vaadin.flow.component.screenorientation` API. Each entry names the use
case where it bit us, the symptom, the workaround, and a suggested shape for
the real fix. The API:

- `ScreenOrientation.orientationSignal()` returns
  `Signal<ScreenOrientationData>`, where `ScreenOrientationData` is a record
  `(ScreenOrientationType type, int angle)`; `ScreenOrientationType` has
  `isLandscape()` / `isPortrait()`.
- `ScreenOrientation.lock(ScreenOrientationType, onSuccess, onError)` and
  `ScreenOrientation.unlock()` / `unlock(SerializableRunnable onComplete)`.

## No `ScreenOrientationSimulator` for tests

**Where it bit us:** uc1 / `AdaptiveLayoutView`, uc2 / `OrientationViewerView`,
uc3 / `RotatePromptView` (and their tests).
**Symptom:** There is no dedicated, discoverable simulator for driving
`ScreenOrientation.orientationSignal()` in tests. The geolocation feature
exposes `GeolocationSimulator.current()` for this purpose; the
screen-orientation API has nothing equivalent.
**Workaround used:** `ScreenOrientationTestSupport` (in `src/test/java`) drives
the signal through the public
`UI.getCurrent().getInternals().setScreenOrientationFromClient(String type,
String angle)` — no reflection needed. That public seam works, but is the
raw JS-bridge entry point rather than a purpose-built testing helper.
**Suggested API:** Ship a `ScreenOrientationSimulator` alongside
`GeolocationSimulator` (`ScreenOrientationSimulator.current().setOrientation(
ScreenOrientationType, int angle)`) so tests have a discoverable, typed seam
rather than reaching into `UIInternals#setScreenOrientationFromClient`.

## No `Signal<Boolean>` for `isLandscape` / `isPortrait`

**Where it bit us:** uc1 / `AdaptiveLayoutView`, uc3 / `RotatePromptView`.
**Symptom:** `ScreenOrientationType#isLandscape()` and `#isPortrait()` exist on
the enum but the natural binding shape we want is a `Signal<Boolean>` over
"is the device currently in landscape?". The consumer has to do
`ScreenOrientation.orientationSignal().map(d -> d.type().isLandscape())`
everywhere.
**Workaround used:** A `.map(...)` in every consumer. Tolerable, but boilerplate.
**Suggested API:** Convenience derived signals —
`ScreenOrientation.isLandscapeSignal()` and
`ScreenOrientation.isPortraitSignal()` — both mapping to `false` when the type
is `UNKNOWN` or `UNSUPPORTED`, matching the existing enum behaviour.

## Pre-bootstrap `UNKNOWN` is briefly observable in `onAttach`

**Where it bit us:** uc1 / `AdaptiveLayoutView`, uc2 / `OrientationViewerView`,
uc3 / `RotatePromptView`.
**Symptom:** Views attached during the very first request still see
`ScreenOrientationType.UNKNOWN` on the very first signal read because the
client-side bootstrap parameter has not been processed yet when `onAttach`
runs in some test/dev scenarios. Each view ends up needing an explicit
`UNKNOWN` branch in its mapping logic just to render something sensible
for that one tick.
**Workaround used:** Every view enumerates the `UNKNOWN` branch in its
switch — usually as "treat like UNSUPPORTED" or "treat like portrait".
**Suggested API:** Either (a) document a recommended default mapping, or
(b) consider a `orientationSignalOrDefault(ScreenOrientationType
fallback)` accessor that swaps `UNKNOWN` for an application-provided
fallback before the value reaches consumers.

## No way to feature-detect screen-orientation support from the server before bootstrap

**Where it bit us:** uc3 / `RotatePromptView`, uc4 / `LockForVideoView`.
**Symptom:** To decide whether to show a "rotate your device" hint or a
"lock orientation" button, you want a synchronous answer to "does this
browser implement the Screen Orientation API?". You must wait for the
signal to settle on `UNSUPPORTED` — distinct from `UNKNOWN` but still
asynchronous.
**Workaround used:** Treat `UNSUPPORTED` as feature-detect, but accept a
brief window where the UI offers a control that will fail on click.
**Suggested API:** Add `ExtendedClientDetails#supportsScreenOrientation()` (the
bootstrap already carries enough information to fill it in synchronously).
That mirrors how `ExtendedClientDetails` already exposes other
"feature-detect" booleans.
