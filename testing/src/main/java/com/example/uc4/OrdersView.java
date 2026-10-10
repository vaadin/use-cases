package com.example.uc4;

import java.text.NumberFormat;
import java.util.Locale;

import com.example.common.UseCaseDescription;
import com.example.orders.Order;
import com.example.orders.OrderHistory;
import com.example.orders.OrderStore;
import com.example.views.MainLayout;
import com.example.views.TestsNote;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.dataview.GridLazyDataView;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

/**
 * UC4 — Grids and lazy data.
 * <p>
 * The order list loads 100,000 orders page by page from {@link OrderHistory},
 * sorted and filtered in the backend. Selecting a row shows its details, and
 * orders placed in this application have a Cancel button in their row.
 * <p>
 * The tests read rows and cells through the Grid's tester, check that sorting
 * and filtering reach the backend, select a row, and click the button inside a
 * cell.
 */
@Route(value = "uc4", layout = MainLayout.class)
@PageTitle("UC4 — Grids and lazy data")
@UseCaseDescription("Testing rows, sorting, filtering, selection and buttons in a lazy Grid")
@Menu(order = 4, title = "UC4 — Grids and lazy data")
@AnonymousAllowed
public class OrdersView extends VerticalLayout {

    private final Grid<Order> grid = new Grid<>();
    private final TextField filter = new TextField();
    private final Span details = new Span("Select an order");
    private final GridLazyDataView<Order> dataView;

    public OrdersView(OrderHistory history, OrderStore store) {
        add(new H1("UC4 — Grids and lazy data"));
        add(new Paragraph("100,000 orders, loaded page by page. Sort by a "
                + "column, filter by customer, select an order, or cancel "
                + "one you placed in UC2."));

        NumberFormat euros = NumberFormat.getCurrencyInstance(Locale.GERMANY);
        grid.addColumn(Order::number).setHeader("Order").setKey("number")
                .setSortable(true).setSortProperty("number");
        grid.addColumn(Order::customer).setHeader("Customer").setKey("customer")
                .setSortable(true).setSortProperty("customer");
        grid.addColumn(order -> order.product().title()).setHeader("Product")
                .setKey("product");
        grid.addColumn(Order::quantity).setHeader("Qty").setKey("quantity");
        grid.addColumn(order -> euros.format(order.total())).setHeader("Total")
                .setKey("total").setSortable(true).setSortProperty("total");
        grid.addComponentColumn(order -> {
            if (order.number() <= OrderHistory.PAST_ORDERS) {
                return new Span();
            }
            Button cancel = new Button("Cancel", VaadinIcon.CLOSE.create(),
                    e -> {
                        store.cancel(order.number());
                        Notification.show(
                                "Order #" + order.number() + " cancelled");
                        grid.getDataProvider().refreshAll();
                    });
            cancel.addThemeVariants(ButtonVariant.TERTIARY,
                    ButtonVariant.SMALL);
            return cancel;
        }).setHeader("").setKey("actions");

        dataView = grid.setItems(query -> history.fetch(filter.getValue(), query
                .getSortOrders().stream()
                .map(order -> new OrderHistory.Sort(order.getSorted(),
                        order.getDirection() == SortDirection.ASCENDING))
                .toList(), query.getOffset(), query.getLimit()),
                query -> history.count(filter.getValue()));
        grid.setHeight("26rem");
        grid.asSingleSelect().addValueChangeListener(
                e -> details.setText(e.getValue() == null ? "Select an order"
                        : "Order #%d: %d × %s for %s".formatted(
                                e.getValue().number(), e.getValue().quantity(),
                                e.getValue().product().title(),
                                e.getValue().customer())));

        filter.setPlaceholder("Filter by customer");
        filter.setClearButtonVisible(true);
        filter.setValueChangeMode(ValueChangeMode.LAZY);
        filter.addValueChangeListener(e -> dataView.refreshAll());

        Div sample = new Div(filter, grid, details);
        sample.addClassName("sample");
        add(sample,
                new TestsNote("View test: uc4/OrdersViewTest (browserless)"));
    }

    // Package-private test seams.
    Grid<Order> grid() {
        return grid;
    }

    TextField filter() {
        return filter;
    }

    String details() {
        return details.getText();
    }
}
