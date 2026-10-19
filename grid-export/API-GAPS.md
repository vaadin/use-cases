# Grid data export — API gaps

Where Vaadin Flow has no API for a Grid export use case, or makes one awkward
enough to need a workaround.

The reference point is
[vaadin/platform#7196 — Grid data export API](https://github.com/vaadin/platform/issues/7196).
It is still a draft, and nothing of it is implemented: there is no
`GridExporter`, no `grid.createExporter()` and no `Column#getCellContent(item)`.
All eight use cases can be built today, but each one needs the same plumbing
first. That plumbing is a workaround and lives in two places:

- `com.example.MissingAPI` — the primitives that should be on `Grid` and
  `Grid.Column`: the rows in view order, the text of a cell, a text alternative
  for a rendered component.
- `com.example.export.GridExport` — a hand-written version of the facade the
  issue sketches, plus `CsvWriter`, `XlsxWriter` and `PdfWriter` as the
  copy-paste format examples that acceptance criterion 0 asks Vaadin to ship.

## 1. A plain `addColumn(ValueProvider)` column does not tell you its value

**Where it bites:** UC1 / CsvOfCurrentViewView.java, and every other use case
**Symptom:** `grid.addColumn(Employee::name)` wraps the value provider in a
`ColumnPathRenderer`. The renderer keeps the provider and the property name in
private fields with no getters, so `Column#getRenderer()` returns an object
that cannot say what a cell shows. This is the most common column type, and
there is no supported way to read its text. Vaadin's own `GridTester#getCellText`
has the same problem and reads the private `provider` field through reflection.
**Workaround used:** `MissingAPI#cellText(column, item)` reads the same field
through reflection (cached in a static `Field`). If the field is renamed, it
throws an error that points to this entry.
**Suggested API:** on `Grid.Column`, `CellType getCellType()` (TEXT, HTML,
COMPONENT) and `Object getCellContent(T item)`, as proposed in the issue
discussion. A smaller fix that already unblocks exporters: public
`ColumnPathRenderer#getValueProvider()` and `#getProperty()`, like the public
`LitRenderer#getValueProviders()`.
Upstream: https://github.com/vaadin/platform/issues/7196

## 2. No `Grid#getItems()`, and the grid's sort order is not in the data provider

**Where it bites:** UC1 / CsvOfCurrentViewView.java, UC7 /
LargeDatasetExportView.java
**Symptom:** "export what the user sees" needs the current filter and the
current sort order. A click on a column header does not change the
`ListDataProvider` sort comparator; it calls
`DataCommunicator#setInMemorySorting`. So reading the data provider again
silently returns the rows in their original order. `Grid#setItems` has no
`getItems()` counterpart; the nearest is `getGenericDataView().getItems()`.
`GridListDataView#getItems()` applies filter and sorting, but it exists only
for in-memory data.
**Workaround used:** `MissingAPI#rowsInViewOrder(grid)` fetches
`dataProvider.fetch(dataCommunicator.buildQuery(0, Integer.MAX_VALUE))`, the
only query that has both the filter and the sort order.
**Suggested API:** `Grid#getItemsInViewOrder()` (or an exporter method) that
works for every data provider type and is documented as "filtered and sorted
as displayed".
Upstream: https://github.com/vaadin/platform/issues/7196

## 3. A formatted cell costs a component, and you never get both the value and the text

**Where it bites:** UC4 / RenderedValuesView.java, UC5 / ExcelReportView.java
**Symptom:** `NumberRenderer` and `LocalDateRenderer` know the formatted
string, but `BasicRenderer#getFormattedValue` and
`BasicRenderer#getValueProvider` are `protected`. The only public way in is
`createComponent(item)`, which builds a `Span` per cell just to read its text
back: 24 500 throwaway components per formatted column in a 24 500-row export.
The result is only the formatted string, so UC5 writes `"45,000.00"` into the
`.xlsx` as text instead of the number `45000.0` that Excel can sum.
Building the `Span` also needs a current UI. A `DownloadHandler` has none, so
reading the grid there fails with "No currently active UI found" unless it is
wrapped in `event.getUI().accessSynchronously(…)`. That method takes only a
`Command`, so the result has to come out through an `AtomicReference`.
**Workaround used:** `MissingAPI#cellText` sends `BasicRenderer` subclasses
through `createComponent(item)` and reads the element text. UC5 computes its
totals from the items, not from the exported cells. The download handlers
build the report through `GridExport#exportFor`, which runs inside
`UI#accessSynchronously`.
**Suggested API:** make `BasicRenderer#getFormattedValue` and
`#getValueProvider` public, or return both from `Column#getCellContent(item)`,
e.g. as a `CellContent(Object value, String text)`, so a writer can pick the
one it needs.

## 4. A multi-property `LitRenderer` column cannot be read

**Where it bites:** UC4 / RenderedValuesView.java (the "Contact" column)
**Symptom:** `LitRenderer#getValueProviders()` is public, which is enough for a
column with one property. With two or more, nothing says which ones the cell
shows or in what order: the template is only in the `protected`
`LitRenderer#getTemplateExpression()`. The exporter sees that `name` and
`country` exist, but not that the cell reads `${item.name} · ${item.country}`.
**Workaround used:** `MissingAPI#cellText` returns `null` here, and
`GridExport` turns that into an error that names the column and asks for
`withColumnExtractor(column, item -> ...)`. The view shows that error on a
button press.
**Suggested API:** make `LitRenderer#getTemplateExpression()` public, or expose
an ordered map of properties together with the template, so a plain-text form
can be derived.

## 5. No text alternative for a rendered component

**Where it bites:** UC4 / RenderedValuesView.java, UC6 / RedactedExportView.java
**Symptom:** `ComponentRenderer` has no way to say what the text of a cell is.
A `Checkbox` has no text; #7196 suggests "Yes"/"No", an `Icon` its `icon`
attribute, a `Button` its caption. These are reasonable guesses, but the
framework states none of them, so every exporter guesses differently, and the
guesses can differ from what a screen reader says for the same cell.
Reading the text of a component tree also has a trap: `Element#getText()`
returns the direct text nodes, and `Element#getChildren()` includes the same
text nodes, so a simple recursion emits every string twice ("Active Active").
**Workaround used:** `MissingAPI#componentText(component)`, a hand-written
walker with special cases for `Checkbox`, `Icon`, `Avatar`, `Anchor` and
`HasValue` that skips text-node children.
**Suggested API:** a text alternative declared next to the rendering, e.g.
`ComponentRenderer#withTextAlternative(SerializableFunction<SOURCE, String>)`,
with the defaults from #7196's acceptance criterion 1, and also used as the
cell's accessible name.
Upstream: https://github.com/vaadin/platform/issues/7196

## 6. `getSelectedItems()` is an unordered `Set`

**Where it bites:** UC2 / ExportSelectionView.java
**Symptom:** `Grid#getSelectedItems()` returns a `Set<T>`. A report must list
its rows in the order the user saw them. Exporting the set directly gives the
rows in hash order, which looks like a bug and is not caught by a test that
only checks row contents.
**Workaround used:** `GridExport#selectedRowsOnly()` reads all rows in grid
order and uses the selection only as a filter: a full read of the data
provider to order a few rows.
**Suggested API:** `Grid#getSelectedItemsInViewOrder()`, or an exporter
"selected rows only" option that keeps the grid order.

## 7. `Grid#getColumns()` does not reflect a user's drag reorder

**Where it bites:** UC3 / ColumnChoiceView.java
**Symptom:** with `setColumnReorderingAllowed(true)` the user can drag columns
into a new order, but `Grid#getColumns()` still returns the declaration order.
The new order exists only in the `ColumnReorderEvent` fired at drag time, so
the application has to keep its own copy. Column visibility needs no such
work: `Column` is a `Component`, so `isVisible()` works.
**Workaround used:** UC3 keeps a `List<Grid.Column<Employee>> userOrder`,
seeds it from `getColumns()`, rewrites it on every `ColumnReorderEvent` and
passes it to `GridExport#withColumnOrder(...)`. The view prints both orders
side by side.
**Suggested API:** let the client's reorder update the grid's own column
order, so `getColumns()` is the single source of truth, or add
`Grid#getColumnsInDisplayOrder()`.
Upstream: https://github.com/vaadin/flow-components/issues/1315

## 8. Header and footer cells have no public common supertype, and spans are only visible through cell identity

**Where it bites:** UC5 / ExcelReportView.java
**Symptom:** `HeaderRow.HeaderCell` and `FooterRow.FooterCell` have the same
API (`getText()`, `getComponent()`) and share `AbstractRow.AbstractCell`, but
`AbstractRow` is package-private. `GridExport` therefore has its `cellOf(...)`
and `text(...)` methods twice, once per cell type.
A joined header cell also does not say which columns it covers:
`AbstractCell#getColumn()` is `protected`, and
`ColumnGroup#getChildColumns()` needs the group first. The only clue is that
`row.getCell(column)` returns the same cell instance for every column of a
join.
**Workaround used:** `GridExport#headerRows(...)` groups consecutive columns
by cell identity into `(text, span)` pairs, which `XlsxWriter` turns into
`CellRangeAddress` merges. The `text(...)` overloads exist once for header
cells and once for footer cells.
**Suggested API:** make `AbstractRow` and `AbstractRow.AbstractCell` public
(or add a public `GridCell` interface), and add `AbstractCell#getColumns()`
that returns the columns the cell spans.

## 9. No per-column extractor or converter hook

**Where it bites:** UC6 / RedactedExportView.java
**Symptom:** an export is rarely a literal copy of the screen: drop the
internal id column, mask a card number, write a boolean as "Yes"/"No". Grid
has no place for such per-column rules, so every exporter grows its own
"which column is this" switch, often keyed on something fragile like the
header text.
**Workaround used:** `GridExport#withColumnExtractor` and
`#withColumnConverter`, backed by `IdentityHashMap`s. `Grid.Column` does not
override `equals`/`hashCode`, so identity is the only safe key; the real API
should say so.
**Suggested API:** the builder from #7196's API sketch:
`withColumnExtractor(column, extractor)`,
`withColumnConverter(column, converter)` and `withExcludedColumns(...)`.
Upstream: https://github.com/vaadin/platform/issues/7196

## 10. No paged or streaming export path; `buildQuery` returns a raw `Query`

**Where it bites:** UC7 / LargeDatasetExportView.java
**Symptom:** the grid never loads a large backend into memory, and the export
must not either. `getLazyDataView().getItems()` sends one query with
`limit = Integer.MAX_VALUE`, which is exactly what a 24 500-row export must
avoid. Paging works with `DataCommunicator#buildQuery(offset, limit)`, which
is public and carries the current sort order, but it is not documented as an
export entry point, and it returns a raw `Query`. Passing it to
`grid.getDataProvider().fetch(...)` needs
`@SuppressWarnings({"unchecked", "rawtypes"})`.
**Workaround used:** `MissingAPI#rowsInViewOrder(grid, pageSize)` wraps the
paging in a lazy `Stream`, and `CsvWriter#streamingDownload` writes each page
straight to the response through `DownloadEvent#getWriter()`. UC7 counts the
backend round trips to show that it never fetches more than one page at a
time.
**Suggested API:** a paged or streaming export, e.g.
`exporter.streamRows(pageSize)` returning a lazy `Stream<Row>`, and,
separately, a generically typed `DataCommunicator#buildQuery`.

## 11. A paginating writer needs the static parts many times, and the rows twice

**Where it bites:** UC8 / MultiPagePdfView.java
**Symptom:** CSV and `.xlsx` write the headers once and stream the rows after
them. A printed report must draw all header rows, grouped cells included, at
the top of every page, so it needs them as data it can read again. It also
walks the rows twice: once to measure the text and size the columns, once to
lay them out. Even "Page 1 of 6" needs the row count first.
The API sketch in #7196 returns the report as `List<Row> getRows()` next to
`getHeaderRows()` / `getFooterRows()`. That fits this case, but it is the
opposite of the streaming shape UC7 needs (entry #10). A real API needs both a
lazy `Stream<Row>` and a materialised snapshot, and must say which one each
accessor returns.
**Workaround used:** `GridExport` has two entry points: `export()` for a
materialised `ExportedGrid` and `streamRows()` for the lazy walk. `PdfWriter`
takes the materialised one, measures it, then paginates it.
**Suggested API:** keep the static parts (`getHeaderRows`, `getFooterRows`,
`getColumns`, `getEmptyStateText`) on the exporter itself, independent of the
rows, and offer the rows both ways: `getRows()` for a snapshot and
`streamRows(pageSize)` for a lazy walk.
Upstream: https://github.com/vaadin/platform/issues/7196

## 12. Column widths are CSS strings, so a report cannot size its columns

**Where it bites:** UC8 / MultiPagePdfView.java
**Symptom:** a PDF table needs each column width in points. `Column#getWidth()`
returns a CSS string (`"120px"`, `"8em"`) or `null`. With
`setAutoWidth(true)` the real width is computed by the browser and never
reaches the server, and `getFlexGrow()` is only a ratio. So a report writer
must measure every cell in its own font, which is the second of UC8's two
passes over the data.
Alignment does survive: `ColumnBase#getTextAlign()` returns a
`ColumnTextAlign`, so the money columns in UC8 are right-aligned in the PDF as
on screen. It is the only part of #7196's styling criterion that works today
(see entry #13).
**Workaround used:** `PdfWriter` measures every cell with
`PDFont#getStringWidth`, takes the maximum per column and scales the result to
the page width. `ExportedGrid` carries an `Alignment` per column, mapped from
`getTextAlign()`, so the writers do not depend on Grid types.
**Suggested API:** a resolved width per column in the export data (even an
approximate character count beats a CSS string), or a clear statement in the
export API that widths are the report writer's job.
Upstream: https://github.com/vaadin/platform/issues/7196

## 13. No styling information to export

**Where it bites:** UC5 / ExcelReportView.java
**Symptom:** acceptance criterion 2 of #7196 asks for "border, background and
text colors, width, spaces and other styles", and the issue's own sketch says
it is "not clear what exactly should be collected here". The server knows
`Column#getWidth()`, `getFlexGrow()`, `isAutoWidth()` and the part name
generators, but no resolved colour, border or font: those come from CSS that
the browser resolves against the theme. A `GridStyles` object filled on the
server would be almost empty.
**Workaround used:** none. `XlsxWriter` applies its own styling (bold grey
headers, a bold footer with a top border, merged group cells) instead of
copying the grid.
**Suggested API:** do not try. Export the structure (spans, column order,
alignment, widths) and let the report writer own the look. If visual fidelity
is really the goal, treat it as a client-side snapshot problem, not a
server-side data problem.
Upstream: https://github.com/vaadin/platform/issues/7196

## 14. Not built: hierarchical export and Grid cloning

**Where it bites:** nowhere. These two use cases from #7196 were left out of
the module on purpose, because there is no API to show.
**Symptom:**

- *TreeGrid.* Exporting the rows as the user sees them means the expanded
  nodes, in display order, with their depth. `HierarchicalDataProvider` can
  fetch children level by level, but `TreeGrid` has no public way to list its
  visible rows and gives no depth per item. An exporter would have to walk
  the tree again and rebuild the expanded set.
- *Cloning a grid* (use case [3] of the issue: the same grid with no data, as
  a drag-and-drop target). Column configuration cannot be read back in a form
  that can be applied again: a `Renderer` cannot be attached to a second
  column, and there is no `Column` copy constructor or
  `Grid#copyConfigurationFrom(grid)`.

**Workaround used:** none.
**Suggested API:** for the hierarchy, `TreeGrid#getVisibleItems()` returning
items with their depth. For cloning, renderers that can be reused across
columns, or a `Grid#copyConfiguration()` that covers columns, headers, footers
and widths.
Upstream: https://github.com/vaadin/platform/issues/7196
