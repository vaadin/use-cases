package com.example.uc2;

import java.util.List;

import com.example.MissingAPI;
import com.example.data.Order;
import com.example.data.Orders;
import com.example.views.MainLayout;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.router.RouteParameters;

/**
 * UC2 — A route that exists only to be printed.
 * <p>
 * The everyday pattern behind every "print invoice" button: the user stays in
 * the application, and the document opens in its own window, prints itself and
 * disappears. Nothing of the application shell is involved, so nothing of it
 * can end up on the paper — see {@link PackingSlipView}.
 * <p>
 * Two things have to be worked around.
 * {@link com.vaadin.flow.component.page.Page#open(String, String)} accepts a
 * window name but no window features, so a print window cannot be sized without
 * dropping to {@code window.open}; and the new window must print itself only
 * once the browser has painted the document, which takes a pair of animation
 * frames because Flow delivers the DOM update and the JavaScript in the same
 * response.
 */
@Route(value = "uc2", layout = MainLayout.class)
@PageTitle("UC2 — A print-only route")
@Menu(order = 2, title = "UC2 — A print-only route")
public class PrintRouteView extends VerticalLayout {

    private static final int ORDER_COUNT = 24;

    public PrintRouteView() {
        add(new H1("UC2 — A print-only route"));
        add(new Paragraph(
                "Pick an order and print its packing slip. The slip opens at "
                        + "uc2/slip/<order> in its own window — a route with "
                        + "no layout at all — prints itself, and closes when "
                        + "the print dialog does."));

        List<Order> orders = Orders.sample(ORDER_COUNT);

        Grid<Order> grid = new Grid<>();
        grid.addColumn(Order::id).setHeader("Order").setAutoWidth(true);
        grid.addColumn(Order::customer).setHeader("Customer")
                .setAutoWidth(true);
        grid.addColumn(order -> order.ordered().toString()).setHeader("Ordered")
                .setAutoWidth(true);
        grid.addComponentColumn(this::printButton).setHeader("Packing slip")
                .setAutoWidth(true);
        grid.setItems(orders);
        grid.setAllRowsVisible(true);
        add(grid);
    }

    private Button printButton(Order order) {
        Button print = new Button("Print", event -> MissingAPI
                .openPrintWindow(UI.getCurrent(), slipUrl(order)));
        print.setId("print-" + order.id());
        return print;
    }

    /**
     * The URL of the print-only route for one order.
     *
     * @param order
     *            the order to print
     * @return the relative URL of its packing slip
     */
    public static String slipUrl(Order order) {
        return RouteConfiguration.forSessionScope().getUrl(
                PackingSlipView.class,
                new RouteParameters("orderId", order.id()));
    }
}
