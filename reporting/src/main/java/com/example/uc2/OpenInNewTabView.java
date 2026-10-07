package com.example.uc2;

import java.io.ByteArrayInputStream;

import com.example.MissingAPI;
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
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
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
 * The awkward half is doing the same thing from server code — after saving a
 * draft, say, or from a context menu item. {@code Page#open} needs a URL, and a
 * {@code DownloadHandler} has no URL until it has been bound to an element;
 * nothing exposes it afterwards either, except {@code Anchor#getHref()}. So the
 * second button here works the way applications have to: a hidden anchor that
 * the server clicks through JavaScript
 * ({@link MissingAPI#openInNewTab(Anchor)}).
 *
 * @see <a href="https://github.com/vaadin/flow/issues/21929">vaadin/flow#21929
 *      — Improve the DownloadHandler API for opening a file in a new tab</a>
 */
@Route(value = "uc2", layout = MainLayout.class)
@PageTitle("UC2 — Open it in a new tab")
@Menu(order = 2, title = "UC2 — Open it in a new tab")
@StyleSheet("invoicing.css")
public class OpenInNewTabView extends VerticalLayout {

    private final Anchor hiddenLink;
    private final Span href = new Span(
            "(the URL exists only once the " + "anchor is attached)");

    public OpenInNewTabView() {
        Invoice invoice = Invoices.sampleInvoice();

        add(new H1("UC2 — Open it in a new tab"));
        add(new Paragraph(
                "The same generated PDF, delivered inline instead of as an "
                        + "attachment. The link opens it in a new tab by "
                        + "itself; the button has to go the long way round, "
                        + "because the server cannot ask a DownloadHandler "
                        + "for its URL."));

        Anchor view = new Anchor(handler(invoice), AttachmentType.INLINE,
                "View invoice " + invoice.number());
        view.setTarget(AnchorTarget.BLANK);
        view.setId("view-link");

        hiddenLink = new Anchor(handler(invoice), AttachmentType.INLINE, "");
        hiddenLink.setTarget(AnchorTarget.BLANK);
        hiddenLink.setId("hidden-link");
        hiddenLink.getStyle().set("display", "none");

        Button open = new Button("Open from the server",
                event -> openFromServer());
        open.addThemeVariants(ButtonVariant.PRIMARY);
        open.setId("open-button");

        HorizontalLayout actions = new HorizontalLayout(view, open);
        actions.setAlignItems(Alignment.CENTER);

        Div url = new Div(new Span("Resolved URL: "), href);
        url.addClassName("resolved-url");
        href.setId("resolved-url");

        add(actions, url, hiddenLink);
    }

    private void openFromServer() {
        // Anchor#getHref() is the only place the handler's URL surfaces, and
        // only after the anchor has been attached.
        href.setText(hiddenLink.getHref());
        MissingAPI.openInNewTab(hiddenLink);
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
