package com.example.uc2;

import java.util.List;

import com.example.common.BuiltWith;
import com.example.common.UseCaseDescription;
import com.example.demoapp.Order;
import com.example.tour.Tour;
import com.example.tour.TourStep;
import com.example.views.MainLayout;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.popover.PopoverPosition;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * UC2 — Tour of a busy view.
 * <p>
 * A screen with filters, a multi-select grid, a bulk action and a second tab
 * has its own short tour, which starts the first time the user opens the view
 * in this session. Two steps point at parts that are hidden at first: a date
 * filter inside the closed "More filters" panel and a button on the "History"
 * tab. The step opens the panel or selects the tab before it is shown. A target
 * further down the page, as on a phone, is scrolled into view first.
 */
@Route(value = "uc2", layout = MainLayout.class)
@PageTitle("UC2 — Tour of a busy view")
@UseCaseDescription("A tour of one complex view, including parts hidden in panels and tabs")
@Menu(order = 2, title = "UC2 — Tour of a busy view")
public class ComplexViewTourView extends VerticalLayout {

    /** The session attribute set once the user has opened this view. */
    static final String VISITED = ComplexViewTourView.class.getName()
            + ".visited";

    public ComplexViewTourView() {
        add(new H1("UC2 — Tour of a busy view"));
        add(new Paragraph("Some views need their own introduction, at the "
                + "moment the user first opens them. The tour of this order "
                + "screen starts on your first visit in this session. Some of "
                + "its steps point at parts in a closed panel or on another "
                + "tab; the tour opens them first, and scrolls to steps "
                + "further down the page. Leave the view and come "
                + "back: the tour does not start again."));

        TextField customer = new TextField("Customer");
        ComboBox<String> status = new ComboBox<>("Status");
        status.setItems("New", "Packed", "Shipped");
        DatePicker orderedAfter = new DatePicker("Ordered after");
        Details moreFilters = new Details("More filters", orderedAfter);
        Div filters = new Div(customer, status, moreFilters);
        filters.addClassName("uc2-filters");

        Grid<Order> orders = new Grid<>(Order.class, false);
        orders.addColumn(Order::number).setHeader("Order").setAutoWidth(true);
        orders.addColumn(Order::customer).setHeader("Customer");
        orders.addColumn(Order::status).setHeader("Status");
        orders.setItems(Order.samples());
        orders.setSelectionMode(Grid.SelectionMode.MULTI);
        orders.setAllRowsVisible(true);
        Button markShipped = new Button("Mark as shipped");
        markShipped.setEnabled(false);
        orders.addSelectionListener(e -> markShipped
                .setEnabled(!e.getAllSelectedItems().isEmpty()));

        Button exportHistory = new Button("Export history");
        TabSheet tabs = new TabSheet();
        tabs.add("Orders", new Div(orders, markShipped));
        tabs.add("History",
                new Div(new Paragraph(
                        "Every change to an order is listed here."),
                        exportHistory));
        tabs.setWidthFull();

        Tour tour = new Tour(List.of(
                TourStep.of(status, "Filter by status",
                        "Show only new, packed or shipped orders."),
                TourStep.of(orderedAfter, "More filters",
                        "Rarely used filters wait in this panel.")
                        .beforeShow(() -> moreFilters.setOpened(true)),
                TourStep.of(orders, "Pick orders",
                        "Tick the orders you want to work on.")
                        .at(PopoverPosition.TOP)
                        .beforeShow(() -> tabs.setSelectedIndex(0)),
                TourStep.of(markShipped, "Act on many at once",
                        "Mark all ticked orders as shipped in one go.")
                        .beforeShow(() -> tabs.setSelectedIndex(0)),
                TourStep.of(exportHistory, "Order history",
                        "Download every change to your orders.")
                        .beforeShow(() -> tabs.setSelectedIndex(1))));

        ValueSignal<String> why = new ValueSignal<>("");
        Span whyLine = new Span();
        whyLine.addClassName("tour-status");
        whyLine.bindText(why);
        Button start = new Button("Start the tour", e -> tour.start());
        Div controls = new Div(whyLine, start);
        controls.addClassName("tour-controls");

        add(controls, filters, tabs);
        add(new BuiltWith("A tour that belongs to one view").point(
                "The view starts its tour when it is attached, unless the session "
                        + "has an attribute saying the user was here before.")
                .point("A step can run code before it is shown: here it opens the "
                        + "\"More filters\" panel or selects a tab, so the target is on screen.")
                .point("A target on a tab that is not selected is not in the page yet; the "
                        + "popover is added once the tab shows it.")
                .java("uc2/ComplexViewTourView").java("tour/TourStep"));

        whenAttached(ui -> {
            VaadinSession session = ui.getSession();
            if (session.getAttribute(VISITED) == null) {
                session.setAttribute(VISITED, Boolean.TRUE);
                why.set("Your first visit in this session, so the tour "
                        + "started.");
                tour.start();
            } else {
                why.set("You were here before in this session, so the tour "
                        + "did not start.");
            }
            return tour::reset;
        });
    }
}
