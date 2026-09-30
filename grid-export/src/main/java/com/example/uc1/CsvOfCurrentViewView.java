package com.example.uc1;

import java.util.Locale;

import com.example.data.Employee;
import com.example.data.Employees;
import com.example.export.CsvWriter;
import com.example.export.ExportedGrid;
import com.example.export.GridExport;
import com.example.views.MainLayout;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.streams.DownloadHandler;

/**
 * UC1 — "Export what I see" as CSV.
 * <p>
 * The simplest and most common export: no formatting, no styling, no
 * customisation — just the rows the grid is showing right now, in the order it
 * is showing them, as plain text. Type in the filter field, click a column
 * header to sort, and the preview below (and the downloaded file) follows.
 * <p>
 * Honouring the filter and the sorting is the part that surprises people. The
 * grid's sorting does not live in the data provider, it lives in the
 * {@code DataCommunicator}, so re-reading the items from the
 * {@code ListDataProvider} gives you the unsorted list. The one query that has
 * both is the one the grid builds for itself, which is what
 * {@code MissingAPI.rowsInViewOrder(grid)} asks for.
 * <p>
 * Every column here is a plain {@code addColumn(ValueProvider)} column — the
 * most ordinary kind there is — and even for those the exporter has to reach
 * into a private field of {@code ColumnPathRenderer} to find out what the cell
 * says. See {@code API-GAPS.md}.
 */
@Route(value = "uc1", layout = MainLayout.class)
@PageTitle("UC1 — CSV of the current view")
@Menu(order = 1, title = "UC1 — CSV of the current view")
public class CsvOfCurrentViewView extends VerticalLayout {

    static final int ROW_COUNT = 60;

    final Grid<Employee> grid = new Grid<>();

    final TextField filter = new TextField();

    private final GridListDataView<Employee> dataView;

    private final Pre preview = new Pre();

    public CsvOfCurrentViewView() {
        setSizeFull();
        add(new H1("UC1 — CSV of the current view"));
        add(new Paragraph("The CSV below is generated from the grid as it is "
                + "right now: the active filter applied, the active sorting "
                + "applied, in the grid's column order. Filter or re-sort and "
                + "watch the preview change."));

        grid.addColumn(Employee::name).setHeader("Name").setKey("name")
                .setSortable(true).setAutoWidth(true);
        grid.addColumn(Employee::department).setHeader("Department")
                .setKey("department").setSortable(true).setAutoWidth(true);
        grid.addColumn(Employee::country).setHeader("Country").setKey("country")
                .setSortable(true).setAutoWidth(true);
        grid.addColumn(Employee::hired).setHeader("Hired").setKey("hired")
                .setSortable(true).setAutoWidth(true);
        grid.addColumn(Employee::salary).setHeader("Salary").setKey("salary")
                .setSortable(true).setAutoWidth(true);
        grid.setEmptyStateText("No employees match the current filter.");
        dataView = grid.setItems(Employees.sample(ROW_COUNT));
        grid.addClassName("export-grid");

        filter.setLabel("Filter by name, department or country");
        filter.setClearButtonVisible(true);
        filter.setValueChangeMode(ValueChangeMode.LAZY);
        filter.addValueChangeListener(event -> {
            dataView.setFilter(this::matchesFilter);
            refreshPreview();
        });
        filter.setWidth("24em");

        // The grid's sort order is what changes the export, so the preview has
        // to follow it.
        grid.addSortListener(event -> refreshPreview());

        DownloadHandler csv = CsvWriter.download("employees.csv", this::report);
        Anchor download = new Anchor(csv, "Download employees.csv");

        add(filter, grid, download, new H2("Preview"), preview);
        preview.addClassName("export-preview");
        refreshPreview();
    }

    private boolean matchesFilter(Employee employee) {
        String term = filter.getValue();
        if (term == null || term.isBlank()) {
            return true;
        }
        String needle = term.toLowerCase(Locale.ROOT);
        return employee.name().toLowerCase(Locale.ROOT).contains(needle)
                || employee.department().toLowerCase(Locale.ROOT)
                        .contains(needle)
                || employee.country().toLowerCase(Locale.ROOT).contains(needle);
    }

    /** The report, read from the grid on every call. */
    ExportedGrid report() {
        return GridExport.of(grid).withTitle("Employees").export();
    }

    /** Test seam: the CSV the download link would produce. */
    String csv() {
        return CsvWriter.toCsv(report());
    }

    private void refreshPreview() {
        preview.setText(csv());
    }
}
