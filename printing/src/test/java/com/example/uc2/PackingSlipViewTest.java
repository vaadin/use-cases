package com.example.uc2;

import java.util.Map;

import com.example.PrintTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = PackingSlipView.class)
class PackingSlipViewTest extends SpringBrowserlessTest {

    @Test
    void slipRendersTheOrderAndPrintsItselfWithoutAnyChrome() {
        navigate(PackingSlipView.class, Map.of("orderId", "ORD-1003"));

        Div document = findInView(Div.class).id("order-document");
        assertTrue(document.getElement().getTextRecursively()
                .contains("ORD-1003"));
        assertTrue(document.hasClassName("printable"));

        assertTrue(PrintTestSupport.pendingJsContains("requestAnimationFrame"),
                "The slip must wait for a painted frame before printing");
        assertTrue(PrintTestSupport.printRequested());
        assertTrue(PrintTestSupport.pendingJsContains("afterprint"),
                "The print window should close itself again");
    }

    @Test
    void unknownOrderPrintsNothing() {
        navigate(PackingSlipView.class, Map.of("orderId", "ORD-9999"));

        assertTrue(findInView(Paragraph.class).all().stream()
                .anyMatch(p -> p.getText().contains("No order ORD-9999")));
        assertFalse(PrintTestSupport.printRequested(),
                "An empty page should not open the print dialog");
    }
}
