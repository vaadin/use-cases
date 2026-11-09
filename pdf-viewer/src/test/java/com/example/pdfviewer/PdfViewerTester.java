package com.example.pdfviewer;

import tools.jackson.databind.node.ObjectNode;

import com.vaadin.browserless.ComponentTester;
import com.vaadin.flow.component.pdfviewer.PdfViewer;
import com.vaadin.flow.internal.JacksonUtils;

/**
 * Simulates the browser side of {@link PdfViewer} in browserless tests: a
 * document that loads or fails, and the user scrolling to a page.
 * <p>
 * Browserless tests have no tester for the viewer (API-GAPS.md #5). This one
 * fires the same events the web component fires, with the data the Flow
 * component reads from them.
 */
public class PdfViewerTester extends ComponentTester<PdfViewer> {

    public PdfViewerTester(PdfViewer viewer) {
        super(viewer);
    }

    /** The document has loaded, as {@code document-load} reports it. */
    public void load(int pageCount, String title) {
        ObjectNode data = JacksonUtils.createObjectNode();
        data.put("event.detail.pageCount", pageCount);
        data.put("event.detail.title", title);
        fireDomEvent("document-load", data);
    }

    /**
     * The document could not be loaded.
     *
     * @param reason
     *            {@code network}, {@code invalid} or {@code password}
     */
    public void fail(String reason) {
        ObjectNode data = JacksonUtils.createObjectNode();
        data.put("event.detail.reason", reason);
        fireDomEvent("document-error", data);
    }

    /** The user scrolled until the given page takes up most of the view. */
    public void scrollTo(int page) {
        setPropertyAsUser("page", page);
    }
}
