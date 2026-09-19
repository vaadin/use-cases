# Printing — API gaps discovered while building the demos

Places where Vaadin has no API for a genuine printing use case, or makes one
awkward enough to need a workaround. Each entry names the use case that
surfaced it.

The short version: **Vaadin has no printing API of any kind.** There is no
`Page#print()`, no print lifecycle event, no way for a component to say "I am
chrome, not content", nothing for the CSS page box, and nothing that makes a
`Grid` or a `Chart` printable. Every use case in this module is buildable
today, and every one of them starts by writing JavaScript. That plumbing lives
in `src/main/java/com/example/MissingAPI.java` and
`src/main/java/com/example/print/PrintEvents.java` rather than being scattered
over the views.

Printing is also missing from the issue trackers: it appears only as
[vaadin/cookbook#184](https://github.com/vaadin/cookbook/issues/184) ("How do I
print a Grid", open since 2021),
[vaadin/board#103](https://github.com/vaadin/board/issues/103) and
[vaadin/charts#530](https://github.com/vaadin/charts/issues/530). Data *export*
is tracked — [vaadin/platform#7196](https://github.com/vaadin/platform/issues/7196)
— and the two meet in UC3.

---

## There is no `Page#print()`

**Where it bit us:** uc1 / PrintCurrentViewView.java, and every other use case
**Symptom:** the single most common printing operation there is — open the
browser's print dialog for the current page — has no Java API. `Page` offers
`reload()`, `open()`, `setTitle()`, `fetchCurrentURL()`, but nothing for
printing.
**Workaround used:** `MissingAPI#print(UI)` — `executeJs("window.print()")`.
**Suggested API:** `Page#print()`, and a `Page#print(Component)` that prints
one component's subtree.

## The server cannot learn that anything was printed

**Where it bit us:** uc1 / PrintCurrentViewView.java
**Symptom:** "who printed this invoice, and when" is an ordinary audit
requirement, and the browser does fire `beforeprint` and `afterprint` — on
`window`. Flow's `Element` API can only listen on elements, so there is no way
to subscribe to them without JavaScript, and no server-side event to hook into.
Unregistering is worse than registering: a `window.addEventListener` issued
from `onAttach` outlives the view, so every visit to the view leaks one more
listener, and JavaScript scheduled on an element that is being detached is
dropped before it reaches the browser.
**Workaround used:** `PrintEvents`, an invisible component that registers the
two listeners against an `AbortController` stored in a registry on `window`
under a key of its own, maps them to `@ClientCallable` methods, and aborts that
controller from `onDetach` through `Page#executeJs` (not `Element#executeJs`,
which would never be sent). `MissingAPI#withPrintListenerRegistry` and
`#abortPrintListeners` hold that pattern, because the chart reflow below needs
exactly the same bookkeeping.
**Suggested API:** `Page#addBeforePrintListener` / `addAfterPrintListener`
returning a `Registration`, in the shape of the existing
`Page#addBrowserWindowResizeListener`. A `printStateSignal()` in the shape of
`pageVisibilitySignal()` would fit the 25.x style even better.

## Nothing can say "this component is chrome, do not print it"

**Where it bit us:** uc1, uc3, uc4, uc5, uc6 — the whole module
**Symptom:** a printed page should contain the document and nothing else: no
navbar, no drawer, no toolbar, no "View source" overlay. Components expose no
opinion about printing, so the application has to name every non-printable
piece in CSS. Worse, `display: none` in a print stylesheet cannot reach into a
component's shadow root, so hiding `vaadin-app-layout`'s navbar and drawer
needs either `::part()` — one rule per exposed part, and only for the parts
that happen to be exposed — or a trick.
**Workaround used:** `print.css` hides `body *` with `visibility: hidden` and
un-hides the `.printable` subtree. `visibility` is inherited and inheritance
crosses shadow roots, so hiding the host hides everything inside it too. The
document then has to be lifted out of the flow with `position: absolute` so the
space its hidden ancestors occupy does not print as a blank first sheet.
**Suggested API:** `component.setPrintable(false)`, or a
`vaadin-app-layout` that keeps its own chrome off the paper by default, or a
documented `@media print` contract per component.

## A print-only route prints a blank page

**Where it bit us:** uc2 / PackingSlipView.java
**Symptom:** a route that prints itself on arrival is the standard "print
invoice" pattern. Calling `print()` from `onAttach` prints an empty sheet: Flow
delivers the DOM changes and the JavaScript in the same response, and
`window.print()` blocks the main thread before the browser has laid the new DOM
out. There is no "run this once the client has rendered" callback —
`beforeClientResponse` is still server-side.
**Workaround used:** `MissingAPI#printAfterRender(UI)` — two nested
`requestAnimationFrame` calls before `window.print()`.
**Suggested API:** `Page#executeJsAfterRender(...)`, or simply a `Page#print()`
that is specified to print what the current response renders.

## `Page#open` cannot size the window it opens

**Where it bit us:** uc2 / PrintRouteView.java
**Symptom:** `Page#open(String url, String windowName)` takes a window name but
no window features, so the print window cannot be given a paper-shaped size or
be opened without the browser's own chrome.
**Workaround used:** `MissingAPI#openPrintWindow(UI, String)` —
`window.open($0, '_blank', 'width=900,height=1000')`.
**Suggested API:** `Page#open(String url, String windowName, WindowFeatures
features)`.

## A `Grid` cannot be printed at all

**Where it bit us:** uc3 / PrintableListView.java
**Symptom:** three separate reasons, and any one of them would be enough.
The Grid renders only the rows that fit its viewport and recycles them while
scrolling, so the printer sees a handful of rows out of 120. Its cells live in
a shadow root, so a print stylesheet cannot restyle them. And its rows are not
table rows, so the browser cannot repeat the header on each sheet or avoid
breaking a row in half. `setAllRowsVisible(true)` fixes none of it for a list
of any length.
**Workaround used:** render the same data a second time as a plain HTML
`Table`, hidden on screen (`.print-only`) and revealed by `print.css`. Both
renderings are driven from one list of `PrintColumn`s
(`com.example.print.PrintColumns`) so a column is described once — but keeping
the two in sync at runtime is the application's job: change the sort order and
both have to be rebuilt.
**Suggested API:** `grid.getPrintableContent()` returning a light-DOM table of
the rows in view order, or a `Grid` print mode that swaps itself for one while
`beforeprint` runs. The data half of this is exactly what
[vaadin/platform#7196](https://github.com/vaadin/platform/issues/7196) asks for
— "the rows as displayed, with filter and sorting applied, and the plain text
of each cell" — which suggests the export API and a print rendering should be
built on the same primitive, not twice.

## There is no API for the page box

**Where it bit us:** uc4 / PrintPreviewView.java
**Symptom:** paper size, orientation and margins are the CSS `@page` at-rule.
Being a document-level at-rule, it cannot be set through `Element#getStyle()`,
a class name or a theme variant, and Flow exposes nothing else for it. The
application also never learns what the user actually chose in the print dialog,
so an in-app preview can only ever be a guess.
**Workaround used:** `MissingAPI#setPageRule(UI, String)` writes a `<style>`
element into the head and replaces its content on every change;
`com.example.print.PageSetup` builds the rule and the matching preview
dimensions from one record. Because the rule belongs to the document rather
than to the view that set it, UC4 also has to push it from `onAttach` (the
constructor has no UI yet) and drop it again from `onDetach`
(`MissingAPI#clearPageRule`) — otherwise every later view in the application
prints on the paper somebody chose here.
**Suggested API:** `Page#setPageSetup(PageSetup)` with paper size, orientation
and margins, mapping to `@page` — and, if the browser ever reports it, the
chosen setup back.

## Page numbers and a repeated letterhead have to be paginated by hand

**Where it bit us:** uc5 / HeaderFooterView.java
**Symptom:** "Page 2 of 4" and a letterhead on every sheet are what makes a
printed document a business document. CSS specifies both — `@page` margin boxes
and `counter(page)` — and no major browser implements them for content pages;
the browser's own header and footer belong to the user's settings and carry the
URL, not a company address. The only construct that repeats is a table's
`thead` (`display: table-header-group`), which covers a table header and
nothing else.
**Workaround used:** the server cuts the document into fixed-size sheets, each
with its own letterhead and its own "Page n of m", and `break-after: page`
puts each on its own sheet. The application has to guess how many lines fit,
because nothing can measure the paper.
**Suggested API:** hard to fix in the framework alone, but a documented
`Sheet`/`PrintLayout` component that owns the repeat-on-every-page header and
footer, plus the `break-*` rules, would at least keep every application from
inventing the same one.

## A `Chart` prints at its screen width

**Where it bit us:** uc6 / PrintDashboardView.java
**Symptom:** Highcharts sizes its SVG once, in pixels, when the chart is drawn.
The print stylesheet changes the layout width underneath it and nothing tells
the chart to re-measure, so it prints clipped or spilling onto the next sheet.
This is the shape of
[vaadin/charts#530](https://github.com/vaadin/charts/issues/530). The server
cannot help: printing is synchronous on the client, so by the time a
`beforeprint` round trip reached the server the page would already be
rasterised.
**Workaround used:** `com.example.print.ChartPrintReflow`, an invisible
component in the same shape as `PrintEvents`: it registers a
`beforeprint`/`afterprint` pair that calls `chart.configuration.reflow()` on
every `vaadin-chart` in the subtree, and aborts them on detach so that
revisiting the view does not pile up another pair.
**Suggested API:** `Chart#setReflowOnPrint(true)`, or simply making that the
default — a chart that prints wrong by default is a bug, not a setting.

## `Dashboard` gives printing nothing to hold on to

**Where it bit us:** uc6 / PrintDashboardView.java
**Symptom:** a dashboard's columns are sized for a screen; on A4 they are too
narrow to read, and a widget can be broken across two sheets. The layout lives
in shadow DOM, so the only levers are the exposed custom properties and
`::part()`, and "one column when printing" has to be expressed as an override
of `--vaadin-dashboard-col-max-count` inside a media query.
[vaadin/board#103](https://github.com/vaadin/board/issues/103) has asked for a
printable Board since 2017.
**Workaround used:** `uc6.css` — a `@media print` block that forces a single
column and `break-inside: avoid` on every widget.
**Suggested API:** a documented print behaviour for `Dashboard` and `Board`:
single column, widgets kept whole, charts reflowed.

## Printing cannot be tested

**Where it bit us:** every test in this module
**Symptom:** there is no print simulator in the browserless test kit — nothing
comparable to `GeolocationSimulator` — so a test cannot make the browser print,
and cannot assert on what came out. What a test *can* observe is the JavaScript
a view queued, which is why `PrintTestSupport` reads
`UIInternals#containsPendingJavascript`. That in turn has a sharp edge:
`Page#executeJs` lands in the pending list immediately, while
`Element#executeJs` only does after a `roundTrip()`.
**Workaround used:** `src/test/java/com/example/PrintTestSupport.java`, plus
calling `PrintEvents#beforePrint()` / `#afterPrint()` directly to stand in for
the browser's events.
**Suggested API:** a `PrintSimulator` in the test kit that fires the print
lifecycle, and — once a printing API exists — an assertion that a print was
requested, instead of matching on a JavaScript string.
