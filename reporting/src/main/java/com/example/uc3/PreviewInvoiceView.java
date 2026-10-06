package com.example.uc3;

import java.io.ByteArrayInputStream;
import java.util.List;

import com.example.data.Invoice;
import com.example.data.Invoices;
import com.example.pdf.InvoicePdf;
import com.example.views.InvoiceView;
import com.example.views.MainLayout;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.IFrame;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;

/**
 * UC3 — Check the document before it goes out.
 * <p>
 * Nobody sends an invoice they have not looked at. The preview on the right is
 * the actual generated PDF, not a second rendering of the same data:
 * {@code IFrame#setSrc(DownloadHandler)} points the frame at the same inline
 * handler the customer would get, so what the user approves is byte for byte
 * what leaves the building.
 * <p>
 * The catch is that the viewer is the browser's, not the application's. There
 * is no Vaadin PDF component, so the application cannot open the document at a
 * given page, highlight a line, or show anything at all on the mobile browsers
 * that refuse to render PDFs inline — it can only hand over a URL and hope. The
 * panel on the left is the application's own rendering of the same invoice,
 * which is what a reviewer actually reads.
 *
 * @see <a href=
 *      "https://github.com/vaadin/web-components/issues/7669">vaadin/web-components#7669
 *      — PDF Viewer component</a>
 */
@Route(value = "uc3", layout = MainLayout.class)
@PageTitle("UC3 — Check it before sending")
@Menu(order = 3, title = "UC3 — Check it before sending")
@StyleSheet("invoicing.css")
public class PreviewInvoiceView extends VerticalLayout {

    private static final int INVOICE_COUNT = 12;

    private final Div summary = new Div();
    private final IFrame preview = new IFrame();
    private final Span size = new Span();

    public PreviewInvoiceView() {
        add(new H1("UC3 — Check it before sending"));
        add(new Paragraph(
                "Pick an invoice: the left panel is the application's own "
                        + "rendering, the right one is the generated PDF as "
                        + "the customer's browser will show it."));

        List<Invoice> invoices = Invoices.sample(INVOICE_COUNT);

        Select<Invoice> select = new Select<>();
        select.setLabel("Invoice");
        select.setItems(invoices);
        select.setItemLabelGenerator(
                invoice -> invoice.number() + " — " + invoice.customer());
        select.setId("invoice-select");
        select.addValueChangeListener(event -> show(event.getValue()));

        size.setId("pdf-size");
        HorizontalLayout controls = new HorizontalLayout(select, size);
        controls.setAlignItems(Alignment.END);

        summary.setId("invoice-summary");
        summary.addClassName("preview-pane");
        preview.setId("pdf-preview");
        preview.addClassName("preview-pane");
        preview.getElement().setAttribute("title", "Invoice preview");

        HorizontalLayout panes = new HorizontalLayout(summary, preview);
        panes.setWidthFull();

        add(controls, panes);
        select.setValue(invoices.getFirst());
    }

    private void show(Invoice invoice) {
        summary.removeAll();
        summary.add(new InvoiceView(invoice));

        byte[] pdf = InvoicePdf.render(invoice);
        size.setText("The PDF is " + pdf.length + " bytes");
        // IFrame#setSrc switches the handler to inline for us; the file name
        // has to be the handler's own, because that is what it writes into
        // the header.
        preview.setSrc(DownloadHandler.fromInputStream(
                event -> new DownloadResponse(new ByteArrayInputStream(pdf),
                        InvoicePdf.fileName(invoice), InvoicePdf.CONTENT_TYPE,
                        pdf.length),
                InvoicePdf.fileName(invoice)));
    }
}
