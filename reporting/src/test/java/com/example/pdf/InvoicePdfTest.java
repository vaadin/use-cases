package com.example.pdf;

import java.io.IOException;
import java.util.List;

import com.example.data.Invoice;
import com.example.data.Invoices;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Reads the generated documents back with the same library that wrote them,
 * because "it produced some bytes" is not the same as "the customer can read
 * their invoice".
 */
class InvoicePdfTest {

    @Test
    void oneInvoiceCarriesItsNumbersOntoTheOnePage() throws IOException {
        Invoice invoice = Invoices.sampleInvoice();

        String text = textOf(InvoicePdf.render(invoice));

        assertTrue(text.contains("Invoice " + invoice.number()), text);
        assertTrue(text.contains(invoice.customer()));
        assertTrue(text.contains("Kettle & Cup Oy"),
                "The letterhead identifies the seller");
        assertTrue(text.contains(InvoicePdf.money(invoice.gross())),
                "The total due must be the one the UI shows");
        assertTrue(text.contains("Page 1 of 1"));
        invoice.lines()
                .forEach(line -> assertTrue(text.contains(line.description()),
                        line.description()));
    }

    @Test
    void aBatchIsOneDocumentWithContinuousPageNumbers() throws IOException {
        List<Invoice> invoices = Invoices.sample(3);

        byte[] pdf = InvoicePdf.render(invoices);

        try (PDDocument document = Loader.loadPDF(pdf)) {
            assertEquals(3, document.getNumberOfPages(),
                    "One page per invoice");
        }
        String text = textOf(pdf);
        invoices.forEach(invoice -> assertTrue(
                text.contains("Invoice " + invoice.number()),
                invoice.number()));
        assertTrue(text.contains("Page 1 of 3"));
        assertTrue(text.contains("Page 3 of 3"),
                "Numbering runs across the batch, not per invoice");
    }

    @Test
    void fileNameIsBuiltFromTheInvoiceNumber() {
        assertEquals("invoice-2026-0001.pdf",
                InvoicePdf.fileName(Invoices.sampleInvoice()));
    }

    private static String textOf(byte[] pdf) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(document);
        }
    }
}
