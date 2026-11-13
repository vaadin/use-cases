package com.example.uc5;

import java.io.ByteArrayOutputStream;

import com.example.data.InvoiceBook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import com.vaadin.browserless.BrowserlessUIContext;
import com.vaadin.browserless.SpringBrowserlessApplicationContext;
import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Span;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = DeliveryReceiptView.class)
class DeliveryReceiptViewTest extends SpringBrowserlessTest {

    private static final int BADGE_COLUMN = 2;
    private static final int PDF_COLUMN = 3;

    @Autowired
    private InvoiceBook book;

    @Autowired
    private ApplicationContext applicationContext;

    @BeforeEach
    void emptyTheBook() {
        book.clear();
    }

    @Test
    void anInvoiceIsUnsentUntilATransferCompletes() {
        DeliveryReceiptView view = navigate(DeliveryReceiptView.class);

        assertEquals("Not sent", badge(0).getText());

        // What whenComplete does once the last byte has left.
        book.markSent("2026-0001", 2048);
        view.refresh();

        assertEquals("Sent, 2048 bytes", badge(0).getText());
        assertTrue(badge(0).hasClassName("sent"),
                "A sent invoice reads as sent, not just as text");
    }

    @Test
    void aDeliveryInOneSessionIsVisibleInTheNext() {
        try (var app = SpringBrowserlessApplicationContext.create(
                applicationContext,
                b -> b.withViewPackages(DeliveryReceiptView.class)
                        .withSessionLockIsolation(true))) {
            var alice = app.newUser().newWindow();
            var colleague = app.newUser().newWindow();
            alice.navigate(DeliveryReceiptView.class);
            assertEquals("Not sent", badge(alice, 1).getText());

            // A colleague, in their own browser, downloads the invoice.
            colleague.navigate(DeliveryReceiptView.class);
            Grid<?> grid = colleague.findInView(Grid.class).single();
            Anchor link = (Anchor) colleague.test(grid).getCellComponent(1,
                    PDF_COLUMN);
            ByteArrayOutputStream pdf = new ByteArrayOutputStream();
            colleague.test(link).download(pdf);
            colleague.runPendingSignalsTasks();
            String sent = "Sent, " + pdf.size() + " bytes";
            assertEquals(sent, badge(colleague, 1).getText());

            // Alice sees it the next time she loads the page.
            alice.reload();
            assertEquals(sent, badge(alice, 1).getText(),
                    "Delivery is a fact about the invoice, not about a "
                            + "session");
        }
    }

    @Test
    void resetForgetsEveryDelivery() {
        DeliveryReceiptView view = navigate(DeliveryReceiptView.class);
        book.markSent("2026-0001", 2048);
        view.refresh();
        assertEquals("Sent, 2048 bytes", badge(0).getText());

        test(findInView(Button.class).id("reset-button")).click();

        assertEquals("Not sent", badge(0).getText());
        assertTrue(book.delivery("2026-0001").isEmpty());
    }

    private static Span badge(BrowserlessUIContext window, int row) {
        Grid<?> grid = window.findInView(Grid.class).single();
        return (Span) window.test(grid).getCellComponent(row, BADGE_COLUMN);
    }

    private Span badge(int row) {
        Grid<?> grid = findInView(Grid.class).single();
        return (Span) test(grid).getCellComponent(row, BADGE_COLUMN);
    }
}
