package com.example.uc3;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import com.example.MissingAPI;
import com.example.data.Employee;
import com.example.data.Employees;
import com.example.export.CsvWriter;
import com.example.export.ExportedGrid;
import com.example.export.GridExport;
import com.example.views.MainLayout;

import com.vaadin.flow.component.checkbox.CheckboxGroup;
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
 * UC3 — The export follows the user's column choices.
 * <p>
 * The user owns the table: they hide the columns they don't care about and drag
 * the rest into the order they want. A report that ignores those choices is the
 * wrong report. Untick a column below, or drag a column header, and the preview
 * follows.
 * <p>
 * Hiding works out of the box — {@code Column} is a {@code Component}, so
 * {@code isVisible()} tells the exporter what to skip. Reordering does not:
 * after a drag, {@link Grid#getColumns()} still returns the declaration order.
 * The only place the user's order exists is the {@code ColumnReorderEvent}, so
 * this view keeps its own copy of the order and hands it to the exporter. See
 * {@code API-GAPS.md}.
 */
@Route(value = "uc3", layout = MainLayout.class)
@PageTitle("UC3 — The user's column choices")
@Menu(order = 3, title = "UC3 — The user's column choices")
public class ColumnChoiceView extends VerticalLayout {

    static final int ROW_COUNT = 30;

    final Grid<Employee> grid = new Grid<>();

    final CheckboxGroup<Grid.Column<Employee>> columnPicker = new CheckboxGroup<>();

    /**
     * The column order as the user sees it. Seeded from the grid and then kept
     * up to date from {@code ColumnReorderEvent}, because the grid itself keeps
     * reporting the declaration order.
     */
    private final List<Grid.Column<Employee>> userOrder = new ArrayList<>();

    private final Span orderLabel = new Span();

    private final Pre preview = new Pre();

    public ColumnChoiceView() {
        setSizeFull();
        add(new H1("UC3 — The export follows the user's column choices"));
        add(new Paragraph("Untick a column to hide it, or drag a column "
                + "header to move it. The preview — and the downloaded file — "
                + "shows exactly the columns you left visible, in the order "
                + "you put them in."));

        grid.addColumn(Employee::name).setHeader("Name").setKey("name")
                .setAutoWidth(true);
        grid.addColumn(Employee::department).setHeader("Department")
                .setKey("department").setAutoWidth(true);
        grid.addColumn(Employee::country).setHeader("Country").setKey("country")
                .setAutoWidth(true);
        grid.addColumn(Employee::hired).setHeader("Hired").setKey("hired")
                .setAutoWidth(true);
        grid.addColumn(Employee::salary).setHeader("Salary").setKey("salary")
                .setAutoWidth(true);
        grid.setItems(Employees.sample(ROW_COUNT));
        grid.setColumnReorderingAllowed(true);
        grid.addClassName("export-grid");

        userOrder.addAll(grid.getColumns());
        grid.addColumnReorderListener(event -> {
            userOrder.clear();
            userOrder.addAll(event.getColumns());
            refresh();
        });

        columnPicker.setLabel("Columns");
        columnPicker.setItems(grid.getColumns());
        columnPicker.setItemLabelGenerator(MissingAPI::headerText);
        columnPicker.setValue(new LinkedHashSet<>(grid.getColumns()));
        columnPicker.addValueChangeListener(event -> {
            grid.getColumns().forEach(column -> column
                    .setVisible(event.getValue().contains(column)));
            refresh();
        });

        DownloadHandler csv = CsvWriter.download("employees.csv", this::report);
        Anchor download = new Anchor(csv, "Download the chosen columns");

        orderLabel.addClassName("export-note");
        add(columnPicker, grid, orderLabel, download, new H2("Preview"),
                preview);
        preview.addClassName("export-preview");
        refresh();
    }

    /** The report for the columns the user left visible, in their order. */
    ExportedGrid report() {
        return GridExport.of(grid).withTitle("Employees")
                .withColumnOrder(userOrder).export();
    }

    /** Test seam: the CSV the download link would produce. */
    String csv() {
        return CsvWriter.toCsv(report());
    }

    private void refresh() {
        orderLabel.setText(
                "Export order: " + String.join(" · ", report().columnHeaders())
                        + " — the grid still reports "
                        + String.join(" · ", grid.getColumns().stream()
                                .map(MissingAPI::headerText).toList()));
        preview.setText(csv());
    }
}
