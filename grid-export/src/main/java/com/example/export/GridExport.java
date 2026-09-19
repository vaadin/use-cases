package com.example.export;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import com.example.MissingAPI;
import com.example.export.ExportedGrid.Alignment;
import com.example.export.ExportedGrid.HeaderCell;
import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.FooterRow;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.function.SerializableFunction;

/**
 * Reads a {@link Grid} for export, as the single facade asked for in
 * <a href="https://github.com/vaadin/platform/issues/7196">vaadin/platform
 * #7196</a> — and hand-written here, because Flow has no such facade yet.
 * <p>
 * The builder deliberately follows the shape sketched in that issue:
 *
 * <pre>
 * ExportedGrid report = GridExport.of(grid).withTitle("Employees")
 *         .selectedRowsOnly().excludeColumns(idColumn)
 *         .withColumnConverter(cardColumn, GridExport::mask).export();
 * </pre>
 *
 * What it does <em>not</em> do is style: the issue's "keep as much styling as
 * possible" goal needs per-cell style information that the server does not
 * have. See {@code API-GAPS.md}.
 *
 * @param <T>
 *            the grid's item type
 */
public final class GridExport<T> {

    private final Grid<T> grid;

    private final Set<Grid.Column<T>> excluded = new LinkedHashSet<>();

    /**
     * Identity maps: {@code Grid.Column} does not override
     * {@code equals}/{@code hashCode}, and an identity map says that on
     * purpose.
     */
    private final Map<Grid.Column<T>, SerializableFunction<T, String>> extractors = new IdentityHashMap<>();

    private final Map<Grid.Column<T>, SerializableFunction<String, String>> converters = new IdentityHashMap<>();

    private String title = "";

    private boolean selectedRowsOnly;

    private boolean includeHeaders = true;

    private boolean includeFooters = true;

    private int pageSize;

    private @Nullable List<Grid.Column<T>> columnOrder;

    private GridExport(Grid<T> grid) {
        this.grid = grid;
    }

    /** Starts an export of {@code grid}. */
    public static <T> GridExport<T> of(Grid<T> grid) {
        return new GridExport<>(grid);
    }

    /** Sets the report title. */
    public GridExport<T> withTitle(String title) {
        this.title = title;
        return this;
    }

    /**
     * Exports only the rows the user has selected, in the order the grid shows
     * them — {@link Grid#getSelectedItems()} returns an unordered set, so the
     * view order has to be restored from the data provider.
     */
    public GridExport<T> selectedRowsOnly() {
        this.selectedRowsOnly = true;
        return this;
    }

    /** Leaves the given columns out of the report. */
    @SafeVarargs
    public final GridExport<T> excludeColumns(Grid.Column<T>... columns) {
        for (Grid.Column<T> column : columns) {
            excluded.add(column);
        }
        return this;
    }

    /**
     * Exports the given columns, in the given order, instead of
     * {@link Grid#getColumns()}. Needed whenever the user has reordered the
     * columns by dragging: the grid keeps reporting the declaration order, so
     * only the application's own {@code ColumnReorderEvent} bookkeeping knows
     * what the user sees.
     */
    public GridExport<T> withColumnOrder(List<Grid.Column<T>> columns) {
        this.columnOrder = List.copyOf(columns);
        return this;
    }

    /**
     * Supplies the text for a column whose renderer keeps its value out of
     * reach — the escape hatch for multi-property {@code LitRenderer} columns.
     */
    public GridExport<T> withColumnExtractor(Grid.Column<T> column,
            SerializableFunction<T, String> extractor) {
        extractors.put(column, extractor);
        return this;
    }

    /**
     * Post-processes a column's exported text: masking, redaction, or a
     * different representation than the one on screen.
     */
    public GridExport<T> withColumnConverter(Grid.Column<T> column,
            SerializableFunction<String, String> converter) {
        converters.put(column, converter);
        return this;
    }

    /** Leaves the grid's header rows out of the report. */
    public GridExport<T> withoutHeaders() {
        this.includeHeaders = false;
        return this;
    }

    /** Leaves the grid's footer rows out of the report. */
    public GridExport<T> withoutFooters() {
        this.includeFooters = false;
        return this;
    }

    /**
     * Fetches the rows {@code pageSize} at a time instead of all at once, so a
     * report over a large backend never holds more than one page in memory.
     * Only affects {@link #streamRows()}.
     */
    public GridExport<T> withPageSize(int pageSize) {
        this.pageSize = pageSize;
        return this;
    }

    /** The columns this export covers, in export order. */
    public List<Grid.Column<T>> columns() {
        List<Grid.Column<T>> base = columnOrder != null ? columnOrder
                : grid.getColumns();
        return base.stream().filter(Component::isVisible)
                .filter(column -> !excluded.contains(column)).toList();
    }

    /** One header label per exported column. */
    public List<String> columnHeaders() {
        return columns().stream().map(MissingAPI::headerText).toList();
    }

