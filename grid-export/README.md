# Grid data export — use cases

A standalone Spring Boot demo of exporting a Vaadin `Grid`: the rows the user
sees, the columns they chose, the headers and footers around them, in CSV and
in `.xlsx`.

Flow has **no** Grid export API —
[vaadin/platform#7196](https://github.com/vaadin/platform/issues/7196) is still
a draft. So every view here is built on a hand-written facade,
`com.example.export.GridExport`, over the primitives in
`com.example.MissingAPI`. That is the point of the module: the use cases are all
achievable today, and [API-GAPS.md](API-GAPS.md) records what each one had to
work around to get there.

| # | View | What it shows |
| - | ---- | ------------- |
| UC1 | CSV of the current view | The rows as displayed — active filter and active sorting applied — as plain CSV. The grid's sort order lives in the `DataCommunicator`, not the data provider, so re-reading the items is not enough. |
| UC2 | Export the selection | Only the ticked rows, in the grid's row order. `getSelectedItems()` is an unordered `Set`, so the order has to be restored from the data provider. |
| UC3 | The user's column choices | Hidden columns dropped, dragged column order kept. Visibility comes free; the drag order exists only in `ColumnReorderEvent`, so the view keeps its own copy. |
| UC4 | Rendered columns as text | One column per renderer kind — badge, checkbox, formatted currency, formatted date, single- and multi-property `LitRenderer`, action button — and the plain-text equivalent of each. |
| UC5 | Spreadsheet report | A styled `.xlsx` with the grid's grouped header as merged cells and the footer aggregates as a bold total row, read back cell by cell in the preview. |
| UC6 | Redact on export | The internal id column dropped, the card number masked to its last four digits, the status checkbox written as Yes/No. |
| UC7 | Large lazy data set | 24 500 rows behind a callback data provider, streamed to the response 1 000 at a time, in the grid's sort order, with the backend round trips counted. |

## Run

```
cd grid-export
mvn spring-boot:run
```

Open <http://localhost:8080/>.

## Tests

```
./mvnw -pl grid-export -am test -DskipFrontend=true
```

One browserless test per use case, plus `CsvWriterTest` for the CSV quoting
rules. UC5's test reads the generated workbook back with Apache POI and asserts
the merged header regions; UC7's asserts that the export pages through the
backend instead of fetching it in one query.

## Dependencies

Apache POI (`poi-ooxml`) for UC5's `.xlsx` output. Acceptance criterion 0 of
issue #7196 asks Vaadin to ship one or two copy-pasteable format examples
alongside the export API; `CsvWriter` and `XlsxWriter` in this module are what
those would look like.
