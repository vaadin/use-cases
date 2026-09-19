# Grid data export — API gaps discovered while building the demos

Places where Vaadin Flow either has no API for a genuine Grid-export use case,
or makes one awkward enough to work around. Each entry is keyed to the use case
that surfaced it.

The reference point is
[vaadin/platform#7196 — Grid data export API](https://github.com/vaadin/platform/issues/7196),
which is still a draft acceptance-criteria issue: **nothing of it is
implemented**. There is no `GridExporter`, no `grid.createExporter()`, no
`Column#getCellContent(item)`. So the honest summary of this module is that all
eight use cases are buildable today, and all eight need the same plumbing
first — of which one line is reflection into a private field of a Flow class.

That plumbing lives in two places in this module, and both are workarounds:

- `src/main/java/com/example/MissingAPI.java` — the primitives that ought to be
  on `Grid` and `Grid.Column`: the rows in view order, the text of a cell, a
  text alternative for a rendered component.
- `src/main/java/com/example/export/GridExport.java` — the facade the issue
  sketches, hand-written, plus `CsvWriter`, `XlsxWriter` and `PdfWriter` as the
  "production-ready examples" acceptance criterion 0 asks Vaadin to ship.

---

## A plain `addColumn(ValueProvider)` column will not tell you its value

**Where it bit us:** uc1 / CsvOfCurrentViewView.java — and every other use case
**Symptom:** `grid.addColumn(Employee::name)` is the most common column in every
Vaadin application. It wraps the value provider in a `ColumnPathRenderer`, which
keeps it in a **private field** and exposes nothing:

```java
public class ColumnPathRenderer<SOURCE> extends Renderer<SOURCE> {
    private ValueProvider<SOURCE, ?> provider;   // no getter
    private String property;                     // no getter
}
```

`Column#getRenderer()` therefore hands back an object from which the cell's text
cannot be obtained. For the single most ordinary column type in the framework,
there is no supported way to answer "what does this cell say".

How strong a signal this is: Vaadin's own browserless test toolkit needs the
same answer, and solves it the same way. `GridTester#getCellText(row, column)`
contains, verbatim,
`ColumnPathRenderer.class.getDeclaredField("provider").setAccessible(true)`.
When the test library ships the reflection hack, the production API is missing.

**Workaround used:** `MissingAPI#cellText(column, item)` does the same
reflection, cached in a static `Field`, and throws a message pointing at this
file if the field is ever renamed.
**Suggested API:** the accessor `mstahv` proposes in the issue discussion, on
`Column`:

```java
CellType getCellType();          // TEXT, HTML, COMPONENT
Object getCellContent(T item);   // the rendered content for one item
```

A narrower fix that would already unblock exporters:
`ColumnPathRenderer#getValueProvider()` and `#getProperty()` as public getters,
mirroring `LitRenderer#getValueProviders()`, which *is* public.

## No `Grid#getItems()`, and the grid's sort order is not in the data provider

**Where it bit us:** uc1 / CsvOfCurrentViewView.java, uc7 /
LargeDatasetExportView.java
**Symptom:** "export what the user sees" means the current filter and the
current sorting. Neither is where you would look for it. Clicking a column
header does not touch the `ListDataProvider`'s sort comparator — it calls
`DataCommunicator#setInMemorySorting` — so re-reading the data provider gives
the rows back in their original order, silently. `setItems()` has no
`getItems()` opposite either; the nearest thing is
`getGenericDataView().getItems()`, which is not what a user who called
`setItems` goes looking for.

**Workaround used:** `MissingAPI#rowsInViewOrder(grid)` asks the
`DataCommunicator` for the query the grid would send —
`dataProvider.fetch(dataCommunicator.buildQuery(0, Integer.MAX_VALUE))` — which
is the only place filter and sorting are both applied. That is what
`GridListDataView#getItems()` does internally, but that class only exists for
in-memory data.
**Suggested API:** `Grid#getItemsInViewOrder()` (or an exporter method) that
works for every data provider type, documented as "filtered and sorted as
displayed".

## A formatted cell costs a component, and you can never have both the value and its formatting

**Where it bit us:** uc4 / RenderedValuesView.java, uc5 / ExcelReportView.java
**Symptom:** `NumberRenderer` and `LocalDateRenderer` hold exactly what a report
wants — the formatted string — in `getFormattedValue()`, which is `protected`,
fed by `getValueProvider()`, also `protected`. The only public way in is
`ComponentRenderer#createComponent(item)`, which builds a whole `Span` per cell
just so its text can be read back out. For a 24 500-row export that is 24 500
throwaway components per formatted column.

Worse for spreadsheets: the result is *only* the formatted string. UC5 writes
`"45,000.00"` into the `.xlsx` as text, because there is no way to ask the same
column for the underlying `45000.0` that Excel would want to sum. A report
generator needs both — the number for the cell, the format for the cell style —
and can get neither together.

**Workaround used:** `MissingAPI#cellText` routes `BasicRenderer` subclasses
through `createComponent(item)` and reads the element text. UC5's totals are
computed from the items directly, not from the exported cells.
**Suggested API:** promote `BasicRenderer#getFormattedValue` and
`#getValueProvider` to public, or return both from
`Column#getCellContent(item)` — e.g. a `CellContent(Object value, String text)`
— so a writer can choose which one it needs.

## A multi-property `LitRenderer` column is unreadable

**Where it bit us:** uc4 / RenderedValuesView.java (the "Contact" column)
**Symptom:** `LitRenderer#getValueProviders()` is public, which is enough when a
column renders exactly one property. With two or more, nothing says which of
them the cell displays, or in what order, because the template string is only
available through `protected String getTemplateExpression()`. The exporter can
see that `name` and `country` exist; it cannot see that the cell reads
`${item.name} · ${item.country}`.

**Workaround used:** `MissingAPI#cellText` returns `null` for that case, and
`GridExport` turns it into an error that names the column and tells the
developer to call `withColumnExtractor(column, item -> ...)`. The demo shows
that message on a button press, so the gap is visible in the app.
**Suggested API:** make `getTemplateExpression()` public (it is already the
renderer's own contract with the client), or have `LitRenderer` expose an
ordered `Map` plus the template so a plain-text rendering can be derived.

## No text alternative for a rendered component

**Where it bit us:** uc4 / RenderedValuesView.java, uc6 / RedactedExportView.java
**Symptom:** a `ComponentRenderer` cell can be built, but what its *text* is,
is anybody's guess. A `Checkbox` has no text at all; issue #7196 suggests
"Yes"/"No", an `Icon` its `icon` attribute, a `Button` its caption — all
reasonable, none of them stated anywhere the framework can enforce. Every
exporter in every application will guess slightly differently, and the guesses
will disagree with what a screen reader says about the same cell.
`TatuLund` makes the same point in the issue discussion: the text alternative
would serve accessibility as much as export.

Even reading the text of a component tree has a trap.
`Element#getText()` returns the direct text nodes, and `Element#getChildren()`
*includes those same text nodes*, so the obvious recursion emits every string
twice ("Active Active"). It cost us a failing test to notice.

**Workaround used:** `MissingAPI#componentText(component)` — a hand-rolled
walker with special cases for `Checkbox`, `Icon`, `Avatar`, `Anchor` and
`HasValue`, skipping text-node children.
**Suggested API:** a text alternative declared where the rendering is declared,
e.g. `ComponentRenderer#withTextAlternative(SerializableFunction<SOURCE,
String>)`, defaulting to the conventions listed in #7196's acceptance criterion
1, and reused for the cell's accessible name.

## `getSelectedItems()` is an unordered `Set`

**Where it bit us:** uc2 / ExportSelectionView.java
**Symptom:** "export the rows I ticked" is one of the most common export
requests, and `Grid#getSelectedItems()` answers it with a `Set<T>`. A report is
a document: its rows have to appear in the order the user saw them. Exporting
the set directly produces rows in hash order, which looks like a bug and is not
caught by any test that only checks row *contents*.

**Workaround used:** `GridExport#selectedRowsOnly()` keeps the grid's row order
as the spine and uses the selection only as a filter — a full re-read of the
data provider to order a handful of rows.
**Suggested API:** `Grid#getSelectedItemsInViewOrder()`, or make the exporter's
"selected rows only" option carry the ordering guarantee.

## `Grid#getColumns()` does not reflect a user's drag reorder

**Where it bit us:** uc3 / ColumnChoiceView.java
**Symptom:** with `setColumnReorderingAllowed(true)`, the user drags columns
into the order they want. `getColumns()` keeps returning the declaration order
afterwards — the new order exists only in the `ColumnReorderEvent` that was
fired once, at drag time. Any application that wants to export what the user
arranged has to keep a shadow copy of its own column order and maintain it from
that event. Column visibility, by contrast, needs no bookkeeping at all:
`Column` is a `Component`, so `isVisible()` just works.

**Workaround used:** UC3 holds a `List<Grid.Column<Employee>> userOrder`, seeds
it from `getColumns()` and rewrites it on every `ColumnReorderEvent`, then
passes it to `GridExport#withColumnOrder(...)`. The view prints both orders side
by side so the divergence is visible.
**Suggested API:** have the client's reorder update the grid's own column order,
so `getColumns()` is the single source of truth — or add
`Grid#getColumnsInDisplayOrder()`.

## Header and footer cells have no accessible common supertype, and spans are only recoverable by cell identity

**Where it bit us:** uc5 / ExcelReportView.java
**Symptom:** two problems in the same place.

`HeaderRow.HeaderCell` and `FooterRow.FooterCell` have identical APIs
(`getText()`, `getComponent()`) and a shared supertype,
`AbstractRow.AbstractCell` — but `AbstractRow` is **package-private**, so there
is nothing to write the shared code against. `GridExport` carries the same
`cellOf(...)` and `text(...)` methods twice, once per cell type, for no reason
other than visibility.

And a joined header cell does not say which columns it covers:
`AbstractCell#getColumn()` is `protected`, and `ColumnGroup#getChildColumns()`
is only reachable if you already have the group. The one thing that leaks the
structure is that `row.getCell(column)` returns the *same cell instance* for
every column a join covers, so consecutive columns mapping to the same instance
can be collapsed into a span. That works, and is the least obvious code in this
module.

**Workaround used:** `GridExport#headerRows(...)` groups columns by cell
identity to produce `(text, span)` pairs, which `XlsxWriter` turns into
`CellRangeAddress` merges. Duplicate `text(...)` overloads for header and footer
cells.
**Suggested API:** make `AbstractRow`/`AbstractRow.AbstractCell` public (or
introduce a public `GridCell` interface), and add
`AbstractCell#getColumns()` returning the columns the cell spans.

## No per-column extractor or converter hook

**Where it bit us:** uc6 / RedactedExportView.java
**Symptom:** the export is rarely a literal copy of the screen. Dropping the
internal id column, masking a card number, turning a boolean into "Yes"/"No" —
all ordinary application code, with nowhere to put it. Without a per-column
hook, every exporter grows a private "which column is this, and what do I do to
it" switch, keyed on something fragile like the header string.

**Workaround used:** `GridExport#withColumnExtractor` and
`#withColumnConverter`, backed by `IdentityHashMap`s (`Grid.Column` does not
override `equals`/`hashCode`, so identity is the only sane key — worth stating
in whatever the real API does here).
**Suggested API:** exactly the builder in #7196's API sketch —
`withColumnExtractor(column, extractor)` and
`withColumnConverter(column, converter)` — plus `withExcludedColumns(...)`.

## No paged or streaming export path; `buildQuery` returns a raw `Query`

**Where it bit us:** uc7 / LargeDatasetExportView.java
**Symptom:** the grid is careful never to pull a large backend into memory; the
export has to be equally careful, and gets no help. The single-shot option,
`getLazyDataView().getItems()`, issues one query with
`limit = Integer.MAX_VALUE`, which is exactly what a 24 500-row export must not
do. Paging is possible — `DataCommunicator#buildQuery(offset, limit)` is public
and carries the current sorting — but it is documented as an internal detail
rather than an export entry point, and it returns a **raw** `Query`, so feeding
it back to `grid.getDataProvider().fetch(...)` needs
`@SuppressWarnings({"unchecked", "rawtypes"})`.

**Workaround used:** `MissingAPI#rowsInViewOrder(grid, pageSize)` wraps the
paging in a lazy `Stream`, and `CsvWriter#streamingDownload` writes each page
straight to the response through `DownloadEvent#getWriter()`. UC7 counts the
backend round trips to prove the export never fetches more than one page at a
time.
**Suggested API:** a paged/streaming export — `exporter.streamRows(pageSize)`
returning a lazy `Stream<Row>` — and, independently, a generically typed
`DataCommunicator#buildQuery`.

## A paginating writer needs the static parts over and over, and the rows twice

**Where it bit us:** uc8 / MultiPagePdfView.java
**Symptom:** CSV and `.xlsx` are forgiving formats: write the headers once,
stream the rows past them, done. A printed report is not. The header rows —
grouped cells included — have to be drawn again at the top of **every** page, so
the writer needs them as re-readable data for the whole run, not as something it
consumed at the start. And before it can place the first row it has to walk all
of them twice: once to measure the text and size the columns, once to lay them
out. Even "Page 1 of 6" cannot be printed until the row count is known.

The API sketch in #7196 does not support that shape. It exposes the report as
`List<Row> getRows()` alongside `getHeaderRows()` / `getFooterRows()`, which
works — but it is the opposite of the streaming shape UC7 needs, and an
exporter has to serve both. Any real API needs a lazy
`Stream<Row>` *and* a materialised snapshot, and has to be explicit about which
of the two a given accessor hands back.

**Workaround used:** `GridExport` deliberately exposes the two shapes as
separate entry points — `export()` for a materialised `ExportedGrid` and
`streamRows()` for the lazy walk — and `PdfWriter` takes the materialised one,
measures it, then paginates it.
**Suggested API:** keep the static parts (`getHeaderRows`, `getFooterRows`,
`getColumns`, `getEmptyStateText`) on the exporter itself, independent of the
rows, and offer the rows both ways: `getRows()` for a snapshot and
`streamRows(pageSize)` for a lazy walk.

## Column widths are CSS strings, so a report cannot size its columns

**Where it bit us:** uc8 / MultiPagePdfView.java
**Symptom:** a PDF table has to decide how wide each column is, in points. The
grid cannot say. `Column#getWidth()` returns a CSS string — `"120px"`, `"8em"`,
or `null` — and for the common `setAutoWidth(true)` case the real width is
computed by the browser from the rendered content and never exists on the
server. `getFlexGrow()` gives a ratio with no absolute anchor. So a report
writer has to re-measure every cell in its own font to get numbers it can use,
which is the second of UC8's two passes over the data.

What *does* survive is alignment: `ColumnBase#getTextAlign()` is public and
returns a real `ColumnTextAlign`, so the money columns of UC8 come out
right-aligned in the PDF exactly as they are on screen. It is the one piece of
the "export the styling" acceptance criterion that works today, which is worth
saying out loud next to everything in the styling entry below that does not.

**Workaround used:** `PdfWriter` measures every cell with
`PDFont#getStringWidth`, sums the per-column maxima and scales them to the page
width. `ExportedGrid` carries an `Alignment` per column, mapped from
`getTextAlign()` so the writers stay free of Grid types.
**Suggested API:** a resolved width per column in the export data — even an
approximate character count would beat a CSS string — or an explicit statement
in the export API that widths are the report writer's problem, so nobody looks
for them.

## No styling information to export

**Where it bit us:** uc5 / ExcelReportView.java
**Symptom:** acceptance criterion 2 of #7196 asks for "border, background and
text colors, width, spaces and other styles that are important in the report
document", and the issue's own API sketch admits it is "not clear what exactly
should be collected here". Building UC5 confirms the doubt: the server knows
`Column#getWidth()`, `getFlexGrow()`, `isAutoWidth()` and the *names* of part
name generators — it does not know any resolved colour, border or font, because
those are CSS resolved in the browser against the theme. A `GridStyles` facade
filled from the server would be a facade over almost nothing.

**Workaround used:** none. `XlsxWriter` applies its own styling (bold grey
headers, bold footer with a top border, merged group cells) rather than
pretending to mirror the grid.
**Suggested API:** don't try. Export the *structure* (spans, column order,
alignment, widths) and let the report writer own the appearance — or, if visual
fidelity is genuinely the goal, treat it as a client-side snapshot problem
rather than a server-side data problem.

## Not built: hierarchical export and Grid cloning

**Where it bit us:** nowhere — these two use cases from #7196 were left out of
the module deliberately, because there is no API to demonstrate.
**Symptom:**

- *TreeGrid / hierarchical data.* Exporting "the rows as the user sees them"
  means the currently expanded nodes, in display order, with their depth.
  `HierarchicalDataProvider` can fetch children level by level, but there is no
  public way to enumerate the grid's visible rows, and no depth on the item. An
  exporter would have to re-walk the tree and re-derive the expanded set.
- *Cloning a grid* (use case [3] of the issue: the same grid, styled the same,
  with no data, as a drag-and-drop target). Column configuration cannot be read
  back out in a form that can be re-applied: a `Renderer` cannot be re-attached
  to a second column, and there is no `Column` copy constructor or
  `Grid#copyConfigurationFrom(grid)`.

**Workaround used:** none.
**Suggested API:** for the hierarchical case, `TreeGrid#getVisibleItems()`
yielding items with their depth. For the clone case, a renderer that can be
reused across columns, or an explicit
`Grid#copyConfiguration()` covering columns, headers, footers and widths.
