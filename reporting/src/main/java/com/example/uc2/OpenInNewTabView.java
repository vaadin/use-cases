package com.example.uc2;

import java.io.ByteArrayInputStream;

import com.example.common.UseCaseDescription;
import com.example.data.Invoice;
import com.example.data.Invoices;
import com.example.pdf.InvoicePdf;
import com.example.views.MainLayout;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.AnchorTarget;
import com.vaadin.flow.component.html.AttachmentType;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.newtab.OpenInNewTab;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;

/**
 * UC2 — Show the invoice instead of downloading it.
 * <p>
 * A customer clicking "View invoice" expects the document to open, not to land
 * in the downloads folder. That is the difference between
 * {@code Content-Disposition: attachment} and {@code inline}, and
 * {@link AttachmentType#INLINE} on the anchor is all it takes.
 * <p>
 * A button does the same with {@link OpenInNewTab#onClick} and the handler: the
 * tab opens in the browser's own click handler, so popup blockers let it
 * through, and the handler is switched to inline delivery.
 * <p>
 * What is still missing is opening the document when the server decides to —
 * after saving a draft, say, or after a confirm dialog. {@code Page#open} needs
 * a URL, and a {@code DownloadHandler} has none the server can read back
 * ({@code API-GAPS.md} #2).
 *
 * @see <a href="https://github.com/vaadin/flow/issues/21929">vaadin/flow#21929
 *      — Improve the DownloadHandler API for opening a file in a new tab</a>
 */
@Route(value = "uc2", layout = MainLayout.class)
@PageTitle("UC2 — Open it in a new tab")
@UseCaseDescription("Opening a generated document in a new tab instead of downloading it")
@Menu(order = 2, title = "UC2 — Open it in a new tab")
@StyleSheet("invoicing.css")
public class OpenInNewTabView extends VerticalLayout {

    public OpenInNewTabView() {
        Invoice invoice = Invoices.sampleInvoice();

        add(new H1("UC2 — Open it in a new tab"));
        add(new Paragraph(
                "The same generated PDF, delivered inline instead of as an "
                        + "attachment. The link opens it in a new tab by "
                        + "itself; OpenInNewTab gives the button the same "
                        + "behaviour."));

        Anchor view = new Anchor(handler(invoice), AttachmentType.INLINE,
                "View invoice " + invoice.number());
        view.setTarget(AnchorTarget.BLANK);
        view.setId("view-link");

        Button open = new Button("Open in a new tab");
        OpenInNewTab.onClick(open, handler(invoice));
        open.addThemeVariants(ButtonVariant.PRIMARY);
        open.setId("open-button");

        HorizontalLayout actions = new HorizontalLayout(view, open);
        actions.setAlignItems(Alignment.CENTER);

        add(actions);
    }

    private static DownloadHandler handler(Invoice invoice) {
        // The file name has to go to fromInputStream, not only into the
        // DownloadResponse: it is the name the handler writes into the
        // Content-Disposition header before the callback even runs.
        return DownloadHandler.fromInputStream(event -> {
            byte[] pdf = InvoicePdf.render(invoice);
            return new DownloadResponse(new ByteArrayInputStream(pdf),
                    InvoicePdf.fileName(invoice), InvoicePdf.CONTENT_TYPE,
                    pdf.length);
        }, InvoicePdf.fileName(invoice)).inline();
    }
}
