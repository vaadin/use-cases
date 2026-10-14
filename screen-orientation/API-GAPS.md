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

## Resolved: `UNKNOWN` in `onAttach` and feature detection

**Where it bit us:** uc1 / `AdaptiveLayoutView`, uc2 / `OrientationViewerView`,
uc3 / `RotatePromptView`, uc4 / `LockForVideoView`.
**Original concern:** views attached during the first request might read
`ScreenOrientationType.UNKNOWN` before the client reported its orientation,
and there was no synchronous way to ask whether the browser implements the
Screen Orientation API at all.
**Resolution:** neither is a gap. The browser sends its orientation with the
bootstrap (`v-r=init`) request, and Flow seeds `orientationSignal()` from it
before the router creates the first view, so in a real browser `onAttach`
already sees the real type, or `UNSUPPORTED` when the API is missing. The
synchronous feature-detect boolean also exists:
`ExtendedClientDetails#isScreenOrientationSupported()` (@since 25.2), which
mirrors the seeded signal. `UNKNOWN` is only seen when no bootstrap request
ran — in practice, browserless tests that don't seed the signal through
`ScreenOrientationTestSupport` — which is why the views keep an `UNKNOWN`
branch.
