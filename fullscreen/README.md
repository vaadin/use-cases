# Fullscreen API — use cases

A standalone Spring Boot demo of the `com.vaadin.flow.component.fullscreen`
API in Vaadin Flow 25.2: `Fullscreen.onClick(button).enter()` /
`.enter(component)` to request fullscreen from a user gesture,
`Fullscreen.exit()` to leave, and `Fullscreen.stateSignal()` to observe the
current `FullscreenState`. Each view exercises a single realistic scenario.

| # | View | What it shows |
| - | ---- | ------------- |
| UC1 | Image lightbox | Per-component fullscreen via `Fullscreen.onClick(thumb).enter(stage)`; thumbnails enlarge into a stage that fills the viewport. |
| UC2 | Slideshow / presentation | Component fullscreen on the slide stage via `Fullscreen.onClick(present).enter(stage)`, with prev/next shortcuts that keep working in fullscreen. |
| UC3 | Distraction-free editor | An editor pane expands via `Fullscreen.onClick(expand).enter(editorPane)`; the rest of the app chrome is hidden by the wrapper, and Done exits with `Fullscreen.exit()`. |
| UC4 | Reactive layout | A six-card dashboard reformats itself purely through `bindClassName` subscriptions to `Fullscreen.stateSignal()` — no observer code. |
| UC5 | Kiosk: detect unexpected exit | Differentiates Escape-initiated exits from programmatic ones by toggling an `expectingExit` flag around `Fullscreen.exit()`, reconstructing the distinction from the global state signal. |
| UC6 | Chart expand | A dashboard of chart cards, each with its own Expand button that fullscreens just that card via `Fullscreen.onClick(expand).enter(card)`. |
| UC7 | View app fullscreen | Whole-page fullscreen via `Fullscreen.onClick(enter).enter()` — kiosk-style "hide the browser chrome". |

## Run

```
cd fullscreen
mvn spring-boot:run
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
  element and hides the rest of the view, so Vaadin theming and overlay
  components (Notification, ContextMenu, ComboBox dropdowns) keep working in
  fullscreen; the component is restored to its original DOM position on exit.
- See `API-GAPS.md` for the rough edges this module surfaced.
