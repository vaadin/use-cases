# Printing — API gaps discovered while building the demos

Places where Vaadin has no API for a genuine printing use case, or makes one awkward enough to need a workaround. Each entry names the use case that surfaced it.

The short version: **Vaadin has no printing API of any kind.** There is no `Page#print()`, no print lifecycle event, no way for a component to say "I am chrome, not content", nothing for the CSS page box, and nothing that makes a `Grid` or a `Chart` printable. Every use case in this module can be built today, and every one of them starts by writing JavaScript. That code lives in `src/main/java/com/example/MissingAPI.java`, `src/main/java/com/example/print/PrintEvents.java` and `src/main/java/com/example/print/PrintTrigger.java` instead of being repeated in the views.

Data *export* is tracked in [vaadin/platform#7196](https://github.com/vaadin/platform/issues/7196); the two meet in UC3 (#8).

---

## 1. There is no `Page#print()`

**Where it bites:** UC1 / PrintCurrentViewView.java, and every other use case
**Symptom:** the most common printing operation — open the browser's print dialog for the current page — has no Java API. `Page` has `reload()`, `open()`, `setTitle()` and `fetchCurrentURL()`, but nothing for printing.
**Workaround used:** `MissingAPI#print(UI)`, which calls `executeJs("window.print()")`.
**Suggested API:** `Page#print()`, and a `Page#print(Component)` that prints one component's subtree.

## 2. The server cannot learn that anything was printed

**Where it bites:** UC1 / PrintCurrentViewView.java
**Symptom:** "who printed this invoice, and when" is a normal audit requirement. The browser fires `beforeprint` and `afterprint`, but on `window`, and Flow's `Element` API can only listen on elements. So there is no server-side event without JavaScript. Removing the listener is harder than adding it: a `window.addEventListener` from `onAttach` outlives the view, so every visit leaks one more listener, and JavaScript scheduled on an element that is being detached never reaches the browser.
**Workaround used:** `PrintEvents`, an invisible component. It registers the two listeners against an `AbortController` that it stores in a registry on `window` under its own key, maps them to `@ClientCallable` methods, and aborts the controller from `onDetach` through `Page#executeJs` (`Element#executeJs` would never be sent). `MissingAPI#withPrintListenerRegistry` and `#abortPrintListeners` hold that pattern.
**Suggested API:** `Page#addBeforePrintListener` / `addAfterPrintListener` returning a `Registration`, like the existing `Page#addBrowserWindowResizeListener`. A `printStateSignal()` like `pageVisibilitySignal()` would fit the 25.x style even better.

Such listeners can only observe printing, never change it: the browser lays out the pages synchronously after `beforeprint`, before any round trip returns. Changing what is printed needs a trigger that runs in the browser (#4).

## 3. Nothing can say "this component is chrome, do not print it"

**Where it bites:** UC1, UC3, UC4, UC5, UC6 — the whole module
**Symptom:** a printed page should contain the document and nothing else: no navbar, no drawer, no toolbar, no "View source" overlay. Components have no printing behaviour, so the application has to name every non-printable piece in CSS. `display: none` in a print stylesheet cannot reach into a shadow root, so hiding the navbar and drawer of `vaadin-app-layout` needs `::part()` (one rule per exposed part, and only for parts that are exposed) or a trick.
**Workaround used:** `print.css` hides `body *` with `visibility: hidden` and shows the `.printable` subtree again. `visibility` is inherited across shadow roots, so hiding the host hides everything inside it. The document then needs `position: absolute`, so the space of its hidden ancestors does not print as a blank first sheet.
**Suggested API:** `component.setPrintable(false)`, or a `vaadin-app-layout` that keeps its own chrome off the paper by default, or a documented `@media print` contract per component.

## 4. Changing the page for print needs a trigger that runs in the browser

**Where it bites:** UC7 / ExpandForPrintView.java, UC6 / PrintDashboardView.java
**Symptom:** some changes for paper cannot be made in CSS. The content of a closed `Details` sits in its shadow root, where a print stylesheet cannot open it, and a chart has to be told to re-measure. A server-side `beforeprint` listener is too late (#2): the change only reaches the screen.
**Workaround used:** `com.example.print.PrintTrigger`, a subclass of Flow's `Trigger` that fires on `beforeprint` or `afterprint` of `window`. Its actions run in the browser inside the event. UC7 wires each section to the built-in `SetPropertyAction` and `PropertyInput` to remember its `opened` state, open it, and restore it afterwards; UC6 wires a small `ReflowChartsAction`. The trigger API adds and removes the listener with the host component. The catch: the whole trigger API is still in `com.vaadin.flow.component.trigger.internal`.
**Suggested API:** a public print trigger once the trigger API leaves `internal`, e.g. `PrintTrigger.beforePrint(host)` / `afterPrint(host)`, so that "open this, hide that, reflow those while printing" is one line of Java. A `Details#setOpenedWhenPrinting(true)` would cover the most common case without any wiring.

## 5. A property set for print is rendered too late when a button prints

**Where it bites:** UC7 / ExpandForPrintView.java
**Symptom:** with Ctrl/Cmd+P every section was open on paper; with the view's own Print button, only the one that was already open. The trigger sets `opened` in time, but Vaadin's web components render a property change in a microtask, and `vaadin-details` hides its content based on the `opened` attribute that this rendering sets. `window.print()` fires `beforeprint` while the calling script is still running, so no microtask runs before the pages are laid out and the sections print folded. Ctrl/Cmd+P starts printing with no script running, which hides the problem.
**Workaround used:** `com.example.print.RenderNowAction`, wired after the `SetPropertyAction`s, calls Lit's `performUpdate()` so the change is rendered inside the event. That method is not part of the components' documented API.
**Suggested API:** a `SetPropertyAction` that renders before it returns, or a documented "render now" on the web components. Waiting for pending updates before `window.print()` would not help: the change is made inside `beforeprint`, after that wait.

## 6. A print-only route prints a blank page

**Where it bites:** UC2 / PackingSlipView.java
**Symptom:** a route that prints itself on arrival is the standard "print invoice" pattern. Calling `print()` from `onAttach` prints an empty sheet: Flow sends the DOM changes and the JavaScript in the same response, and `window.print()` blocks the main thread before the browser has laid out the new DOM. `Page` has no "run this once the client has rendered" callback; `beforeClientResponse` is still server-side.
**Workaround used:** `MissingAPI#printAfterRender(UI)`, two nested `requestAnimationFrame` calls before `window.print()`.
**Suggested API:** `Page#executeJsAfterRender(...)`, or a `Page#print()` that is specified to print what the current response renders.

## 7. `Page#open` cannot size the window it opens

**Where it bites:** UC2 / PrintRouteView.java
**Symptom:** `Page#open(String url, String windowName)` takes a window name but no window features, so the print window cannot get a paper-shaped size or be opened without the browser's own chrome. The trigger API's `OpenInNewTabAction(String url, String features)` takes features, but it is in the `internal` package.
**Workaround used:** `MissingAPI#openPrintWindow(UI, String)`, which calls `window.open($0, '_blank', 'width=900,height=1000')`.
**Suggested API:** `Page#open(String url, String windowName, WindowFeatures features)`.

## 8. A `Grid` cannot be printed at all

**Where it bites:** UC3 / PrintableListView.java
**Symptom:** there are three reasons, and each one is enough. The `Grid` renders only the rows that fit its viewport and reuses them while scrolling, so the printer sees a few rows out of 120. Its cells are in a shadow root, so a print stylesheet cannot restyle them. Its rows are not table rows, so the browser cannot repeat the header on each sheet or avoid cutting a row in half. `setAllRowsVisible(true)` fixes none of this for a long list.
**Workaround used:** render the same data a second time as a plain HTML `Table`, hidden on screen (`.print-only`) and shown by `print.css`. One list of `PrintColumn`s (`com.example.print.PrintColumns`) drives both, so each column is described once. Keeping the two in sync at runtime is still the application's job: when the sort order changes, both have to be rebuilt.
**Suggested API:** `grid.getPrintableContent()` returning a light-DOM table of the rows in view order, or a `Grid` print mode that replaces itself with one during `beforeprint`. The data half is what [vaadin/platform#7196](https://github.com/vaadin/platform/issues/7196) asks for ("the rows as displayed, with filter and sorting applied, and the plain text of each cell"), so export and print should be built on the same primitive.
Upstream: https://github.com/vaadin/cookbook/issues/184

## 9. There is no API for the page box

**Where it bites:** UC4 / PrintPreviewView.java
**Symptom:** paper size, orientation and margins are set with the CSS `@page` at-rule. It is a document-level rule, so it cannot be set through `Element#getStyle()`, a class name or a theme variant, and Flow has nothing else for it. The application also never learns what the user chose in the print dialog, so an in-app preview is always a guess.
**Workaround used:** `MissingAPI#setPageRule(UI, String)` writes a `<style>` element into the head and replaces its content on every change; `com.example.print.PageSetup` builds the rule and the preview size from one record. The rule belongs to the document, not to the view, so UC4 sets it from `onAttach` (the constructor has no UI yet) and removes it from `onDetach` (`MissingAPI#clearPageRule`). Otherwise every later view prints on the paper chosen here.
**Suggested API:** `Page#setPageSetup(PageSetup)` with paper size, orientation and margins, mapped to `@page`, and the chosen setup reported back if browsers ever expose it.

## 10. Page numbers and a repeated letterhead have to be paginated by hand

**Where it bites:** UC5 / HeaderFooterView.java
**Symptom:** "Page 2 of 4" and a letterhead on every sheet make a printed document a business document. CSS defines both (`@page` margin boxes and `counter(page)`), but no major browser supports them for content pages. The browser's own header and footer follow the user's settings and show the URL, not a company address. The only thing that repeats is a table's `thead` (`display: table-header-group`), which only covers a table header.
**Workaround used:** the server cuts the document into fixed-size sheets, each with its own letterhead and "Page n of m", and `break-after: page` puts each on its own sheet. The application has to guess how many lines fit, because nothing can measure the paper.
**Suggested API:** hard to fix in the framework alone. A documented `Sheet` / `PrintLayout` component that owns the header and footer repeated on every page, plus the `break-*` rules, would at least keep every application from building the same one.

## 11. A `Chart` prints at its screen width

**Where it bites:** UC6 / PrintDashboardView.java
**Symptom:** Highcharts sizes its SVG once, in pixels, when the chart is drawn. The print stylesheet changes the layout width, and nothing tells the chart to re-measure, so it prints clipped or runs onto the next sheet. The server cannot help: printing is synchronous in the browser (#2), so a server-side listener such as `Chart#addChartBeforePrintListener` answers after the pages are laid out.
**Workaround used:** a `PrintTrigger` pair (#4) wired to `com.example.print.ReflowChartsAction`, which calls `chart.configuration.reflow()` on every `vaadin-chart` in the subtree on `beforeprint`, and again on `afterprint` for the screen.
**Suggested API:** `Chart#setReflowOnPrint(true)`, or make that the default: a chart that prints wrong by default is a bug, not a setting.

## 12. `Dashboard` gives printing nothing to hold on to

**Where it bites:** UC6 / PrintDashboardView.java
**Symptom:** the columns of a `Dashboard` are sized for a screen. On A4 they are too narrow to read, and a widget can be cut across two sheets. The layout is in shadow DOM, so the only levers are the exposed custom properties and `::part()`, and "one column when printing" has to be an override of `--vaadin-dashboard-col-max-count` inside a media query.
**Workaround used:** `uc6.css`, a `@media print` block that forces one column and `break-inside: avoid` on every widget.
**Suggested API:** a documented print behaviour for `Dashboard` and `Board`: one column, widgets kept whole, charts reflowed.
Upstream: https://github.com/vaadin/board/issues/103

## 13. Printing cannot be tested

**Where it bites:** every test in this module / `PrintTestSupport.java`
**Symptom:** the browserless test kit has no print simulator (nothing like `GeolocationSimulator`), so a test cannot make the browser print or check what came out. A test can only see the JavaScript a view queued, which is why `PrintTestSupport` reads `UIInternals#containsPendingJavascript`. That has a sharp edge: `Page#executeJs` is in the pending list at once, `Element#executeJs` only after a `roundTrip()`. Listeners added through the trigger API are harder still: they travel as function arguments of a generic initializer, so `containsPendingJavascript` does not see them, and their removal on detach happens in the browser without any message from the server.
**Workaround used:** `src/test/java/com/example/PrintTestSupport.java`, which also unpacks those function arguments, and calling `PrintEvents#beforePrint()` / `#afterPrint()` directly in place of the browser's events.
**Suggested API:** a `PrintSimulator` in the test kit that fires the print lifecycle, and, once a printing API exists, an assertion that a print was requested instead of matching a JavaScript string.
