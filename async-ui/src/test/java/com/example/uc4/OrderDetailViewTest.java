package com.example.uc4;

import com.example.ManualLatency;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.RouterLink;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = OrderDetailView.class)
class OrderDetailViewTest extends SpringBrowserlessTest {

    @Autowired
    private ManualLatency latency;

    @BeforeEach
    void resetLatency() {
        latency.reset();
    }

    @Test
    void viewWithoutAnOrderOffersLinks() {
        navigate(OrderDetailView.class);

        assertEquals("UC4 — Detail that streams in",
                findInView(H1.class).single().getText());
        assertEquals("No order selected",
                findInView(H2.class).single().getText());
        assertEquals(OrderDetailView.SAMPLE_IDS.size(),
                findInView(RouterLink.class).all().size());
        assertTrue(latency.pending().isEmpty());
    }

    @Test
    void routeRendersAtOnceAndDetailsFollow() {
        navigate(OrderDetailView.class, 42L);
        runPendingSignalsTasks();

        assertEquals("Order #42", findInView(H2.class).single().getText());
        assertTrue(visibleWithClass("skeleton"),
                "the skeleton shows while the order is looked up");
        assertEquals(1, latency.pending().size());

        latency.completeNext();
        runPendingSignalsTasks();

        assertFalse(visibleWithClass("skeleton"));
        assertTrue(visibleWithClass("order-fields"));
        assertTrue(findInView(Span.class).all().stream()
                .anyMatch(span -> "Customer".equals(span.getText())));
    }

    @Test
    void unknownOrderBecomesNotFoundMessage() {
        navigate(OrderDetailView.class, 9_999_999L);
        latency.completeNext();
        runPendingSignalsTasks();

        assertTrue(findInView(Span.class).all().stream().anyMatch(
                span -> span.isVisible() && "Order #9999999 does not exist."
                        .equals(span.getText())));
        assertFalse(visibleWithClass("order-fields"));
    }

    @Test
    void switchingOrdersDropsTheOlderAnswer() {
        navigate(OrderDetailView.class, 42L);
        navigate(OrderDetailView.class, 9_999_999L);
        runPendingSignalsTasks();

        // The lookup for #42 answers after the user moved on.
        latency.complete(0);
        runPendingSignalsTasks();
        assertTrue(visibleWithClass("skeleton"),
                "the answer for the previous order must not be shown");

        latency.completePending();
        runPendingSignalsTasks();
        assertFalse(visibleWithClass("order-fields"));
    }

    private boolean visibleWithClass(String className) {
        return findInView(Div.class).all().stream()
                .anyMatch(div -> div.getClassNames().contains(className)
                        && div.isVisible());
    }
}
