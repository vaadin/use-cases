package com.example.uc1;

import java.util.HashSet;
import java.util.Set;

import com.example.common.UnreleasedApi;
import com.example.common.UseCaseDescription;
import com.example.documents.SampleDocuments;
import com.example.documents.SampleDocuments.Invoice;
import com.example.pdfviewer.PdfViewer;
import com.example.views.MainLayout;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * UC1 — Check before sending.
 * <p>
 * Before an invoice goes to a customer, someone looks at it. The viewer shows
 * the generated PDF itself, inside the application, so what the user approves
 * is byte for byte what the customer gets. The send button only turns on once
 * {@code document-load} reports that the document has rendered: a document that
 * failed to generate, or that the browser could not show, can not be sent
 * unseen.
 */
@UnreleasedApi("https://github.com/vaadin/web-components/pull/13053")
@Route(value = "uc1", layout = MainLayout.class)
@PageTitle("UC1 — Check before sending")
@UseCaseDescription("Showing a generated invoice inside the app before it is sent")
@Menu(order = 1, title = "UC1 — Check before sending")
@StyleSheet("uc1.css")
public class ReviewBeforeSendingView extends VerticalLayout {

    private final PdfViewer viewer = new PdfViewer();
    private final Button send = new Button("Send to customer");
    private final Span status = new Span();
    private final Set<String> sent = new HashSet<>();
    private final Select<Invoice> invoices = new Select<>();

    public ReviewBeforeSendingView() {
        addClassName("uc1-view");
        add(new H1("UC1 — Check before sending"));
        add(new Paragraph("Pick an invoice and read it in the viewer. This is "
                + "the PDF the customer will get, not a second rendering of "
                + "the same data. \"Send to customer\" turns on once the PDF "
                + "has loaded."));

        invoices.setLabel("Invoice");
        invoices.setWidth("20rem");
        invoices.setItems(SampleDocuments.INVOICES);
        invoices.setItemLabelGenerator(
                invoice -> invoice.number() + " — " + invoice.customer());
        invoices.addValueChangeListener(event -> show(event.getValue()));

        send.addThemeVariants(ButtonVariant.PRIMARY);
        send.addClickListener(event -> {
            sent.add(invoices.getValue().number());
            showStatus(invoices.getValue());
        });

        status.addClassName("uc1-status");
        HorizontalLayout actions = new HorizontalLayout(invoices, send, status);
        actions.setAlignItems(Alignment.BASELINE);
        actions.setWrap(true);

        viewer.addClassName("uc1-viewer");
        viewer.setWidthFull();
        viewer.setFileNameVisible(true);
        viewer.addDocumentLoadListener(
                event -> showStatus(invoices.getValue()));
        viewer.addDocumentErrorListener(event -> {
            send.setEnabled(false);
            status.setText(
                    "The PDF could not be shown, so it can not be " + "sent.");
        });

        add(actions, viewer);
        invoices.setValue(SampleDocuments.INVOICES.getFirst());
    }

    private void show(Invoice invoice) {
        send.setEnabled(false);
        status.setText("Loading " + invoice.fileName() + "…");
        viewer.setSrc(SampleDocuments.invoice(invoice).handler());
    }

    private void showStatus(Invoice invoice) {
        boolean alreadySent = sent.contains(invoice.number());
        send.setEnabled(!alreadySent);
        status.setText(alreadySent
                ? "Marked as sent to " + invoice.customer()
                        + " (the demo emails nothing)"
                : "Draft, " + SampleDocuments.money(invoice.total()));
    }
}
