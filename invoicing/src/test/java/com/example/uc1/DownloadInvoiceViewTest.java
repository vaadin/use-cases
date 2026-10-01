package com.example.uc1;

import com.example.data.Invoice;
import com.example.data.Invoices;
import com.example.pdf.InvoicePdf;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = DownloadInvoiceView.class)
class DownloadInvoiceViewTest extends SpringBrowserlessTest {

    private static final int LINK_COLUMN = 4;

    @Test
    void everyInvoiceIsListedWithADownloadLink() {
        navigate(DownloadInvoiceView.class);

        assertTrue(findInView(H1.class).all().stream().anyMatch(
                h -> "UC1 — Download the invoice".equals(h.getText())));

        Grid<?> grid = findInView(Grid.class).single();
        assertEquals(12, test(grid).size());
        assertEquals("2026-0001", test(grid).getCellText(0, 0));

        Anchor link = (Anchor) test(grid).getCellComponent(0, LINK_COLUMN);
        assertEquals("download-2026-0001", link.getId().orElseThrow());
        assertFalse(link.getHref().isEmpty(),
                "The handler must resolve to a URL the browser can fetch");
    }

    @Test
    void theRowShowsWhatTheDocumentWillTotal() {
        navigate(DownloadInvoiceView.class);
        Grid<?> grid = findInView(Grid.class).single();
        Invoice invoice = Invoices.sampleInvoice();

        // The row and the document have to agree; the row is what the user
        // decided on, the document is what the customer receives.
        assertEquals(InvoicePdf.money(invoice.gross()),
                test(grid).getCellText(0, 3));
        assertEquals(invoice.customer(), test(grid).getCellText(0, 1));
    }
}
