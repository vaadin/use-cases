# Screen Orientation API — use cases

A standalone Spring Boot demo of the Screen Orientation API in Vaadin Flow:
`ScreenOrientation.orientationSignal()` to follow the device's orientation and
angle, and `ScreenOrientation.lock(...)` / `unlock()` to pin the screen to one
orientation. Each view shows a single realistic scenario. Where the API falls
short is recorded in [API-GAPS.md](API-GAPS.md).

| # | View | What it shows |
| - | ---- | ------------- |
| UC1 | Adaptive layout | Two panes sit side by side in landscape and stack in portrait, switching as soon as the device rotates. |
| UC2 | Orientation viewer | Everything the orientation signal reports — type, angle, `isLandscape()`, `isPortrait()` and whether the browser supports the API — with an arrow that follows the angle. |
| UC3 | Rotate prompt | An overlay asks the user to rotate the device until it matches the orientation the content needs. Browsers without the API are never blocked. |
| UC4 | Lock for video | "Play" enters fullscreen and locks the screen to landscape; "Stop" releases it. The lock state also follows when the user leaves fullscreen with Esc. |
| UC5 | Lock error UX | Lock requests that fail in different ways, and the message a view picks for each `ScreenOrientationLockErrorCode`. |

Rotating needs a phone or tablet, or the device emulation of the browser's
developer tools. Locking only works on a phone or tablet, in fullscreen;
desktop browsers reject it.

## Run

```
./mvnw -pl common install -DskipTests
./mvnw -pl screen-orientation spring-boot:run
```

Open <http://localhost:8080/>.
