package com.example.uc8;

import java.util.List;
import java.util.Locale;
import java.util.function.ToDoubleFunction;

import com.example.data.Employee;
import com.example.data.Employees;
import com.example.export.ExportedGrid;
import com.example.export.GridExport;
import com.example.export.PdfWriter;
import com.example.views.MainLayout;

import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.FooterRow;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.NumberRenderer;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.streams.DownloadHandler;

/**
 * UC8 — A multi-page PDF with the headers repeated on every page.
 * <p>
 * A printed report is the one format where the header rows are not a one-off.
 * The grid below holds {@value #ROW_COUNT} rows, which is several pages of
 * paper, and the reader of page 4 needs to know what column 3 means just as
 * much as the reader of page 1 — so both header rows, the grouped
 * "Compensation" cell included, are drawn again at the top of every page, with
 * the totals landing once at the end.
 * <p>
 * Pagination is what makes this use case different from CSV and from
 * {@code .xlsx}. Those two write the static parts once and stream the rows past
 * them; a paginating writer needs the header rows available over and over,
 * <em>and</em> it needs to measure every row before it can place the first one
 * — to size the columns and to print a truthful "Page 1 of 12". An export API
 * that only hands out a flat list of rows is not enough for it, and the column
 * widths it needs are not on the server at all. See {@code API-GAPS.md}.
 */
@Route(value = "uc8", layout = MainLayout.class)
@PageTitle("UC8 — Multi-page PDF")
@Menu(order = 8, title = "UC8 — Multi-page PDF")
public class MultiPagePdfView extends VerticalLayout {

    /** Enough rows to need several pages of paper. */
    static final int ROW_COUNT = 200;

    /** The title of the report and of its first page. */
    static final String TITLE = "Compensation report";

    final Grid<Employee> grid = new Grid<>();

    final Span stats = new Span();

    private final Pre preview = new Pre();

    public MultiPagePdfView() {
        setSizeFull();
        add(new H1("UC8 — A multi-page PDF with repeated headers"));
        add(new Paragraph("The report below runs to several pages. Both "
                + "header rows — the grouped one and the column labels — are "
                + "repeated at the top of every page, the money columns stay "
                + "right-aligned as they are in the grid, and the totals "
                + "appear once, after the last row. The preview is the "
                + "generated PDF, read back page by page."));

        Grid.Column<Employee> name = grid.addColumn(Employee::name)
                .setHeader("Name").setKey("name").setAutoWidth(true);
        Grid.Column<Employee> department = grid.addColumn(Employee::department)
                .setHeader("Department").setKey("department")
                .setAutoWidth(true);
        Grid.Column<Employee> salary = grid
                .addColumn(new NumberRenderer<>(Employee::salary, "%,.2f",
                        Locale.US))
                .setHeader("Salary").setKey("salary")
                .setTextAlign(ColumnTextAlign.END).setAutoWidth(true);
        Grid.Column<Employee> bonus = grid
                .addColumn(new NumberRenderer<>(Employee::bonus, "%,.2f",
                        Locale.US))
                .setHeader("Bonus").setKey("bonus")
                .setTextAlign(ColumnTextAlign.END).setAutoWidth(true);

        List<Employee> employees = Employees.sample(ROW_COUNT);
        grid.setItems(employees);
        grid.addClassName("export-grid");

        HeaderRow group = grid.prependHeaderRow();
        group.join(name, department).setText("Employee");
        group.join(salary, bonus).setText("Compensation");

        FooterRow footer = grid.appendFooterRow();
        footer.getCell(name).setText("Total (" + employees.size() + ")");
        footer.getCell(salary).setText(sum(employees, Employee::salary));
        footer.getCell(bonus).setText(sum(employees, Employee::bonus));

        DownloadHandler pdf = PdfWriter.download("compensation.pdf",
                this::report);
        Anchor download = new Anchor(pdf, "Download compensation.pdf");

        stats.addClassName("export-note");
        add(grid, download, stats, new H2("The generated PDF"), preview);
        preview.addClassName("export-preview");
        showPdf();
    }

    private static String sum(List<Employee> employees,
            ToDoubleFunction<Employee> value) {
        return "%,.2f".formatted(employees.stream().mapToDouble(value).sum());
    }

    /** The report, headers and footers included. */
    ExportedGrid report() {
        return GridExport.of(grid).withTitle(TITLE).export();
    }

    /** Test seam: the PDF the download link would produce. */
    byte[] pdf() {
        return PdfWriter.toBytes(report());
    }

    private void showPdf() {
        List<String> pages = PdfWriter.readBackPages(pdf());
        stats.setText(ROW_COUNT + " rows over " + pages.size()
                + " pages, with both header rows on each of them.");
        StringBuilder text = new StringBuilder();
        for (int page = 0; page < pages.size(); page++) {
            text.append("─── page ").append(page + 1).append(" of ")
                    .append(pages.size()).append(" ───\n")
                    .append(pages.get(page)).append('\n');
        }
        preview.setText(text.toString());
    }
}
