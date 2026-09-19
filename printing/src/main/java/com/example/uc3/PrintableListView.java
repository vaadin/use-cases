package com.example.uc3;

import java.util.Comparator;
import java.util.List;

import com.example.MissingAPI;
import com.example.data.Order;
import com.example.data.Orders;
import com.example.print.OrderDocument;
import com.example.print.PrintColumn;
import com.example.print.PrintColumns;
import com.example.views.MainLayout;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Table;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * UC3 — Printing a long list.
 * <p>
 * A Grid cannot be printed. It renders a fixed number of rows into a scrolling
 * viewport and recycles them as the user scrolls, so the printer only ever sees
 * the handful of rows that happen to exist right now; its cells live in a
 * shadow root, where a print stylesheet cannot reach them; and neither its
 * header nor its rows take part in the browser's pagination, so nothing repeats
 * a header or avoids a break mid-row.
 * <p>
 * The workaround that actually reaches paper is a second rendering of the same
 * data as a plain HTML {@code table}, hidden on screen and shown only when
 * printing. Both renderings are driven from one list of {@link PrintColumn}s,
 * so a column is described once — but the application, not the framework, is
 * what keeps the two in sync: change the sort order and both have to be
 * rebuilt, as the Select below does.
 *
 * @see <a href=
 *      "https://github.com/vaadin/cookbook/issues/184">vaadin/cookbook#184 —
 *      How do I print a Grid</a>
 */
@Route(value = "uc3", layout = MainLayout.class)
@PageTitle("UC3 — Printing a long list")
@Menu(order = 3, title = "UC3 — Printing a long list")
@StyleSheet("uc3.css")
public class PrintableListView extends VerticalLayout {

    /**
     * The columns, described once and rendered into both the Grid and the
     * table.
     */
    public static final List<PrintColumn<Order>> COLUMNS = List.of(
            PrintColumn.of("Order", Order::id),
            PrintColumn.of("Customer", Order::customer),
            PrintColumn.of("Ordered", order -> order.ordered().toString()),
            PrintColumn.numeric("Items",
                    order -> Integer.toString(order.itemCount())),
            PrintColumn.numeric("Total",
                    order -> OrderDocument.money(order.total())));

    private static final int ORDER_COUNT = 120;

    private final Grid<Order> grid = new Grid<>();
    private final Div printable = new Div();

    public PrintableListView() {
        Div intro = new Div();
        intro.addClassName("no-print");
        intro.add(new H1("UC3 — Printing a long list"));
        intro.add(new Paragraph(ORDER_COUNT
                + " orders. On screen they are a Grid; on paper they are the "
                + "same rows as an HTML table, because a virtualised Grid "
                + "prints only the rows it has currently rendered — and those "
                + "live in a shadow root that print CSS cannot style. The "
                + "table repeats its header on every sheet and never breaks "
                + "a row in half."));

        Select<SortOrder> sort = new Select<>();
        sort.setLabel("Sort by");
        sort.setItems(SortOrder.values());
        sort.setItemLabelGenerator(SortOrder::label);
        sort.setValue(SortOrder.ORDER);
        sort.setId("sort-select");
        sort.addValueChangeListener(event -> show(event.getValue()));

        Button print = new Button("Print the list", event -> MissingAPI
                .print(event.getSource().getUI().orElseThrow()));
        print.addThemeVariants(ButtonVariant.PRIMARY);
        print.setId("print-button");

        HorizontalLayout controls = new HorizontalLayout(sort, print);
        controls.addClassName("no-print");
        controls.setAlignItems(Alignment.END);

        grid.addClassName("no-print");
        grid.setAllRowsVisible(false);
        grid.setHeight("22em");
        PrintColumns.addTo(grid, COLUMNS);

        printable.addClassNames("print-only", "printable");
        printable.setId("printable-list");

        add(intro, controls, grid, printable);
        show(SortOrder.ORDER);
    }

    private void show(SortOrder order) {
        List<Order> sorted = Orders.sample(ORDER_COUNT).stream()
                .sorted(order.comparator()).toList();
        grid.setItems(sorted);

        printable.removeAll();
        printable.add(new Div("Orders — " + order.label()));
        Table table = PrintColumns.asTable(COLUMNS, sorted);
        table.setId("printable-table");
        printable.add(table);
    }

    /** The orderings both renderings can be sorted by. */
    public enum SortOrder {

        ORDER("Order number", Comparator.comparing(Order::id)),
        CUSTOMER("Customer",
                Comparator.comparing(Order::customer).thenComparing(Order::id)),
        TOTAL("Total, largest first",
                Comparator.comparing(Order::total).reversed());

        private final String label;
        private final Comparator<Order> comparator;

        SortOrder(String label, Comparator<Order> comparator) {
            this.label = label;
            this.comparator = comparator;
        }

        /** The label shown in the Select and printed above the table. */
        public String label() {
            return label;
        }

        /** The comparator that both the Grid and the printed table use. */
        public Comparator<Order> comparator() {
            return comparator;
        }
    }
}
