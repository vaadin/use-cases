package com.example.uc2;

import java.time.LocalDate;

import com.example.common.UseCaseDescription;
import com.example.orders.Order;
import com.example.orders.OrderStore;
import com.example.orders.Product;
import com.example.orders.QuoteCalculator;
import com.example.uc4.OrdersView;
import com.example.views.MainLayout;
import com.example.views.TestsNote;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

/**
 * UC2 — A view test without a browser.
 * <p>
 * Placing an order is the most common kind of screen: fill in a form, press
 * Save, get a confirmation and land somewhere else. A browserless test drives
 * it the way a user would, through the components' testers (set a value,
 * click), and checks everything the user would notice: the notification, the
 * view it navigates to, and the order that was stored. It runs in a few hundred
 * milliseconds, with Spring beans and navigation but no browser.
 */
@Route(value = "uc2", layout = MainLayout.class)
@PageTitle("UC2 — View test without a browser")
@UseCaseDescription("Testing a form flow through component testers, without a browser")
@Menu(order = 2, title = "UC2 — View test without a browser")
@AnonymousAllowed
public class NewOrderView extends VerticalLayout {

    static final LocalDate EARLIEST_DELIVERY = LocalDate.now().plusDays(1);

    private final TextField customer = new TextField("Customer");
    private final Select<Product> product = new Select<>();
    private final IntegerField quantity = new IntegerField("Quantity");
    private final DatePicker delivery = new DatePicker("Delivery");
    private final Button place = new Button("Place order");

    public NewOrderView(OrderStore store, QuoteCalculator calculator) {
        add(new H1("UC2 — View test without a browser"));
        add(new Paragraph("Place an order: a notification confirms it and "
                + "the order list opens. The browserless test does exactly "
                + "that and checks the notification, the navigation and the "
                + "stored order."));

        customer.setRequired(true);
        product.setLabel("Product");
        product.setItems(Product.values());
        product.setItemLabelGenerator(Product::title);
        product.setValue(Product.ESPRESSO_BEANS);
        quantity.setValue(1);
        quantity.setMin(1);
        quantity.setStepButtonsVisible(true);
        delivery.setValue(EARLIEST_DELIVERY);
        delivery.setMin(EARLIEST_DELIVERY);

        place.addThemeVariants(ButtonVariant.PRIMARY);
        place.addClickListener(e -> {
            String name = customer.getValue().strip();
            Integer amount = quantity.getValue();
            if (name.isEmpty() || amount == null || amount < 1
                    || delivery.getValue() == null) {
                Notification.show("Fill in customer, quantity and delivery")
                        .addThemeVariants(NotificationVariant.WARNING);
                return;
            }
            Order order = store.place(name, product.getValue(), amount,
                    delivery.getValue(), calculator
                            .quote(product.getValue(), amount, false).total());
            Notification.show("Order #" + order.number() + " placed")
                    .addThemeVariants(NotificationVariant.SUCCESS);
            getUI().ifPresent(ui -> ui.navigate(OrdersView.class));
        });

        Div sample = new Div(new H2("New order"),
                new FormLayout(customer, product, quantity, delivery), place);
        sample.addClassName("sample");
        add(sample, new TestsNote(
                "View test: uc2/NewOrderViewTest (browserless)",
                "Same flow in a real browser: uc8/CheckoutIT (TestBench), "
                        + "uc8/CheckoutPlaywrightIT (Playwright)"));
    }
}
