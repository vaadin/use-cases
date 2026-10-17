# Trigger / Action API — use cases

A standalone Spring Boot demo of Flow's trigger and action API in
`com.vaadin.flow.component.trigger.internal` (resolved from the parent pom's
`vaadin.version` property). A `Trigger` fires in the browser, reads
`Action.Input`s and runs `Action`s inside the same browser event, so work such
as a clipboard write, a download or a class change happens without a server
round-trip and still counts as part of the user's gesture. Many use cases
build their own triggers, actions and inputs on top of the API. The package is
marked internal, so its shapes can still change. Where the API falls short is
recorded in [API-GAPS.md](API-GAPS.md).

| # | View | What it shows |
| - | ---- | ------------- |
| UC1 | Ctrl+S snapshot | Ctrl+S (Cmd+S on a Mac) copies the notes to the clipboard in the browser, and the "Save Page" dialog does not open. |
| UC2 | Image gallery select | Clicking a tile selects it and dims the other two in one click, without a server round-trip. |
| UC3 | Hidden share link | Copies a share URL that the server builds but no component shows, through `SignalInput`. |
| UC4 | Double-click copy | Double-clicking a coupon code copies it, and the server shows what the browser reported as copied. |
| UC5 | Idle warning | A custom trigger tells the server when the user has been idle for 5 seconds, and when activity resumes. |
| UC6 | Network status | An online/offline badge that updates in the browser, also when the server can not be reached. |
| UC7 | Cross-tab broadcast | A message typed in one tab shows in every open tab of the view, sent through a `BroadcastChannel` without the server. |
| UC8 | Long-press to delete | Only an 800 ms press-and-hold deletes a row; a short click does nothing. |
| UC9 | Scroll into view | The same scroll done in the browser and from the server, side by side. |
| UC10 | Highlight | One configurable action flashes a box with different colors and durations. |
| UC11 | Right-click coords | A right-click opens no browser menu; the server receives the position instead. |
| UC12 | Accessibility announce | Saving also tells screen readers "Saved", through an aria-live region, without moving the focus. |
| UC13 | Client-side filter | A list sent once by the server is filtered in the browser on every keystroke. |
| UC14 | Responsive cards | A card grid switches between one, two and three columns by the width of the view. |
| UC15 | Live size readout | The width and height of the view update live as the window is resized. |
| UC16 | Pointer tracker | The pointer position inside a box updates on every move, without the server. |
| UC17 | Atomic reset | One click clears three fields in the browser, and a line shows that the server sees the reset. |
| UC18 | Auto-save signal | Every keystroke goes into a server signal, and the character and word counts are bound to it. |
| UC19 | Dynamic responsive styling | A box takes the color the user picked for its current width. |
| UC20 | Double-click → new tab | Double-clicking a row opens its page in a new tab without the popup blocker stopping it. |
| UC21 | Shortcut download | Ctrl+Shift+D (Cmd+Shift+D on a Mac) downloads a CSV file that the server generates. |
| UC22 | Key event log | Shows the key, code and modifiers of every key press, written in the browser. |
| UC23 | Konami code | A key sequence is tracked in the browser; only the completed sequence reaches the server. |
| UC24 | Disable button group | Clicking one of three decision buttons disables all three at once, until the server is done. |

## Run

```
./mvnw -pl common install -DskipTests
./mvnw -pl triggers spring-boot:run
```

Open <http://localhost:8080/>.

The clipboard use cases (UC1, UC3, UC4) need a secure context: HTTPS or
`localhost`.
