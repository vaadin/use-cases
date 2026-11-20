package com.example.demoapp;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Nav;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.TextField;

/**
 * A small, made-up order management app — "Acme Orders" — with a header, a
 * navigation and a grid. The tours of this module introduce its parts. Nothing
 * in it does anything; it is only something to point at.
 */
public class DemoApp extends Div {

    private final Nav navigation = new Nav();
    private final TextField search = new TextField();
    private final Button savedFilters = new Button("Saved filters",
            VaadinIcon.FILTER.create());
    private final Button newOrder = new Button("New order",
            VaadinIcon.PLUS.create());
    private final Button export = new Button("Export",
            VaadinIcon.DOWNLOAD.create());
    private final Button darkMode = new Button(VaadinIcon.MOON_O.create());
    private final Avatar userMenu = new Avatar("Alex Doe");
    private final Grid<Order> orders = new Grid<>(Order.class, false);
    private final Div header;

    public DemoApp() {
        addClassName("demo-app");

        Span logo = new Span("Acme Orders");
        logo.addClassName("demo-app-logo");
        search.setPlaceholder("Search orders");
        search.setPrefixComponent(VaadinIcon.SEARCH.create());
        search.setAriaLabel("Search orders");
        newOrder.addThemeVariants(ButtonVariant.PRIMARY);
        darkMode.setAriaLabel("Dark mode");
        darkMode.setTooltipText("Dark mode");
        userMenu.addClassName("demo-app-user");

        header = new Div(logo, search, savedFilters, newOrder, export, darkMode,
                userMenu);
        header.addClassName("demo-app-header");

        for (String page : new String[] { "Dashboard", "Orders", "Customers",
                "Reports" }) {
            Span item = new Span(page);
            item.addClassName("demo-app-nav-item");
            navigation.add(item);
        }
        navigation.addClassName("demo-app-nav");

        orders.addColumn(Order::number).setHeader("Order").setAutoWidth(true);
        orders.addColumn(Order::customer).setHeader("Customer");
        orders.addColumn(Order::status).setHeader("Status");
        orders.addColumn(Order::total).setHeader("Total");
        orders.setItems(Order.samples());
        orders.setAllRowsVisible(true);

        Div body = new Div(navigation, orders);
        body.addClassName("demo-app-body");
        add(header, body);
    }

    /** Adds a component at the end of the app's header. */
    public void addToHeader(Component component) {
        header.add(component);
    }

    /**
     * Adds a component right after the given component of the app's header.
     */
    public void addNextTo(Component headerComponent, Component component) {
        header.addComponentAtIndex(header.indexOf(headerComponent) + 1,
                component);
    }

    public Nav navigation() {
        return navigation;
    }

    public TextField search() {
        return search;
    }

    public Button savedFilters() {
        return savedFilters;
    }

    public Button newOrder() {
        return newOrder;
    }

    public Button export() {
        return export;
    }

    public Button darkMode() {
        return darkMode;
    }

    public Avatar userMenu() {
        return userMenu;
    }

    public Grid<Order> orders() {
        return orders;
    }
}
