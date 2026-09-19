package com.example.pdf;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import com.example.data.Invoice;
import com.example.data.InvoiceLine;
import com.example.data.Invoices;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName;

/**
 * Renders invoices as PDF.
 * <p>
 * Deliberately plain PDFBox rather than a reporting framework: the point of
 * this module is what an application has to do to get a business document out
 * of Vaadin, and every line of layout here is a line Vaadin does not help with.
 */
public final class InvoicePdf {

    /** The content type to serve the generated documents with. */
    public static final String CONTENT_TYPE = "application/pdf";

    private static final DateTimeFormatter DATE = DateTimeFormatter
            .ofPattern("d MMM yyyy", Locale.ENGLISH);

    private static final float MARGIN = 56;
    private static final float LINE_HEIGHT = 15;

    /** Where each column of the lines table starts, from the left margin. */
    private static final float[] COLUMNS = { 0, 250, 300, 380, 460 };

    private InvoicePdf() {
    }

    /**
     * Renders one invoice.
     *
     * @param invoice
     *            the invoice to render
     * @return the PDF document as bytes
     */
    public static byte[] render(Invoice invoice) {
        return render(List.of(invoice));
    }

    /**
     * Renders several invoices into one document with continuous page numbering
     * — a month's billing run as a single file.
     *
     * @param invoices
     *            the invoices to render, in the order they should appear
     * @return the PDF document as bytes
     */
    public static byte[] render(List<Invoice> invoices) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        write(invoices, out);
        return out.toByteArray();
    }

    /**
     * Renders several invoices to an output stream.
     * <p>
     * Note what this cannot do: PDF's cross-reference table lives at the end of
     * the file, so the whole document has to exist before its first byte can be
     * written. Unlike a CSV, a PDF cannot be streamed row by row, and a
     * thousand-invoice batch is a thousand invoices in memory.
     *
     * @param invoices
     *            the invoices to render
     * @param out
     *            the stream to write the document to
     */
    public static void write(List<Invoice> invoices, OutputStream out) {
        try (PDDocument document = new PDDocument()) {
            for (Invoice invoice : invoices) {
                renderInvoice(document, invoice);
            }
            addPageNumbers(document);
            document.save(out);
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Could not render the invoice document", e);
        }
    }

    /**
     * The file name an invoice is delivered under.
     *
     * @param invoice
     *            the invoice
     * @return a file name of the form {@code invoice-2026-0001.pdf}
     */
    public static String fileName(Invoice invoice) {
        return "invoice-" + invoice.number() + ".pdf";
    }

    /**
     * Formats an amount the same way in the document and in the UI, so the two
     * can be compared.
     *
     * @param amount
     *            the amount to format
     * @return the amount with two decimals and a euro sign
     */
    public static String money(BigDecimal amount) {
        return String.format(Locale.ROOT, "%,.2f EUR", amount);
    }

    private static void renderInvoice(PDDocument document, Invoice invoice)
            throws IOException {
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);

        try (PDPageContentStream content = new PDPageContentStream(document,
                page)) {
            float y = page.getMediaBox().getHeight() - MARGIN;

            y = letterhead(content, y);
            y = addresses(content, invoice, y - LINE_HEIGHT);
            y = heading(content, invoice, y - LINE_HEIGHT * 2);
            y = lines(content, invoice, y - LINE_HEIGHT);
            totals(content, invoice, y - LINE_HEIGHT);
        }
    }

    private static float letterhead(PDPageContentStream content, float y)
            throws IOException {
        text(content, Invoices.SELLER.getFirst(), MARGIN, y,
                FontName.HELVETICA_BOLD, 16);
        float line = y - LINE_HEIGHT;
        for (String detail : Invoices.SELLER.subList(1,
                Invoices.SELLER.size())) {
            text(content, detail, MARGIN, line, FontName.HELVETICA, 9);
            line -= LINE_HEIGHT * 0.8f;
        }
        return line;
    }

    private static float addresses(PDPageContentStream content, Invoice invoice,
            float y) throws IOException {
        float line = y;
        for (String addressLine : invoice.address()) {
            text(content, addressLine, MARGIN, line, FontName.HELVETICA, 11);
            line -= LINE_HEIGHT;
        }
        return line;
    }

    private static float heading(PDPageContentStream content, Invoice invoice,
            float y) throws IOException {
        text(content, "Invoice " + invoice.number(), MARGIN, y,
                FontName.HELVETICA_BOLD, 14);
        float line = y - LINE_HEIGHT * 1.4f;
        text(content,
                "Issued " + invoice.issued().format(DATE) + "    Due "
                        + invoice.due().format(DATE),
                MARGIN, line, FontName.HELVETICA, 10);
        return line;
    }

    private static float lines(PDPageContentStream content, Invoice invoice,
            float y) throws IOException {
        float line = y - LINE_HEIGHT;
        row(content, line, FontName.HELVETICA_BOLD, "Description", "Qty",
                "Unit price", "VAT", "Total");
        line -= LINE_HEIGHT * 0.4f;
        rule(content, line);

        for (InvoiceLine invoiceLine : invoice.lines()) {
            line -= LINE_HEIGHT;
            row(content, line, FontName.HELVETICA, invoiceLine.description(),
                    Integer.toString(invoiceLine.quantity()),
                    money(invoiceLine.unitPrice()),
                    invoiceLine.vatPercent() + " %",
                    money(invoiceLine.gross()));
        }
        line -= LINE_HEIGHT * 0.6f;
        rule(content, line);
        return line;
    }

    private static void totals(PDPageContentStream content, Invoice invoice,
            float y) throws IOException {
        float line = y - LINE_HEIGHT;
        total(content, line, FontName.HELVETICA, "Net", invoice.net());
        line -= LINE_HEIGHT;
        total(content, line, FontName.HELVETICA, "VAT", invoice.vat());
        line -= LINE_HEIGHT;
        total(content, line, FontName.HELVETICA_BOLD, "Total due",
                invoice.gross());
    }

    private static void total(PDPageContentStream content, float y,
            FontName font, String label, BigDecimal amount) throws IOException {
        text(content, label, MARGIN + COLUMNS[3], y, font, 11);
        text(content, money(amount), MARGIN + COLUMNS[4], y, font, 11);
    }

    private static void row(PDPageContentStream content, float y, FontName font,
            String... cells) throws IOException {
        for (int column = 0; column < cells.length; column++) {
            text(content, cells[column], MARGIN + COLUMNS[column], y, font, 10);
        }
    }

    private static void rule(PDPageContentStream content, float y)
            throws IOException {
        content.moveTo(MARGIN, y);
        content.lineTo(PDRectangle.A4.getWidth() - MARGIN, y);
        content.stroke();
    }

    private static void text(PDPageContentStream content, String value, float x,
            float y, FontName font, float size) throws IOException {
        content.beginText();
        content.setFont(new PDType1Font(font), size);
        content.newLineAtOffset(x, y);
        content.showText(value);
        content.endText();
    }

    private static void addPageNumbers(PDDocument document) throws IOException {
        int pageCount = document.getNumberOfPages();
        for (int index = 0; index < pageCount; index++) {
            PDPage page = document.getPage(index);
            try (PDPageContentStream content = new PDPageContentStream(document,
                    page, PDPageContentStream.AppendMode.APPEND, true)) {
                text(content, "Page " + (index + 1) + " of " + pageCount,
                        MARGIN, MARGIN / 2, FontName.HELVETICA, 9);
            }
        }
    }
}
