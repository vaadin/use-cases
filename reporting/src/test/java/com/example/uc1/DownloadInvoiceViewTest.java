package com.example.uc1;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.example.data.Invoice;
import com.example.data.Invoices;
import com.example.pdf.InvoicePdf;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = DownloadInvoiceView.class)
class DownloadInvoiceViewTest extends SpringBrowserlessTest {

    private static final int LINK_COLUMN = 4;

    @Test
    void everyInvoiceIsListedWithADownloadLink() throws IOException {
        navigate(DownloadInvoiceView.class);

        assertTrue(findInView(H1.class).all().stream().anyMatch(
                h -> "UC1 — Download the invoice".equals(h.getText())));

        Grid<?> grid = findInView(Grid.class).single();
        assertEquals(12, test(grid).size());
        assertEquals("2026-0001", test(grid).getCellText(0, 0));

        Anchor link = (Anchor) test(grid).getCellComponent(0, LINK_COLUMN);
        assertEquals("download-2026-0001", link.getId().orElseThrow());
        ByteArrayOutputStream pdf = new ByteArrayOutputStream();
        test(link).download(pdf);
        try (PDDocument document = Loader.loadPDF(pdf.toByteArray())) {
            assertTrue(
                    new PDFTextStripper().getText(document)
                            .contains("Invoice 2026-0001"),
                    "The link delivers the invoice of its own row");
        }
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

    @Test
    void thePanelShowsTheSelectedInvoice() {
        navigate(DownloadInvoiceView.class);
        Grid<?> grid = findInView(Grid.class).single();
        Div selected = findInView(Div.class).withId("selected-invoice")
                .single();

        assertEquals("Invoice 2026-0001", heading(selected));

        test(grid).select(2);
        assertEquals("Invoice " + test(grid).getCellText(2, 0),
                heading(selected));

        // Clicking the selected row again must not leave the panel empty.
        test(grid).deselect(2);
        assertEquals("Invoice " + test(grid).getCellText(2, 0),
                heading(selected));
    }

    private String heading(Div panel) {
        return find(H3.class).from(panel).single().getText();
    }
}
