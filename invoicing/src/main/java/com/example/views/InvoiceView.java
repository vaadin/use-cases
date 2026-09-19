package com.example.views;

import com.example.data.Invoice;
import com.example.data.InvoiceLine;
import com.example.pdf.InvoicePdf;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.Table;
import com.vaadin.flow.component.html.TableRow;

/**
 * The invoice as the application shows it on screen — the same numbers the
 * generated PDF carries, so that a user can check the document before sending
 * it and a test can compare the two.
 */
public class InvoiceView extends Div {

    /**
     * @param invoice
     *            the invoice to render
     */
    public InvoiceView(Invoice invoice) {
        addClassName("invoice");

        add(new H3("Invoice " + invoice.number()));
        Div meta = new Div();
        meta.addClassName("invoice-meta");
        meta.add(new Span(invoice.customer()),
                new Span("Due " + invoice.due()));
        add(meta);

        Table table = new Table();
        table.addClassName("invoice-table");
        table.setWidthFull();
        TableRow header = table.addHeaderRow();
        header.addColumnHeaderCell("Description");
        header.addColumnHeaderCell("Qty").addClassName("numeric");
        header.addColumnHeaderCell("VAT").addClassName("numeric");
        header.addColumnHeaderCell("Total").addClassName("numeric");

        for (InvoiceLine line : invoice.lines()) {
            TableRow row = table.addRow();
            row.addDataCell(line.description());
            row.addDataCell(Integer.toString(line.quantity()))
                    .addClassName("numeric");
            row.addDataCell(line.vatPercent() + " %").addClassName("numeric");
            row.addDataCell(InvoicePdf.money(line.gross()))
                    .addClassName("numeric");
        }

        TableRow total = table.addFooterRow();
        total.addColumnHeaderCell("Total due").setColspan(3);
        total.addDataCell(InvoicePdf.money(invoice.gross()))
                .addClassName("numeric");
        add(table);
    }
}
