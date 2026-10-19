package com.example.uc8;

import java.util.List;
import java.util.stream.IntStream;

import com.example.common.UnreleasedApi;
import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;

import com.vaadin.flow.component.clipboard.Clipboard;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.LitRenderer;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * UC8 — A copy button in every grid row, next to the value it copies.
 * <p>
 * This is the use case from the
 * <a href="https://vaadin.com/forum/t/clipboard-copy/164697/11">forum
 * thread</a>: "a button next to the value (for example addresses) inside a
 * table to copy the value", and the question of how to do that "without
 * instantiating million of components and instances of the clipboard helper".
 * <p>
 * The email column is a {@link LitRenderer}: the cell, button included, is
 * rendered in the browser from the row's data, so no row has a server-side
 * component. The copy is a client action, {@link Clipboard#writeText()}, bound
 * once for the whole column with {@code withClientAction("copy", ...)}. The
 * template decides what to copy by calling it with the row's value,
 * {@code copy(item.email)}, and the write runs inside the browser's click
 * handler, where the user gesture is still valid. Scrolling builds no
 * components and no bindings: there is one binding for {@value #ROW_COUNT}
 * rows.
 * <p>
 * {@code Clipboard.writeText()} and {@code LitRenderer.withClientAction} come
 * from vaadin/flow#25366 and vaadin/flow-components#10470, which this module
 * builds against as snapshots. A client action reports neither success nor
 * failure to the server, so the view can not confirm the copy; see
 * {@code clipboard/API-GAPS.md} #1.
 */
@Route(value = "uc8", layout = MainLayout.class)
@PageTitle("UC8 — Copy button in every grid row")
@UseCaseDescription("Putting a copy button next to every value in a grid")
@Menu(order = 8, title = "UC8 — Copy from a grid")
@StyleSheet("uc8.css")
@UnreleasedApi("https://github.com/vaadin/flow/pull/25366")
public class CopyFromGridView extends VerticalLayout {

    /** A row of the demo grid. */
    public record Customer(String name, String company, String email) {
    }

    static final int ROW_COUNT = 500;

    final Grid<Customer> grid = new Grid<>();

    public CopyFromGridView() {
        setSizeFull();
        add(new H1("UC8 — A copy button in every grid row"));
        add(new Paragraph("Each row has a copy button next to the email "
                + "address it copies. The grid has " + ROW_COUNT
                + " rows, and the cells, buttons included, are rendered in "
                + "the browser from the row data. The server holds one "
                + "clipboard binding for the whole column, however far you "
                + "scroll."));

        grid.addColumn(Customer::name).setHeader("Name").setAutoWidth(true);
        grid.addColumn(Customer::company).setHeader("Company")
                .setAutoWidth(true);
        grid.addColumn(emailCell()).setHeader("Email").setAutoWidth(true)
                .setFlexGrow(1);
        grid.setItems(generateCustomers(ROW_COUNT));
        grid.setSizeFull();

        add(grid);
    }

    /**
     * The cell content: the value, and a button that copies exactly that value.
     * The template is rendered per row in the browser; the {@code copy} action
     * is bound once for the column.
     */
    private static LitRenderer<Customer> emailCell() {
        return LitRenderer.<Customer> of("""
                <div class="copy-cell">
                  <span>${item.email}</span>
                  <vaadin-button theme="tertiary small"
                      aria-label="Copy ${item.email}"
                      @click=${() => copy(item.email)}>
                    <vaadin-icon icon="vaadin:copy-o"></vaadin-icon>
                    <vaadin-tooltip slot="tooltip"
                        text="Copy ${item.email}"></vaadin-tooltip>
                  </vaadin-button>
                </div>""").withProperty("email", Customer::email)
                .withClientAction("copy", Clipboard.writeText());
    }

    private static List<Customer> generateCustomers(int count) {
        String[] firstNames = { "Ada", "Grace", "Alan", "Linus", "Barbara",
                "Ken", "Margaret", "Dennis", "Radia", "Tim" };
        String[] lastNames = { "Lovelace", "Hopper", "Turing", "Torvalds",
                "Liskov", "Thompson", "Hamilton", "Ritchie", "Perlman",
                "Berners-Lee" };
        String[] companies = { "Northwind", "Contoso", "Initech", "Umbrella",
                "Globex", "Hooli", "Acme", "Soylent" };
        return IntStream.range(0, count).mapToObj(i -> {
            String first = firstNames[i % firstNames.length];
            String last = lastNames[(i / firstNames.length) % lastNames.length];
            String company = companies[i % companies.length];
            String email = (first + "." + last + (i + 1) + "@"
                    + company.toLowerCase() + ".example").toLowerCase();
            return new Customer(first + " " + last + " " + (i + 1), company,
                    email);
        }).toList();
    }
}
