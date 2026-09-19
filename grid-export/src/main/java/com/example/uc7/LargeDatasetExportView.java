package com.example.uc7;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import com.example.data.Employee;
import com.example.data.Employees;
import com.example.export.CsvWriter;
import com.example.export.GridExport;
import com.example.views.MainLayout;
import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.data.provider.QuerySortOrder;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.streams.DownloadHandler;

/**
 * UC7 — Exporting a large, lazily-loaded data set.
 * <p>
 * The grid is backed by a callback data provider over {@value #ROW_COUNT} rows
 * and never holds more than a couple of pages in the browser. The export must
 * behave the same way: pull the rows from the backend {@value #PAGE_SIZE} at a
 * time, write each page straight to the response, and never assemble the whole
 * report in memory. Run the export and the counter shows the number of backend
 * round trips it took.
 * <p>
 * Flow has no export entry point for this. What it has is
 * {@code DataCommunicator#buildQuery(offset, limit)}, which returns the raw
 * {@code Query} the grid would send — including the current sorting — so an
 * exporter can page through the backend by asking for one window at a time. The
 * single-shot alternative, {@code getLazyDataView().getItems()}, asks the
 * backend for {@code Integer.MAX_VALUE} rows in one query, which is exactly
 * what a large export must not do. See {@code API-GAPS.md}.
 */
@Route(value = "uc7", layout = MainLayout.class)
@PageTitle("UC7 — Large lazy data set")
@Menu(order = 7, title = "UC7 — Large lazy data set")
public class LargeDatasetExportView extends VerticalLayout {

    /** Rows in the simulated backend. Deliberately not a whole page count. */
    static final int ROW_COUNT = 24_500;

    /** How many CSV lines the preview keeps. */
    static final int PREVIEW_LINES = 10;

    /** How many rows each backend round trip fetches. */
    static final int PAGE_SIZE = 1_000;

    /** The simulated backend the grid and the export both read from. */
    static final List<Employee> BACKEND = Employees.sample(ROW_COUNT);

    /**
     * What one export run measured.
     *
     * @param rows
     *            data rows written
     * @param fetches
     *            backend round trips the run took
     * @param csvHead
     *            the first few lines of the generated CSV
     */
    record ExportRun(long rows, int fetches, String csvHead) {
    }

    final Grid<Employee> grid = new Grid<>();

    final Button runExport = new Button("Run the export now");

    private final AtomicInteger fetches = new AtomicInteger();

    private final Span stats = new Span();

    private final Pre preview = new Pre();

    public LargeDatasetExportView() {
        setSizeFull();
        add(new H1("UC7 — Exporting a large, lazily-loaded data set"));
        add(new Paragraph("The grid below is backed by " + ROW_COUNT
                + " rows that live in a simulated backend. Sort a column, then "
                + "run the export: it walks the backend in pages of "
                + PAGE_SIZE + " rows, in the sort order you chose, and the "
                + "counter shows how many round trips that took."));

        grid.addColumn(Employee::name).setHeader("Name").setKey("name")
                .setSortProperty("name").setAutoWidth(true);
        grid.addColumn(Employee::department).setHeader("Department")
                .setKey("department").setSortProperty("department")
                .setAutoWidth(true);
        grid.addColumn(Employee::country).setHeader("Country").setKey("country")
                .setSortProperty("country").setAutoWidth(true);
        grid.addColumn(Employee::salary).setHeader("Salary").setKey("salary")
                .setSortProperty("salary").setAutoWidth(true);
        grid.setItems(this::fetch, query -> BACKEND.size());
        grid.addClassName("export-grid");

        runExport.addClickListener(event -> showRun(runExport()));

        DownloadHandler csv = CsvWriter.streamingDownload("all-employees.csv",
                () -> exporter().columnHeaders(),
                () -> exporter().streamRows());
        Anchor download = new Anchor(csv, "Stream all-employees.csv");

        stats.addClassName("export-note");
        add(grid, runExport, download, stats, new H2("Preview"), preview);
        preview.addClassName("export-preview");
        stats.setText("The export has not been run yet.");
    }

    /** The exporter used both by the download link and by the button. */
    private GridExport<Employee> exporter() {
        return GridExport.of(grid).withPageSize(PAGE_SIZE);
    }

    /**
     * Runs the export into a counting sink, measuring the round trips it takes.
     * Test seam as well as the button's action.
     */
    ExportRun runExport() {
        int before = fetches.get();
        GridExport<Employee> exporter = exporter();
        StringBuilder head = new StringBuilder();
        head.append(CsvWriter.toLine(exporter.columnHeaders())).append('\n');
        long rows = 0;
        // Consumed row by row: only the first lines are kept for the preview,
        // the rest are counted and forgotten, as a streaming export would
        // write and forget them.
        for (List<String> row : (Iterable<List<String>>) exporter
                .streamRows()::iterator) {
            rows++;
            if (rows <= PREVIEW_LINES) {
                head.append(CsvWriter.toLine(row)).append('\n');
            }
        }
        if (rows > PREVIEW_LINES) {
            head.append("… and ").append(rows - PREVIEW_LINES)
                    .append(" more rows\n");
        }
        return new ExportRun(rows, fetches.get() - before, head.toString());
    }

    /** Test seam: backend round trips so far. */
    int fetchCount() {
        return fetches.get();
    }

    private void showRun(ExportRun run) {
        stats.setText("Exported " + run.rows() + " rows in " + run.fetches()
                + " backend round trips of up to " + PAGE_SIZE + " rows.");
        preview.setText(run.csvHead());
    }

    private Stream<Employee> fetch(Query<Employee, Void> query) {
        fetches.incrementAndGet();
        Comparator<Employee> comparator = comparator(query.getSortOrders());
        Stream<Employee> rows = BACKEND.stream();
        if (comparator != null) {
            rows = rows.sorted(comparator);
        }
        return rows.skip(query.getOffset()).limit(query.getLimit());
    }

    private static @Nullable Comparator<Employee> comparator(
            List<QuerySortOrder> sortOrders) {
        Comparator<Employee> result = null;
        for (QuerySortOrder order : sortOrders) {
            Comparator<Employee> next = switch (order.getSorted()) {
            case "name" -> Comparator.comparing(Employee::name);
            case "department" -> Comparator.comparing(Employee::department);
            case "country" -> Comparator.comparing(Employee::country);
            case "salary" -> Comparator.comparingDouble(Employee::salary);
            default -> null;
            };
            if (next == null) {
                continue;
            }
            if (order.getDirection() == SortDirection.DESCENDING) {
                next = next.reversed();
            }
            result = result == null ? next : result.thenComparing(next);
        }
        return result;
    }
}
