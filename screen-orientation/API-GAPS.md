# Screen Orientation API — API gaps

Where the `com.vaadin.flow.component.screenorientation` API made a use case
in this module awkward. The API:

- `ScreenOrientation.orientationSignal()` returns
  `Signal<ScreenOrientationData>`, where `ScreenOrientationData` is a record
  `(ScreenOrientationType type, int angle)`; `ScreenOrientationType` has
  `isLandscape()` / `isPortrait()`.
- `ScreenOrientation.lock(ScreenOrientationType, onSuccess, onError)` and
  `ScreenOrientation.unlock()` / `unlock(SerializableRunnable onComplete)`.

## 1. No browserless simulator for the screen orientation

**Where it bites:** UC1 / `AdaptiveLayoutView.java`, UC2 /
`OrientationViewerView.java`, UC3 / `RotatePromptView.java` and their tests
**Symptom:** browserless-test has no simulator for
`ScreenOrientation.orientationSignal()`. Geolocation has
`GeolocationSimulator.current()`; screen orientation has nothing like it.
**Workaround used:** `ScreenOrientationTestSupport` (in `src/test/java`)
calls the public
`UIInternals#setScreenOrientationFromClient(String type, String angle)`.
This works without reflection, but it is the raw client-bridge entry point,
not a typed testing helper.
**Suggested API:** a `ScreenOrientationSimulator` next to
`GeolocationSimulator`, e.g.
`ScreenOrientationSimulator.current().setOrientation(ScreenOrientationType, int angle)`.
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
