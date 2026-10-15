package com.example.uc2;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName;

/**
 * Invoices to try the form with, rendered as PDF files when asked for, so the
 * repository holds no binary samples. Each one is laid out differently, as
 * invoices from different vendors are.
 */
public final class SampleInvoices {

    /**
     * A sample invoice: the label of its button, its file name and the lines
     * printed on it, top to bottom. A line starting with {@code #} is printed
     * as a heading.
     */
    public record Sample(String label, String fileName, List<String> lines) {

        public byte[] toPdf() {
            return render(lines);
        }
    }

    public static final Sample OFFICE_SUPPLIES = new Sample(
            "Office supplies invoice", "northwind-invoice-2026-0412.pdf",
            List.of("#Northwind Office Supplies GmbH",
                    "Hafenstrasse 12, 20457 Hamburg, Germany",
                    "VAT ID DE 812 345 678", "", "#INVOICE",
                    "Invoice no.: NW-2026-0412", "Invoice date: 03.09.2026",
                    "Payment due: 03.10.2026", "",
                    "Bill to: Acme Corp, Accounts Payable, Helsinki", "",
                    "Item                         Qty    Unit price      Amount",
                    "Ergonomic office chair         4      299.00     1196.00",
                    "Desk lamp, LED                10       49.00      490.00",
                    "Monitor arm, dual              6       89.00      534.00",
                    "",
                    "Net amount                                  EUR 2220.00",
                    "VAT 19 %                                    EUR  421.80",
                    "Total due                                   EUR 2641.80",
                    "", "Please transfer to IBAN DE89 3704 0044 0532 0130 00",
                    "quoting the invoice number."));

    public static final Sample CONSULTING = new Sample(
            "Consulting invoice (totals do not add up)",
            "fabrikam-invoice-7781.pdf",
            List.of("#Fabrikam Consulting Ltd", "1 Canal Street, Manchester",
                    "", "Invoice 7781", "Issued 15 September 2026",
                    "Terms: 14 days net (due 29 September 2026)", "",
                    "Workshop facilitation, 3 days            GBP 3,600.00",
                    "Travel expenses                          GBP   240.00", "",
                    "Subtotal                                 GBP 3,840.00",
                    "VAT at 20%                               GBP   768.00",
                    "TOTAL                                    GBP 4,680.00", "",
                    "Bank: GB29 NWBK 6016 1331 9268 19"));

    public static final List<Sample> ALL = List.of(OFFICE_SUPPLIES, CONSULTING);

    private SampleInvoices() {
    }

    private static byte[] render(List<String> lines) {
        try (PDDocument document = new PDDocument();
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            PDType1Font regular = new PDType1Font(FontName.COURIER);
            PDType1Font bold = new PDType1Font(FontName.HELVETICA_BOLD);
            try (PDPageContentStream content = new PDPageContentStream(document,
                    page)) {
                float y = page.getMediaBox().getHeight() - 60;
                for (String line : lines) {
                    boolean heading = line.startsWith("#");
                    content.beginText();
                    content.setFont(heading ? bold : regular,
                            heading ? 16 : 10);
                    content.newLineAtOffset(50, y);
                    content.showText(heading ? line.substring(1) : line);
                    content.endText();
                    y -= heading ? 24 : 15;
                }
            }
            document.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
