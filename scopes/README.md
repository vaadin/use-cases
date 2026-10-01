# Scopes — use cases

A standalone Spring Boot demo of the bean scopes available to a Vaadin Flow
application, including the browser tab scope built on
`com.vaadin.flow.server.BrowserTab` from vaadin/flow#25901. Each view shows one
realistic situation where that scope, and not another, is the right fit.

| # | Scope | View | What it shows |
| - | ----- | ---- | ------------- |
| UC1 | Application (singleton; CDI `@ApplicationScoped`) | Maintenance banner | An operator publishes a downtime notice that every user sees immediately. |
| UC2 | `@VaadinSessionScope` | Shared shopping cart | One cart shared by all tabs of the same user; another browser has its own. |
| UC3 | Spring `@SessionScope` (CDI `@SessionScoped`) | Export settings | Export settings chosen in a view are read by a plain Spring MVC controller that serves the CSV download. |
| UC4 | `@UIScope` | Undo history | A toolbar and an editor share one undo history per UI; a reload or another tab starts over. |
| UC5 | `@RouteScope` + `@RouteScopeOwner` | Customer draft | A draft kept across the pages of a multi-page editor and discarded when leaving it. |
| UC6 | Browser tab (`@BrowserTabScope`, see `MissingAPI`) | Booking wizard | A multi-step booking that survives reloads and full page navigation, separate per tab, releasing the held seat when the tab expires. |
| UC7 | Browser tab vs. `@VaadinSessionScope` | Ticket per tab | Each tab keeps its own support ticket and reply draft after a reload; the session-scoped alternative is shown for comparison. |
| UC8 | All of the above | Scope playground | Per-scope visit counters that show which scope survives a reload, a new tab, a duplicated tab, another browser, or navigating away. |

The browser tab scope is not part of Vaadin yet. `MissingAPI` contains a
Spring `Scope` implementation on top of `BrowserTab` and a `@BrowserTabScope`
annotation. The module overrides `flow.version` with the
`25.4.browser-tab-scope-SNAPSHOT` build of vaadin/flow#25901; switch back to
the parent's version once `BrowserTab` is released.

See [API-GAPS.md](API-GAPS.md) for what was missing or awkward.

## Run

```
cd scopes
mvn spring-boot:run
```

Open <http://localhost:8080/>. Most use cases need two tabs or two browsers
(one normal and one private window) to see the difference between scopes.