    /**
     * The data rows as text, lazily. With {@link #withPageSize(int)} set, the
     * rows are fetched page by page as the stream is consumed.
     */
    public Stream<List<String>> streamRows() {
        List<Grid.Column<T>> columns = columns();
        Stream<T> items = pageSize > 0
                ? MissingAPI.rowsInViewOrder(grid, pageSize)
                : MissingAPI.rowsInViewOrder(grid).stream();
        if (selectedRowsOnly) {
            Set<T> selected = grid.getSelectedItems();
            items = items.filter(selected::contains);
        }
        return items.map(item -> row(columns, item));
    }

    /** Reads the whole grid into an {@link ExportedGrid}. */
    public ExportedGrid export() {
        List<Grid.Column<T>> columns = columns();
        return new ExportedGrid(title,
                includeHeaders ? headerRows(columns) : List.of(),
                columns.stream().map(MissingAPI::headerText).toList(),
                columns.stream().map(GridExport::alignmentOf).toList(),
                streamRows().toList(),
                includeFooters ? footerRows(columns) : List.of(),
                emptyStateText());
    }

    private static Alignment alignmentOf(Grid.Column<?> column) {
        ColumnTextAlign align = column.getTextAlign();
        if (align == null) {
            return Alignment.START;
        }
        return switch (align) {
        case END -> Alignment.END;
        case CENTER -> Alignment.CENTER;
        default -> Alignment.START;
        };
    }

    private List<String> row(List<Grid.Column<T>> columns, T item) {
        return columns.stream().map(column -> cell(column, item)).toList();
    }

    private String cell(Grid.Column<T> column, T item) {
        SerializableFunction<T, String> extractor = extractors.get(column);
        String text = extractor != null ? extractor.apply(item)
                : MissingAPI.cellText(column, item);
        if (text == null) {
            throw new IllegalStateException("Column '"
                    + MissingAPI.headerText(column)
                    + "' uses a renderer whose cell text cannot be read; call "
                    + "withColumnExtractor(column, item -> ...) for it");
        }
        SerializableFunction<String, String> converter = converters.get(column);
        return converter != null ? converter.apply(text) : text;
    }

    /**
     * Builds the header matrix. A joined header cell is the <em>same</em> cell
     * instance for every column it covers, so consecutive columns that map to
     * the same instance are collapsed into one cell with a span — the only way
     * to recover the spans, since the cell-to-column mapping itself is
     * protected.
     */
    private List<List<HeaderCell>> headerRows(List<Grid.Column<T>> columns) {
        List<List<HeaderCell>> result = new ArrayList<>();
        for (HeaderRow row : grid.getHeaderRows()) {
            List<HeaderCell> cells = new ArrayList<>();
            HeaderRow.HeaderCell previous = null;
            for (Grid.Column<T> column : columns) {
                HeaderRow.HeaderCell cell = cellOf(row, column);
                if (cell != null && cell == previous && !cells.isEmpty()) {
                    HeaderCell last = cells.removeLast();
                    cells.add(new HeaderCell(last.text(), last.span() + 1));
                } else {
                    cells.add(new HeaderCell(text(cell), 1));
                    previous = cell;
                }
            }
            result.add(List.copyOf(cells));
        }
        return List.copyOf(result);
    }

    private List<List<String>> footerRows(List<Grid.Column<T>> columns) {
        List<List<String>> result = new ArrayList<>();
        for (FooterRow row : grid.getFooterRows()) {
            List<String> cells = columns.stream()
                    .map(column -> text(cellOf(row, column))).toList();
            if (cells.stream().anyMatch(cell -> !cell.isEmpty())) {
                result.add(cells);
            }
        }
        return List.copyOf(result);
    }

    // The next four methods are the same method twice over. Header cells and
    // footer cells share an API (getText(), getComponent()) and a supertype
    // (AbstractRow.AbstractCell), but that supertype is package-private, so
    // there is nothing to write the shared version against. See API-GAPS.md.

    private static HeaderRow.@Nullable HeaderCell cellOf(HeaderRow row,
            Grid.Column<?> column) {
        try {
            return row.getCell(column);
        } catch (RuntimeException e) {
            // A column added after the row was created has no cell in it.
            return null;
        }
    }

    private static FooterRow.@Nullable FooterCell cellOf(FooterRow row,
            Grid.Column<?> column) {
        try {
            return row.getCell(column);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static String text(HeaderRow.@Nullable HeaderCell cell) {
        if (cell == null) {
            return "";
        }
        String text = cell.getText();
        if (text != null && !text.isBlank()) {
            return text;
        }
        return MissingAPI.componentText(cell.getComponent());
    }

    private static String text(FooterRow.@Nullable FooterCell cell) {
        if (cell == null) {
            return "";
        }
        String text = cell.getText();
        if (text != null && !text.isBlank()) {
            return text;
        }
        return MissingAPI.componentText(cell.getComponent());
    }

    private String emptyStateText() {
        String text = grid.getEmptyStateText();
        if (text != null && !text.isBlank()) {
            return text;
        }
        return MissingAPI.componentText(grid.getEmptyStateComponent());
    }
}
