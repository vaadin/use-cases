package com.example.uc1;

import java.io.ByteArrayInputStream;
import java.util.List;

import com.example.data.Invoice;
import com.example.data.Invoices;
import com.example.pdf.InvoicePdf;
import com.example.views.InvoiceView;
import com.example.views.MainLayout;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;

/**
 * UC1 — Download the invoice as a PDF.
 * <p>
 * The everyday case: a list of invoices, and a link that hands the customer's
 * copy to the browser. Flow 25's {@code DownloadHandler} is what makes this
 * short — {@code fromInputStream} with a file name sets the
 * {@code Content-Disposition}, the content type and the length, and the handler
 * is bound to the {@link Anchor} that carries it, so it is session-scoped and
 * disappears with the view.
 * <p>
 * What the framework does not do is the document. Every millimetre of the
 * invoice below is application code ({@link InvoicePdf}), and the bytes have to
 * exist in full before the first one is written, because a PDF's
 * cross-reference table lives at the end of the file.
 */
@Route(value = "uc1", layout = MainLayout.class)
@PageTitle("UC1 — Download the invoice")
@Menu(order = 1, title = "UC1 — Download the invoice")
@StyleSheet("invoicing.css")
public class DownloadInvoiceView extends VerticalLayout {

    private static final int INVOICE_COUNT = 12;

    public DownloadInvoiceView() {
        add(new H1("UC1 — Download the invoice"));
        add(new Paragraph(
                "Each row links to a PDF generated on the spot from the same "
                        + "data the row shows. The link is an ordinary "
                        + "anchor: the browser downloads it without a round "
                        + "trip through the UI, and the file name comes from "
                        + "the invoice number."));

        List<Invoice> invoices = Invoices.sample(INVOICE_COUNT);

        Grid<Invoice> grid = new Grid<>();
        grid.addColumn(Invoice::number).setHeader("Invoice").setAutoWidth(true);
        grid.addColumn(Invoice::customer).setHeader("Customer")
                .setAutoWidth(true);
        grid.addColumn(invoice -> invoice.due().toString()).setHeader("Due")
                .setAutoWidth(true);
        grid.addColumn(invoice -> InvoicePdf.money(invoice.gross()))
                .setHeader("Total").setAutoWidth(true);
        grid.addComponentColumn(DownloadInvoiceView::downloadLink)
                .setHeader("PDF").setAutoWidth(true);
        grid.setItems(invoices);
        grid.setAllRowsVisible(true);
        add(grid);

        Div preview = new Div(new InvoiceView(invoices.getFirst()));
        preview.setId("first-invoice");
        add(preview);
    }

    /**
     * The download link for one invoice, and the whole of the delivery side of
     * this use case.
     *
     * @param invoice
     *            the invoice to offer
     * @return an anchor that downloads the invoice as a PDF
     */
    public static Anchor downloadLink(Invoice invoice) {
        // The file name is given to the handler, not only to the response:
        // that is what ends up in the Content-Disposition header.
        DownloadHandler handler = DownloadHandler.fromInputStream(event -> {
            byte[] pdf = InvoicePdf.render(invoice);
            return new DownloadResponse(new ByteArrayInputStream(pdf),
                    InvoicePdf.fileName(invoice), InvoicePdf.CONTENT_TYPE,
                    pdf.length);
        }, InvoicePdf.fileName(invoice));
        Anchor link = new Anchor(handler, "Download PDF");
        link.setId("download-" + invoice.number());
        return link;
    }
}
