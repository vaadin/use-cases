package com.example.uc2;

import com.example.MissingAPI;
import com.example.data.Order;
import com.example.data.Orders;
import com.example.print.OrderDocument;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * The print-only route behind UC2: {@code uc2/slip/<order>}.
 * <p>
 * It deliberately declares no {@code layout}, so the application shell is not
 * merely hidden by print CSS — it is never built. That is the whole trick: a
 * route that exists to be printed is a different page, not a styled version of
 * an existing one. The view prints itself as soon as the browser has painted it
 * and closes the window when the dialog goes away.
 */
@Route("uc2/slip/:orderId")
@PageTitle("Packing slip")
@StyleSheet("uc2.css")
public class PackingSlipView extends VerticalLayout
        implements BeforeEnterObserver {

    private static final int ORDER_COUNT = 24;

    private final Div content = new Div();

    private boolean hasDocument;

    public PackingSlipView() {
        addClassName("slip-view");
        setPadding(false);
        add(content);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        content.removeAll();
        hasDocument = false;
        String orderId = event.getRouteParameters().get("orderId").orElse("");
        Orders.byId(orderId, ORDER_COUNT).ifPresentOrElse(this::show,
                () -> content.add(new Paragraph("No order " + orderId + ".")));
    }

    private void show(Order order) {
        hasDocument = true;
        OrderDocument document = new OrderDocument(order);
        document.addClassName("printable");
        document.setId("order-document");
        content.add(document);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        if (!hasDocument) {
            return;
        }
        // Printing straight from onAttach would print a blank sheet: the
        // JavaScript arrives in the same response as the DOM it should print.
        MissingAPI.printAfterRender(attachEvent.getUI());
        MissingAPI.closeWindowAfterPrint(attachEvent.getUI());
    }
}
