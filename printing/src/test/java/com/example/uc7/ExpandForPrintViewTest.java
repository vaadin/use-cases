package com.example.uc7;

import java.util.List;
import java.util.Map;

import com.example.PrintTestSupport;
import com.example.data.Orders;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Table;
import com.vaadin.flow.dom.Element;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = ExpandForPrintView.class)
class ExpandForPrintViewTest extends SpringBrowserlessTest {

    @Test
    void theOrderIsSplitIntoFoldableSections() {
        navigate(ExpandForPrintView.class);

        assertTrue(findInView(H1.class).all().stream().anyMatch(
                h -> "UC7 — Expand everything for print".equals(h.getText())));
        Div document = findInView(Div.class).id("order-document");
        assertTrue(document.hasClassName("printable"));

        List<Details> sections = findInView(Details.class).all();
        assertEquals(
                List.of("Delivery address", "Order lines",
                        "Delivery and returns"),
                sections.stream().map(Details::getSummaryText).toList());
        // Folded on screen, so there is something for printing to unfold.
        assertEquals(List.of(true, false, false),
                sections.stream().map(Details::isOpened).toList());
        assertEquals(Orders.sampleOrder().lines().size(), find(Table.class)
                .from(sections.get(1)).single().getBodyRows().size());
    }

    @Test
    void everySectionIsOpenedForPrintAndRestoredAfterwards() {
        navigate(ExpandForPrintView.class);
        // Trigger listeners are only queued when the response is written.
        roundTrip();

        Map<Element, List<List<Object>>> listeners = PrintTestSupport
                .queuedListeners();
        for (Details section : findInView(Details.class).all()) {
            // Remember the state the user left the section in, open it for
            // the paper, and put that state back when the dialog closes —
            // all in the browser, in this order.
            assertEquals(
                    List.of(List.of("openedOnScreen", "opened", "beforeprint"),
                            List.of("opened", true, "beforeprint"),
                            List.of("opened", "openedOnScreen", "afterprint")),
                    listeners.get(section.getElement()),
                    section.getSummaryText());
        }
    }

    @Test
    void printButtonOpensThePrintDialog() {
        navigate(ExpandForPrintView.class);
        assertFalse(PrintTestSupport.printRequested());

        test(findInView(Button.class).id("print-button")).click();

        assertTrue(PrintTestSupport.printRequested());
    }
}
