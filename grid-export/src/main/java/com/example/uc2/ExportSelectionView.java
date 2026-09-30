package com.example.uc2;

import com.example.data.Employee;
import com.example.data.Employees;
import com.example.export.CsvWriter;
import com.example.export.ExportedGrid;
import com.example.export.GridExport;
import com.example.views.MainLayout;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.streams.DownloadHandler;

/**
 * UC2 — Export only the rows the user picked.
 * <p>
 * Tick a few rows and the export contains exactly those, and nothing else. Sort
 * the grid and the exported rows follow the new order — which is the subtle
 * part: {@link Grid#getSelectedItems()} hands back an unordered {@code Set}, so
 * exporting it directly produces a report whose row order is whatever the set's
 * iteration order happens to be. A report is a document; its rows have to come
 * out in the order the user saw them.
 * <p>
 * The fix is to keep the grid's own row order as the spine and use the
 * selection only as a filter, which is what {@code selectedRowsOnly()} does.
 * See {@code API-GAPS.md}.
 */
@Route(value = "uc2", layout = MainLayout.class)
@PageTitle("UC2 — Export the selection")
@Menu(order = 2, title = "UC2 — Export the selection")
public class ExportSelectionView extends VerticalLayout {

    static final int ROW_COUNT = 40;

    final Grid<Employee> grid = new Grid<>();

    private final Span status = new Span();

    private final Pre preview = new Pre();

    public ExportSelectionView() {
        setSizeFull();
        add(new H1("UC2 — Export only the selected rows"));
        add(new Paragraph("Tick some rows: the preview below holds just "
                + "those, in the grid's row order. Re-sort the grid without "
                + "changing the selection and the exported rows are reordered "
                + "too — the selection decides which rows, the grid decides "
                + "in what order."));

        grid.setSelectionMode(Grid.SelectionMode.MULTI);
        grid.addColumn(Employee::name).setHeader("Name").setKey("name")
                .setSortable(true).setAutoWidth(true);
        grid.addColumn(Employee::department).setHeader("Department")
                .setKey("department").setSortable(true).setAutoWidth(true);
        grid.addColumn(Employee::country).setHeader("Country").setKey("country")
                .setSortable(true).setAutoWidth(true);
        grid.addColumn(Employee::salary).setHeader("Salary").setKey("salary")
                .setSortable(true).setAutoWidth(true);
        grid.setEmptyStateText("No rows selected.");
        grid.setItems(Employees.sample(ROW_COUNT));
        grid.addClassName("export-grid");

        grid.addSelectionListener(event -> refresh());
        grid.addSortListener(event -> refresh());

        DownloadHandler csv = CsvWriter.download("selected-employees.csv",
                this::report);
        Anchor download = new Anchor(csv, "Download the selection as CSV");

        status.addClassName("export-note");
        add(grid, status, download, new H2("Preview"), preview);
        preview.addClassName("export-preview");
        refresh();
    }

    /** The report for the current selection. */
    ExportedGrid report() {
        return GridExport.of(grid).withTitle("Selected employees")
                .selectedRowsOnly().export();
    }

    /** Test seam: the CSV the download link would produce. */
    String csv() {
        return CsvWriter.toCsv(report());
    }

    private void refresh() {
        int selected = grid.getSelectedItems().size();
        status.setText(selected == 0
                ? "Nothing selected — the report would carry only the grid's "
                        + "empty-state text."
                : selected + " of " + ROW_COUNT + " rows selected.");
        preview.setText(csv());
    }
}
