package com.example.uc5;

import java.util.List;
import java.util.Locale;
import java.util.function.ToDoubleFunction;

import com.example.data.Employee;
import com.example.data.Employees;
import com.example.export.ExportedGrid;
import com.example.export.GridExport;
import com.example.export.XlsxWriter;
import com.example.views.MainLayout;

import com.vaadin.flow.component.grid.FooterRow;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.NumberRenderer;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.streams.DownloadHandler;

/**
 * UC5 — A spreadsheet report with the grid's headers and footers.
 * <p>
 * The rows are only half of a report. The other half is the static furniture:
 * the title, the grouped header ("Compensation" spanning Salary and Bonus), the
 * column labels underneath it, and the footer row carrying the totals. All of
 * it has to survive into the {@code .xlsx}, with the header group as a real
 * merged cell.
 * <p>
 * Flow does expose the static parts — {@code getHeaderRows()},
 * {@code getFooterRows()}, {@code HeaderRow#getCell(column)} — but not the
 * shape of them: which columns a joined cell covers is only derivable from the
 * fact that the same cell instance answers for each of them, because
 * {@code AbstractCell#getColumn()} is protected. And a cell holding a component
 * has no text at all, so the exporter has to invent a text alternative for it.
 * See {@code API-GAPS.md}.
 */
@Route(value = "uc5", layout = MainLayout.class)
@PageTitle("UC5 — Spreadsheet report")
@Menu(order = 5, title = "UC5 — Spreadsheet report")
public class ExcelReportView extends VerticalLayout {

    static final int ROW_COUNT = 20;

    /** The title of the report, the sheet and the first merged row. */
    static final String TITLE = "Compensation report";

    final Grid<Employee> grid = new Grid<>();

    private final Pre preview = new Pre();

    public ExcelReportView() {
        setSizeFull();
        add(new H1("UC5 — A spreadsheet report, headers and footers included"));
        add(new Paragraph("The grid below has a grouped header and a footer "
                + "row of totals. Both make it into the workbook: the group "
                + "becomes a merged cell above the two columns it covers, and "
                + "the totals become a bold footer row. The preview is the "
                + "generated workbook, read back cell by cell."));

        Grid.Column<Employee> name = grid.addColumn(Employee::name)
                .setHeader("Name").setKey("name").setAutoWidth(true);
        Grid.Column<Employee> department = grid.addColumn(Employee::department)
                .setHeader("Department").setKey("department")
                .setAutoWidth(true);
        Grid.Column<Employee> salary = grid
                .addColumn(new NumberRenderer<>(Employee::salary, "%,.2f",
                        Locale.US))
                .setHeader("Salary").setKey("salary").setAutoWidth(true);
        Grid.Column<Employee> bonus = grid
                .addColumn(new NumberRenderer<>(Employee::bonus, "%,.2f",
                        Locale.US))
                .setHeader("Bonus").setKey("bonus").setAutoWidth(true);

        List<Employee> employees = Employees.sample(ROW_COUNT);
        grid.setItems(employees);
        grid.addClassName("export-grid");

        // A second header row above the column labels, joining the two money
        // columns under one group heading.
        HeaderRow group = grid.prependHeaderRow();
        group.join(name, department).setText("Employee");
        group.join(salary, bonus).setText("Compensation");

        FooterRow footer = grid.appendFooterRow();
        footer.getCell(name).setText("Total (" + employees.size() + ")");
        footer.getCell(salary).setText(sum(employees, Employee::salary));
        footer.getCell(bonus).setText(sum(employees, Employee::bonus));

        DownloadHandler xlsx = XlsxWriter.download("compensation.xlsx",
                this::report);
        Anchor download = new Anchor(xlsx, "Download compensation.xlsx");

        add(grid, download, new H2("The generated workbook"), preview);
        preview.addClassName("export-preview");
        preview.setText(workbookAsText());
    }

    private static String sum(List<Employee> employees,
            ToDoubleFunction<Employee> value) {
        return "%,.2f".formatted(employees.stream().mapToDouble(value).sum());
    }

    /** The report, headers and footers included. */
    ExportedGrid report() {
        return GridExport.of(grid).withTitle(TITLE).export();
    }

    /** Test seam: the workbook the download link would produce. */
    byte[] workbook() {
        return XlsxWriter.toBytes(report());
    }

    private String workbookAsText() {
        return XlsxWriter.readBack(workbook()).stream()
                .map(row -> String.join(" | ", row))
                .reduce((left, right) -> left + "\n" + right).orElse("");
    }
}
