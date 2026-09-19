# Printing — use cases

A standalone Spring Boot demo of printing a Vaadin application: the page the
user is looking at, a document that opens in its own window, a list too long
for one sheet, paper setup, a letterhead on every page, and a dashboard of
charts.

Vaadin has **no printing API**. No `Page#print()`, no print lifecycle event, no
way to mark a component as chrome rather than content, nothing for the CSS page
box, and nothing that makes a `Grid` or a `Chart` printable. Every view here is
built on `src/main/resources/META-INF/resources/print.css` and a handful of
JavaScript shims in `com.example.MissingAPI` and `com.example.print`
(`PrintEvents`, `ChartPrintReflow`). That is
the point of the module: the use cases are all achievable today, and
[API-GAPS.md](API-GAPS.md) records what each one had to work around to get
there.

| # | View | What it shows |
| - | ---- | ------------- |
| UC1 | Print the current view | A Print button, an application shell that stays off the paper, an opt-in for an internal note, and a status line fed by the browser's `beforeprint` / `afterprint` events. |
| UC2 | A print-only route | `uc2/slip/<order>` has no layout at all: it opens in its own window, prints itself once the browser has painted it, and closes with the dialog. |
| UC3 | Printing a long list | 120 orders. A virtualised Grid prints only its rendered rows, from a shadow root print CSS cannot reach, so the same rows are rendered a second time as an HTML table whose header repeats on every sheet. |
| UC4 | Paper setup and preview | Paper size, orientation and margins, previewed at true size on screen and applied to the real page box through an injected `@page` rule. |
| UC5 | Letterhead and page numbers | A 64-line order paginated by the server into numbered sheets, because no browser implements CSS page counters for content pages. |
| UC6 | Printing a dashboard | Charts reflowed to the paper width on `beforeprint`, the dashboard collapsed to one column, every widget kept whole. |

Data export is deliberately **not** part of this module — that is
[vaadin/platform#7196](https://github.com/vaadin/platform/issues/7196) and the
`grid-export` module. UC3 is where the two meet: a printable rendering of a
Grid needs exactly the same "rows as displayed, cell as text" primitive the
export API is being designed around.

## Run

```
cd printing
mvn spring-boot:run
```

Open <http://localhost:8080/>. To see what a view puts on paper without a
printer, use the browser's print preview (Ctrl/Cmd-P) or Chrome DevTools →
Rendering → *Emulate CSS media type: print*.

## Tests

```
./mvnw -pl printing -am test -DskipFrontend=true
```

One browserless test per use case. Printing itself cannot be observed without
a browser — there is no print simulator in the test kit — so the tests assert
on what a view asks the browser to do (`PrintTestSupport` reads the pending
JavaScript) and on what it renders: that the printable table holds all 120
rows the Grid has, that 64 lines become four numbered sheets, that the `@page`
rule follows the paper controls.

The print stylesheet itself was verified in a real headless Chromium with the
print media type emulated: the application shell resolves to
`visibility: hidden`, the `.printable` document to `visible`, `.no-print` to
`display: none`, UC3's table to `display: block`, and UC6's dashboard to a
single full-width column.
