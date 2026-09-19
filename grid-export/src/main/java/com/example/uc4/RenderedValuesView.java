package com.example.uc4;

import java.util.Locale;

import com.example.data.Employee;
import com.example.data.Employees;
import com.example.export.CsvWriter;
import com.example.export.ExportedGrid;
import com.example.export.GridExport;
import com.example.views.MainLayout;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.LitRenderer;
import com.vaadin.flow.data.renderer.LocalDateRenderer;
import com.vaadin.flow.data.renderer.NumberRenderer;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.streams.DownloadHandler;

/**
 * UC4 — Columns that render something other than plain text.
 * <p>
 * Real grids are full of badges, checkboxes, action buttons, formatted currency
 * and formatted dates. A CSV or a spreadsheet wants the text a human reads in
 * that cell, so every one of those renderers needs a plain-text equivalent.
 * This view has one column per renderer kind and exports them all.
 * <p>
 * Three different stories hide behind the eight columns:
 * <ul>
 * <li>A {@code ComponentRenderer} can be asked to build its component for an
 * item, so the exporter builds the cell and reads its text. It works, but it
 * means instantiating a whole component per cell just to get a string — and
 * what "the text" of a component is, is the exporter's guess: a checkbox
 * becomes "Yes"/"No" here because vaadin/platform#7196 suggests that, not
 * because the checkbox said so.</li>
 * <li>{@code NumberRenderer} and {@code LocalDateRenderer} go the same way,
 * which is how the formatted "45,000.00 EUR" reaches the file. What you cannot
 * get is both: the file gets the formatted string, never the underlying
 * {@code 45000.0} that a spreadsheet would want to sum.</li>
 * <li>A {@code LitRenderer} with a single property can be read from
 * {@code getValueProviders()}. With two properties it cannot: the template is
 * protected, so nothing says which property the cell shows, or in what order.
 * The "Contact" column below is that case, and it needs an explicit extractor —
 * press the button to see what the exporter says without one.</li>
 * </ul>
 * See {@code API-GAPS.md}.
 */
@Route(value = "uc4", layout = MainLayout.class)
@PageTitle("UC4 — Rendered columns as text")
@Menu(order = 4, title = "UC4 — Rendered columns as text")
public class RenderedValuesView extends VerticalLayout {

    static final int ROW_COUNT = 25;

    /** The format the Salary column renders, and therefore exports. */
    static final String SALARY_FORMAT = "%,.2f EUR";

    /** The pattern the Hired column renders, and therefore exports. */
    static final String DATE_PATTERN = "dd.MM.yyyy";

    final Grid<Employee> grid = new Grid<>();

    final Button withoutExtractor = new Button(
            "Export the Contact column without an extractor");

    private final Grid.Column<Employee> contactColumn;

    final Span problem = new Span();

    private final Pre preview = new Pre();

    public RenderedValuesView() {
        setSizeFull();
        add(new H1("UC4 — Rendered columns as text"));
        add(new Paragraph("Every column below renders something different — a "
                + "badge, a checkbox, formatted currency, a formatted date, a "
                + "lightweight Lit template, an action button — and the CSV "
                + "underneath holds the plain-text equivalent of each."));

        grid.addColumn(Employee::name).setHeader("Name").setKey("name")
                .setAutoWidth(true);
        grid.addColumn(new ComponentRenderer<>(RenderedValuesView::statusBadge))
                .setHeader("Status").setKey("status").setAutoWidth(true);
        grid.addColumn(new ComponentRenderer<>(RenderedValuesView::activeBox))
                .setHeader("Active").setKey("active").setAutoWidth(true);
        grid.addColumn(new NumberRenderer<>(Employee::salary, SALARY_FORMAT,
                Locale.US)).setHeader("Salary").setKey("salary")
                .setAutoWidth(true);
        grid.addColumn(new LocalDateRenderer<>(Employee::hired, DATE_PATTERN))
                .setHeader("Hired").setKey("hired").setAutoWidth(true);
        grid.addColumn(LitRenderer.<Employee> of("<span>${item.country}</span>")
                .withProperty("country", Employee::country))
                .setHeader("Country").setKey("country").setAutoWidth(true);
        contactColumn = grid
                .addColumn(LitRenderer
                        .<Employee> of("<span>${item.name} &middot; "
                                + "${item.country}</span>")
                        .withProperty("name", Employee::name)
                        .withProperty("country", Employee::country))
                .setHeader("Contact").setKey("contact").setAutoWidth(true);
        grid.addColumn(new ComponentRenderer<>(RenderedValuesView::editButton))
                .setHeader("Actions").setKey("actions").setAutoWidth(true);
        grid.setItems(Employees.sample(ROW_COUNT));
        grid.addClassName("export-grid");

        DownloadHandler csv = CsvWriter.download("rendered-employees.csv",
                this::report);
        Anchor download = new Anchor(csv, "Download rendered-employees.csv");

        withoutExtractor.addThemeVariants(ButtonVariant.TERTIARY);
        withoutExtractor.addClickListener(
                event -> problem.setText(reportWithoutExtractorMessage()));
        problem.addClassName("export-note");

        add(grid, download, withoutExtractor, problem, new H2("Preview"),
                preview);
        preview.addClassName("export-preview");
        preview.setText(csv());
    }

    private static Span statusBadge(Employee employee) {
        Span badge = new Span(employee.active() ? "Active" : "On leave");
        badge.getElement().getThemeList().add("badge");
        return badge;
    }

    private static Checkbox activeBox(Employee employee) {
        Checkbox checkbox = new Checkbox(employee.active());
        checkbox.setReadOnly(true);
        checkbox.setAriaLabel(employee.active() ? "Active" : "Not active");
        return checkbox;
    }

    private static Button editButton(Employee employee) {
        Button edit = new Button("Edit", VaadinIcon.EDIT.create());
        edit.addThemeVariants(ButtonVariant.TERTIARY, ButtonVariant.SMALL);
        edit.setAriaLabel("Edit " + employee.name());
        return edit;
    }

    /**
     * The report. Only the two-property "Contact" column needs to be spelled
     * out; everything else the exporter can read off the renderer.
     */
    ExportedGrid report() {
        return GridExport.of(grid).withTitle("Employees").withColumnExtractor(
                contactColumn,
                employee -> employee.name() + " · " + employee.country())
                .export();
    }

    /**
     * What the exporter says about the "Contact" column when no extractor is
     * supplied — the gap, made visible in the demo.
     */
    String reportWithoutExtractorMessage() {
        try {
            GridExport.of(grid).export();
            return "The Contact column exported without an extractor — the "
                    + "LitRenderer gap has been closed upstream.";
        } catch (IllegalStateException e) {
            String message = e.getMessage();
            return message == null ? e.toString() : message;
        }
    }

    /** Test seam: the CSV the download link would produce. */
    String csv() {
        return CsvWriter.toCsv(report());
    }
}
