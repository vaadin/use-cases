package com.example.uc4;

import java.io.ByteArrayInputStream;
import java.util.Comparator;
import java.util.List;

import com.example.data.Invoice;
import com.example.data.Invoices;
import com.example.pdf.InvoicePdf;
import com.example.views.MainLayout;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;

/**
 * UC4 — The monthly billing run.
 * <p>
 * Accounting does not download invoices one at a time; it wants the month as
 * one file, with continuous page numbers, in the order the invoices were
 * issued. The selection drives what goes in, and the link builds the document
 * when it is clicked, not when the page was rendered, so it always matches what
 * is ticked now.
 * <p>
 * Two limits show up here that the single-invoice case hides. A PDF cannot be
 * streamed: its cross-reference table is written last, so the entire batch
 * exists in memory before the browser sees a byte — the opposite of a CSV
 * export, which can be written row by row. And the progress callbacks
 * ({@code onProgress}, {@code whenComplete}) are delivered while the response
 * is being written, i.e. outside any client round trip: Flow wraps them in
 * {@code UI#access} for you, but the resulting changes only reach the browser
 * if the application has {@code @Push} enabled — which nothing enforces or
 * warns about.
 */
@Route(value = "uc4", layout = MainLayout.class)
@PageTitle("UC4 — The monthly billing run")
@Menu(order = 4, title = "UC4 — The monthly billing run")
@StyleSheet("invoicing.css")
public class BillingRunView extends VerticalLayout {

    private static final int INVOICE_COUNT = 12;

    private final Grid<Invoice> grid = new Grid<>();
    private final Anchor download;
    private final Span status = new Span();
    private final ProgressBar progress = new ProgressBar();

    public BillingRunView() {
        add(new H1("UC4 — The monthly billing run"));
        add(new Paragraph("Tick the invoices to bill and download them as one "
                + "document. Page numbering runs across the whole "
                + "batch, so \"Page 7 of 12\" means the same thing to "
                + "the reader as it does to the printer."));

        List<Invoice> invoices = Invoices.sample(INVOICE_COUNT);

        grid.setSelectionMode(Grid.SelectionMode.MULTI);
        grid.addThemeVariants(GridVariant.NO_BORDER);
        grid.addColumn(Invoice::number).setHeader("Invoice").setAutoWidth(true);
        grid.addColumn(Invoice::customer).setHeader("Customer")
                .setAutoWidth(true);
        grid.addColumn(invoice -> invoice.issued().toString())
                .setHeader("Issued").setAutoWidth(true);
        grid.addColumn(invoice -> InvoicePdf.money(invoice.gross()))
                .setHeader("Total").setAutoWidth(true);
        grid.setItems(invoices);
        grid.setAllRowsVisible(true);
        grid.asMultiSelect()
                .addValueChangeListener(event -> selectionChanged());

        download = new Anchor(batchHandler(), "Download the batch");
        download.setId("batch-link");

        progress.setId("batch-progress");
        progress.setWidth("12em");
        progress.setValue(0);

        status.setId("batch-status");

        HorizontalLayout actions = new HorizontalLayout(download, progress,
                status);
        actions.setAlignItems(Alignment.CENTER);

        add(grid, actions);
        grid.select(invoices.getFirst());
    }

    /**
     * The invoices that will go into the document, in issue order rather than
     * in the order they happened to be ticked.
     *
     * @return the selected invoices
     */
    public List<Invoice> selection() {
        return grid.getSelectedItems().stream()
                .sorted(Comparator.comparing(Invoice::number)).toList();
    }

    private void selectionChanged() {
        int selected = selection().size();
        // A PDF with no pages is a file no viewer will open, so an empty run
        // is not offered at all.
        download.setVisible(selected > 0);
        status.setText(
                selected == 0 ? "Nothing selected" : selected + " selected");
    }

    private DownloadHandler batchHandler() {
        return DownloadHandler.fromInputStream(event -> {
            byte[] pdf = InvoicePdf.render(selection());
            return new DownloadResponse(new ByteArrayInputStream(pdf),
                    "billing-run.pdf", InvoicePdf.CONTENT_TYPE, pdf.length);
        }, "billing-run.pdf").onProgress(this::updateProgress)
                .whenComplete(this::completed);
    }

    private void updateProgress(long transferred, long total) {
        if (total > 0) {
            progress.setValue((double) transferred / total);
        }
    }

    private void completed(boolean success) {
        progress.setValue(success ? 1 : 0);
        status.setText(
                success ? "Sent " + selection().size() + " invoices as one file"
                        : "The transfer failed");
    }
}
