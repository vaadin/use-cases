package com.example.uc7;

import com.example.demoapp.Order;
import com.example.views.MainLayout;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * The orders page of UC7 — Tour across views: the tour continues here from the
 * dashboard.
 */
@Route(value = "uc7/orders", layout = MainLayout.class)
@PageTitle("UC7 — Tour across views")
public class CrossViewOrdersView extends CrossViewPage {

    static final String ORDERS = "orders";

    public CrossViewOrdersView() {
        super("Orders");
        Grid<Order> orders = new Grid<>(Order.class, false);
        orders.addColumn(Order::number).setHeader("Order").setAutoWidth(true);
        orders.addColumn(Order::customer).setHeader("Customer");
        orders.addColumn(Order::status).setHeader("Status");
        orders.setItems(Order.samples());
        orders.setAllRowsVisible(true);
        addTarget(ORDERS, orders);
        addContent(orders);
    }
}
