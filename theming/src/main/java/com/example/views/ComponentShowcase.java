package com.example.views;

import java.time.LocalDate;
import java.util.List;

import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.badge.BadgeVariant;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextField;

/**
 * A small slice of a business application (an order form and a list), used by
 * the use cases that change how everything looks, so the effect of a theme
 * change is visible on familiar components.
 */
public class ComponentShowcase extends Div {

    record Order(String number, String customer, String status) {
    }

    public ComponentShowcase() {
        addClassName("sample");

        Tabs tabs = new Tabs(new Tab("Details"), new Tab("History"),
                new Tab("Notes"));

        TextField customer = new TextField("Customer", "Kestrel Air", "");
        customer.setPrefixComponent(VaadinIcon.BUILDING.create());
        DatePicker delivery = new DatePicker("Delivery",
                LocalDate.of(2026, 3, 5));
        RadioButtonGroup<String> priority = new RadioButtonGroup<>("Priority",
                "Normal", "Express");
        priority.setValue("Normal");
        HorizontalLayout fields = new HorizontalLayout(customer, delivery,
                priority);
        fields.setWrap(true);

        Checkbox terms = new Checkbox("Send an order confirmation", true);

        Button save = new Button("Save order", VaadinIcon.CHECK.create());
        save.addThemeVariants(ButtonVariant.PRIMARY);
        Button cancel = new Button("Cancel");
        Button delete = new Button("Delete");
        delete.addThemeVariants(ButtonVariant.ERROR, ButtonVariant.TERTIARY);
        Badge badge = new Badge("Paid");
        badge.addThemeVariants(BadgeVariant.SUCCESS);
        HorizontalLayout actions = new HorizontalLayout(save, cancel, delete,
                badge);
        actions.setAlignItems(Alignment.CENTER);

        ProgressBar progress = new ProgressBar(0, 1, 0.6);
        progress.setWidth("16rem");

        Grid<Order> grid = new Grid<>();
        grid.addColumn(Order::number).setHeader("Order");
        grid.addColumn(Order::customer).setHeader("Customer");
        grid.addColumn(Order::status).setHeader("Status");
        grid.setItems(List.of(new Order("#1042", "Kestrel Air", "Paid"),
                new Order("#1043", "Blue Finch", "Open"),
                new Order("#1044", "Harbour Foods", "Overdue")));
        grid.setAllRowsVisible(true);

        add(new H2("Order #1042"), tabs, fields, terms, actions, progress,
                grid);
    }
}
