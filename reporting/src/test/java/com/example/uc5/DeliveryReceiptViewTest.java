package com.example.uc5;

import com.example.data.InvoiceBook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = DeliveryReceiptView.class)
class DeliveryReceiptViewTest extends SpringBrowserlessTest {

    private static final int BADGE_COLUMN = 2;

    @Autowired
    private InvoiceBook book;

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
        navigate(DeliveryReceiptView.class);
        assertEquals("Not sent", badge(1).getText());

        // A colleague, in their own browser, sends the invoice.
        book.markSent("2026-0002", 1024);

        cleanVaadinEnvironment();
        initVaadinEnvironment();
        navigate(DeliveryReceiptView.class);

        assertEquals("Sent, 1024 bytes", badge(1).getText(),
                "Delivery is a fact about the invoice, not about a session");
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

    private Span badge(int row) {
        Grid<?> grid = findInView(Grid.class).single();
        return (Span) test(grid).getCellComponent(row, BADGE_COLUMN);
    }
}
