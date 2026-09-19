package com.example.uc4;

import java.io.IOException;
import java.util.List;

import com.example.data.Invoice;
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
import com.vaadin.flow.component.html.Span;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = BillingRunView.class)
class BillingRunViewTest extends SpringBrowserlessTest {

    @Test
    void theRunStartsWithTheFirstInvoiceTicked() {
        BillingRunView view = navigate(BillingRunView.class);

        assertEquals(List.of("2026-0001"),
                view.selection().stream().map(Invoice::number).toList());
        assertEquals("1 selected",
                findInView(Span.class).id("batch-status").getText());
        assertFalse(
                findInView(Anchor.class).id("batch-link").getHref().isEmpty());
    }

    @Test
    void theBatchIsOneDocumentInIssueOrderWhateverTheTickingOrder()
            throws IOException {
        BillingRunView view = navigate(BillingRunView.class);
        Grid<?> grid = findInView(Grid.class).single();
        test(grid).deselectAll();

        // Tick them back to front; the run must not come out that way.
        test(grid).select(4);
        test(grid).select(0);
        test(grid).select(2);

        assertEquals(List.of("2026-0001", "2026-0003", "2026-0005"),
                view.selection().stream().map(Invoice::number).toList());
        assertEquals("3 selected",
                findInView(Span.class).id("batch-status").getText());

        byte[] pdf = InvoicePdf.render(view.selection());
        try (PDDocument document = Loader.loadPDF(pdf)) {
            assertEquals(3, document.getNumberOfPages());
            String text = new PDFTextStripper().getText(document);
            assertTrue(text.indexOf("Invoice 2026-0001") < text
                    .indexOf("Invoice 2026-0005"));
            assertTrue(text.contains("Page 3 of 3"));
        }
    }

    @Test
    void anEmptyRunIsNotOfferedAtAll() {
        navigate(BillingRunView.class);
        assertTrue(findInView(Anchor.class).withId("batch-link").exists());

        test(findInView(Grid.class).single()).deselectAll();

        assertFalse(findInView(Anchor.class).withId("batch-link").exists(),
                "A PDF with no pages is a file no viewer opens, so the link "
                        + "has to go away with the selection");
        assertEquals("Nothing selected",
                findInView(Span.class).id("batch-status").getText());
    }
}
