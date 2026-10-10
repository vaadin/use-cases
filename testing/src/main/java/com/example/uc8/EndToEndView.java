package com.example.uc8;

import com.example.common.UseCaseDescription;
import com.example.orders.OrderStore;
import com.example.uc2.NewOrderView;
import com.example.views.MainLayout;
import com.example.views.TestsNote;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.auth.AnonymousAllowed;

/**
 * UC8 — End-to-end in a real browser.
 * <p>
 * The same order flow as UC2, now in a real Chrome against the packaged
 * application: open the order form, place an order, and find it at the top of
 * the order list. It is written twice, once with TestBench (Vaadin's element
 * API: {@code $(TextFieldElement.class).withLabel(...)}) and once with
 * Playwright for Java (role and label locators), so the two can be compared.
 * <p>
 * End-to-end tests share one running server, so they must not depend on each
 * other's data: each run uses a customer name of its own. The reset button
 * below is for trying the flow by hand.
 */
@Route(value = "uc8", layout = MainLayout.class)
@PageTitle("UC8 — End-to-end in a real browser")
@UseCaseDescription("Driving a full user flow in a real browser with TestBench and Playwright")
@Menu(order = 8, title = "UC8 — End-to-end in a real browser")
@AnonymousAllowed
public class EndToEndView extends VerticalLayout {

    private final Span count = new Span();

    public EndToEndView(OrderStore store) {
        add(new H1("UC8 — End-to-end in a real browser"));
        add(new Paragraph("The browser tests place an order through the UC2 "
                + "form and look it up in the UC4 list, in Chrome, against "
                + "the running application."));

        count.setText(store.all().size() + " orders placed so far");
        Button reset = new Button("Remove placed orders", e -> {
            store.clear();
            count.setText("0 orders placed so far");
        });

        Div sample = new Div(
                new RouterLink("Open the order form", NewOrderView.class),
                count, reset);
        sample.addClassName("sample");
        add(sample,
                new TestsNote("TestBench: uc8/CheckoutIT",
                        "Playwright: uc8/CheckoutPlaywrightIT",
                        "Run: mvn -pl testing -am verify -Pit"));
    }

    // Package-private test seam.
    String count() {
        return count.getText();
    }
}
