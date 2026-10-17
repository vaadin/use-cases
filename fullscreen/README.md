# Fullscreen API — use cases

A standalone Spring Boot demo of the `com.vaadin.flow.component.fullscreen`
API in Vaadin Flow (resolved from the parent pom's `vaadin.version` property):
`Fullscreen.onClick(button).enter()` / `.enter(component)` to request
fullscreen from a user gesture, `Fullscreen.exit()` to leave, and
`Fullscreen.stateSignal()` to observe the current `FullscreenState`. Each view
exercises a single realistic scenario. Where the API falls short is recorded
in [API-GAPS.md](API-GAPS.md).

| # | View | What it shows |
| - | ---- | ------------- |
| UC1 | Image lightbox | Per-component fullscreen via `Fullscreen.onClick(thumb).enter(stage)`; thumbnails enlarge into a stage that fills the screen. |
| UC2 | Slideshow | Component fullscreen on the slide stage via `Fullscreen.onClick(present).enter(stage)`, with Previous/Next arrow-key shortcuts that keep working in fullscreen. |
| UC3 | Distraction-free editor | An editor pane expands via `Fullscreen.onClick(expand).enter(editorPane)`; the rest of the app is hidden, and Done exits with `Fullscreen.exit()`. |
| UC4 | Reactive layout | The whole page goes fullscreen and a six-card dashboard changes from two to three columns, purely through `bindClassName` bindings to `Fullscreen.stateSignal()` — no observer code. |
| UC5 | Kiosk with staff exit | A visitor sign-in kiosk that staff leave with a PIN (`Fullscreen.exit()`). An `expectingExit` flag tells that exit apart from Escape, because the state change does not say why fullscreen ended (`API-GAPS.md` #2). |
| UC6 | Chart expand | A dashboard of chart cards, each with its own Expand button that fullscreens just that card via `Fullscreen.onClick(expand).enter(card)`; the view tracks which card is fullscreen itself (`API-GAPS.md` #2). |
| UC7 | View app fullscreen | Whole-page fullscreen via `Fullscreen.onClick(enter).enter()` — kiosk-style "hide the browser chrome". |
| UC8 | Overlays in fullscreen | A fullscreen panel with a menu bar, popover, select, context menu and tooltip; every overlay still opens on top of the panel. |

## Run

```
./mvnw -pl common install -DskipTests
./mvnw -pl fullscreen spring-boot:run
```

Open <http://localhost:8080/>.

## Notes

- Browsers require **transient user activation** to enter fullscreen mode.
  A request is therefore *bound* to a click trigger with
  `Fullscreen.onClick(button).enter(...)` so it only fires inside the DOM
  click event. `Fullscreen.exit()` needs no gesture and is an ordinary click
  listener. Leaving fullscreen is observable via `Fullscreen.stateSignal()`
  (Escape, the browser's close gesture, or a superseding request all flip it).
- For component fullscreen, Flow moves the target into a Vaadin *wrapper*
  element, hides the rest of the app and fullscreens the whole document, so
  Vaadin theming and overlay components (Notification, ContextMenu, ComboBox
  dropdowns) keep working in fullscreen; the component is restored to its
  original DOM position on exit.
