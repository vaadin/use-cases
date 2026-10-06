package com.example.uc3;

import com.example.data.Invoice;
import com.example.data.Invoices;
import com.example.pdf.InvoicePdf;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.IFrame;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.select.Select;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = PreviewInvoiceView.class)
class PreviewInvoiceViewTest extends SpringBrowserlessTest {

    @Test
    void theFirstInvoiceIsPreviewedOnArrival() {
        navigate(PreviewInvoiceView.class);
        Invoice invoice = Invoices.sampleInvoice();

        String summary = findInView(Div.class).id("invoice-summary")
                .getElement().getTextRecursively();
        assertTrue(summary.contains("Invoice " + invoice.number()), summary);
        assertTrue(summary.contains(InvoicePdf.money(invoice.gross())),
                "The panel shows what the document totals");

        assertFalse(src().isEmpty(),
                "The frame shows the generated document, not a copy of it");
        assertTrue(findInView(Span.class).id("pdf-size").getText()
                .contains("bytes"));
    }

    @Test
    void choosingAnotherInvoiceRegeneratesTheDocument() {
        navigate(PreviewInvoiceView.class);
        String firstSrc = src();
        Invoice second = Invoices.sample(2).getLast();

        test(findInView(Select.class).id("invoice-select"))
                .selectItem(second.number() + " — " + second.customer());

        assertTrue(findInView(Div.class).id("invoice-summary").getElement()
                .getTextRecursively().contains("Invoice " + second.number()));
        assertNotEquals(firstSrc, src(),
                "A new preview means a new document, not a cached URL");
    }

    private String src() {
        return findInView(IFrame.class).id("pdf-preview").getElement()
                .getAttribute("src");
    }
}
