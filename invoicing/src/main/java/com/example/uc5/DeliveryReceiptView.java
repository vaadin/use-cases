package com.example.uc5;

import java.io.ByteArrayInputStream;

import com.example.data.Invoice;
import com.example.data.InvoiceBook;
import com.example.data.Invoices;
import com.example.pdf.InvoicePdf;
import com.example.views.MainLayout;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;

/**
 * UC5 — Know that the invoice actually went out.
 * <p>
 * "Sent" is a fact about the world, so it must not be recorded when the user
 * clicks a link — a click is not a delivery, and a cancelled or failed transfer
 * is not either. {@code whenComplete} is the only place that knows, and it is
 * worth using: the book below is only written when the last byte has left, and
 * it remembers how many bytes those were.
 * <p>
 * Two things make this harder than it looks. The callback is delivered while
 * the response is being written, outside any client round trip: Flow runs it
 * through {@code UI#access} already, but the repainted badge only reaches the
 * browser because the application enables {@code @Push}. And because the state
 * is shared between users, a second browser has to be told too, which is why
 * {@link InvoiceBook} is application-scoped and this view re-reads it on every
 * attach rather than caching it.
 */
@Route(value = "uc5", layout = MainLayout.class)
@PageTitle("UC5 — Know that it arrived")
@Menu(order = 5, title = "UC5 — Know that it arrived")
@StyleSheet("invoicing.css")
public class DeliveryReceiptView extends VerticalLayout {

    private static final int INVOICE_COUNT = 12;

    private final InvoiceBook book;
    private final Grid<Invoice> grid = new Grid<>();

    public DeliveryReceiptView(InvoiceBook book) {
        this.book = book;

        add(new H1("UC5 — Know that it arrived"));
        add(new Paragraph(
                "Downloading an invoice marks it as sent — but only once the "
                        + "transfer has completed, and for every user, not "
                        + "just this browser tab."));

        grid.addColumn(Invoice::number).setHeader("Invoice").setAutoWidth(true);
        grid.addColumn(Invoice::customer).setHeader("Customer")
                .setAutoWidth(true);
        grid.addComponentColumn(this::deliveryBadge).setHeader("Delivery")
                .setAutoWidth(true);
        grid.addComponentColumn(this::downloadLink).setHeader("PDF")
                .setAutoWidth(true);
        grid.setItems(Invoices.sample(INVOICE_COUNT));
        grid.setAllRowsVisible(true);

        Button reset = new Button("Forget all deliveries", event -> {
            book.clear();
            refresh();
        });
        reset.setId("reset-button");

        add(grid, reset);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        // Another session may have sent an invoice since this view was built.
        refresh();
    }

    /**
     * Re-reads the delivery book and repaints the badges.
     */
    public void refresh() {
        grid.getDataProvider().refreshAll();
    }

    private Span deliveryBadge(Invoice invoice) {
        Span badge = new Span(book.delivery(invoice.number())
                .map(delivery -> "Sent, " + delivery.bytes() + " bytes")
                .orElse("Not sent"));
        badge.addClassName("status-badge");
        badge.setClassName("sent", book.delivery(invoice.number()).isPresent());
        badge.setId("delivery-" + invoice.number());
        return badge;
    }

    private Anchor downloadLink(Invoice invoice) {
        DownloadHandler handler = DownloadHandler.fromInputStream(event -> {
            byte[] pdf = InvoicePdf.render(invoice);
            return new DownloadResponse(new ByteArrayInputStream(pdf),
                    InvoicePdf.fileName(invoice), InvoicePdf.CONTENT_TYPE,
                    pdf.length);
        }, InvoicePdf.fileName(invoice)).whenComplete((context, success) -> {
            if (Boolean.TRUE.equals(success)) {
                recordDelivery(invoice, context.contentLength());
            }
        });
        Anchor link = new Anchor(handler, "Download PDF");
        link.setId("download-" + invoice.number());
        return link;
    }

    private void recordDelivery(Invoice invoice, long bytes) {
        book.markSent(invoice.number(), bytes);
        // Flow delivers this callback with the UI lock held; pushing the
        // repaint to the browser is what @Push on the application is for.
        refresh();
    }
}
