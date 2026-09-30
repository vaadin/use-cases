package com.example.uc6;

import com.example.data.Employee;
import com.example.data.Employees;
import com.example.export.CsvWriter;
import com.example.export.ExportedGrid;
import com.example.export.GridExport;
import com.example.views.MainLayout;

import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.streams.DownloadHandler;

/**
 * UC6 — The report is not the screen: redact and convert on the way out.
 * <p>
 * A file that leaves the application is read by people and systems the screen
 * was never designed for, so the export is rarely a literal copy of the grid.
 * Three ordinary demands, all visible below:
 * <ul>
 * <li>The internal database id is on screen because support staff need it to
 * be. It has no business being in a report, so the export drops the
 * column.</li>
 * <li>The corporate card number is masked down to its last four digits.</li>
 * <li>The employment status is a checkbox on screen; the report wants the word
 * "Yes" or "No".</li>
 * </ul>
 * None of this needs new API — it is all ordinary application code. What it
 * needs is somewhere to put it. Without a per-column hook, every exporter in
 * every application grows its own private "which column is this, and what do I
 * do to it" switch. See {@code API-GAPS.md}.
 */
@Route(value = "uc6", layout = MainLayout.class)
@PageTitle("UC6 — Redact on export")
@Menu(order = 6, title = "UC6 — Redact on export")
public class RedactedExportView extends VerticalLayout {

    static final int ROW_COUNT = 20;

    /** What a masked card number looks like. */
    static final String MASK = "•••• •••• •••• ";

    final Grid<Employee> grid = new Grid<>();

    final Checkbox includeInternal = new Checkbox(
            "Include the internal id column (don't, in a real report)");

    private final Grid.Column<Employee> idColumn;

    private final Grid.Column<Employee> cardColumn;

    private final Pre preview = new Pre();

    public RedactedExportView() {
        setSizeFull();
        add(new H1("UC6 — Redact and convert on export"));
        add(new Paragraph("The grid shows the internal id and the full card "
                + "number because the people using the application need them. "
                + "The report does not: the id column is dropped, the card "
                + "number is masked to its last four digits, and the status "
                + "checkbox becomes Yes or No."));

        idColumn = grid.addColumn(Employee::id).setHeader("Id").setKey("id")
                .setAutoWidth(true);
        grid.addColumn(Employee::name).setHeader("Name").setKey("name")
                .setAutoWidth(true);
        grid.addColumn(Employee::department).setHeader("Department")
                .setKey("department").setAutoWidth(true);
        cardColumn = grid.addColumn(Employee::cardNumber)
                .setHeader("Card number").setKey("card").setAutoWidth(true);
        grid.addColumn(new ComponentRenderer<>(RedactedExportView::activeBox))
                .setHeader("Active").setKey("active").setAutoWidth(true);
        grid.setItems(Employees.sample(ROW_COUNT));
        grid.addClassName("export-grid");

        includeInternal.addValueChangeListener(event -> preview.setText(csv()));

        DownloadHandler csv = CsvWriter.download("employees-report.csv",
                this::report);
        Anchor download = new Anchor(csv, "Download employees-report.csv");

        add(grid, includeInternal, download, new H2("Preview"), preview);
        preview.addClassName("export-preview");
        preview.setText(csv());
    }

    private static Checkbox activeBox(Employee employee) {
        Checkbox checkbox = new Checkbox(employee.active());
        checkbox.setReadOnly(true);
        checkbox.setAriaLabel(employee.active() ? "Active" : "Not active");
        return checkbox;
    }

    /** Masks everything but the last four digits of a card number. */
    static String maskCardNumber(String cardNumber) {
        String digits = cardNumber.replaceAll("\\D", "");
        if (digits.length() < 4) {
            return MASK.strip();
        }
        return MASK + digits.substring(digits.length() - 4);
    }

    /** The redacted report. */
    ExportedGrid report() {
        GridExport<Employee> export = GridExport.of(grid).withTitle("Employees")
                .withColumnConverter(cardColumn,
                        RedactedExportView::maskCardNumber);
        if (!includeInternal.getValue()) {
            export.excludeColumns(idColumn);
        }
        return export.export();
    }

    /** Test seam: the CSV the download link would produce. */
    String csv() {
        return CsvWriter.toCsv(report());
    }
}
