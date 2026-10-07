package com.example.uc8;

import java.util.List;
import java.util.stream.IntStream;

import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.clipboard.Clipboard;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
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
 * The copy button is rendered by a {@link ComponentRenderer}, and each row's
 * button binds the row's own value as a literal:
 * {@code Clipboard.onClick(button).writeText(customer.email())}. That is the
 * whole implementation — the value is known when the cell is rendered, so no
 * server round trip and no dynamic value source are needed, and the write still
 * runs inside the browser's click handler where the user gesture is valid.
 * <p>
 * On the instantiation worry: a {@code ComponentRenderer} only builds
 * components for the rows the grid actually renders, and destroys them again
 * once a row scrolls out of the buffer. Measured in a browser, this grid holds
 * ten copy buttons — not {@value #ROW_COUNT} — and scrolling to row 400 leaves
 * it at ten. What it does mean is that every one of those buttons carries its
 * own {@code Clipboard.onClick} binding, torn down and rebuilt as rows recycle,
 * where one binding for the whole column would do. See
 * {@code clipboard/API-GAPS.md}.
 */
@Route(value = "uc8", layout = MainLayout.class)
@PageTitle("UC8 — Copy button in every grid row")
@UseCaseDescription("Putting a copy button next to every value in a grid")
@Menu(order = 8, title = "UC8 — Copy from a grid")
@StyleSheet("uc8.css")
public class CopyFromGridView extends VerticalLayout {

    /** A row of the demo grid. */
    public record Customer(String name, String company, String email) {
    }

    static final int ROW_COUNT = 500;

    final Grid<Customer> grid = new Grid<>();

    public CopyFromGridView() {
        setSizeFull();
        add(new H1("UC8 — A copy button in every grid row"));
        add(new Paragraph(
                "Each row carries its own copy button next to the email "
                        + "address it copies. The grid has " + ROW_COUNT
                        + " rows, but the buttons only exist for the rows "
                        + "currently rendered — scroll to the bottom and back "
                        + "and the copy buttons keep working, because the "
                        + "renderer rebuilds them (and their clipboard "
                        + "bindings) as rows come into view."));

        grid.addColumn(Customer::name).setHeader("Name").setAutoWidth(true);
        grid.addColumn(Customer::company).setHeader("Company")
                .setAutoWidth(true);
        grid.addColumn(new ComponentRenderer<>(CopyFromGridView::emailCell))
                .setHeader("Email").setAutoWidth(true).setFlexGrow(1);
        grid.setItems(generateCustomers(ROW_COUNT));
        grid.setSizeFull();

        add(grid);
    }

    /**
     * The cell content: the value, and a button that copies exactly that value.
     * The binding is per row — this method runs once per rendered row.
     */
    private static Component emailCell(Customer customer) {
        Span value = new Span(customer.email());

        Button copy = new Button(VaadinIcon.COPY_O.create());
        copy.addThemeVariants(ButtonVariant.TERTIARY, ButtonVariant.SMALL);
        copy.setAriaLabel("Copy " + customer.email());
        copy.setTooltipText("Copy " + customer.email());
        Clipboard.onClick(copy).writeText(customer.email(),
                copied -> Notification.show("Copied " + copied),
                error -> Notification.show("Copy failed: " + error.message()));

        HorizontalLayout cell = new HorizontalLayout(value, copy);
        cell.addClassName("copy-cell");
        cell.setAlignItems(Alignment.CENTER);
        // Keeps the buttons in a column of their own instead of trailing
        // behind each address.
        cell.setFlexGrow(1, value);
        return cell;
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
