package com.example.pdfviewer;

import java.util.List;

import tools.jackson.databind.node.ObjectNode;

import com.vaadin.browserless.ComponentTester;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.pdfviewer.PdfViewer;
import com.vaadin.flow.internal.JacksonUtils;

/**
 * Simulates the browser side of {@link PdfViewer} in browserless tests: a
 * document that loads or fails, the user scrolling to a page, and the pages the
 * server asked the viewer to scroll to.
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

    /**
     * Takes the pages the server asked the viewer to scroll to with
     * {@link PdfViewer#scrollToPage(int)} since the last call, in order. Other
     * calls queued for the viewer are dropped.
     *
     * @return the pages
     */
    public List<Integer> takeScrollToPage() {
        return takeCalls("scrollToPage").stream()
                .map(arguments -> (Integer) arguments.getFirst()).toList();
    }

    /**
     * Takes the number of times the server asked the viewer to download the
     * document with {@link PdfViewer#download()} since the last call. Other
     * calls queued for the viewer are dropped.
     *
     * @return the number of downloads
     */
    public int takeDownloads() {
        return takeCalls("download").size();
    }

    /**
     * Takes the arguments of each call of a function of the web component
     * queued with {@code callJsFunction}, and drops the other calls.
     */
    private List<List<Object>> takeCalls(String function) {
        UI ui = UI.getCurrent();
        // callJsFunction is queued before the response; run that step without
        // the rest of the round trip, which would discard the calls.
        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();
        return ui.getInternals().dumpPendingJavaScriptInvocations().stream()
                .filter(invocation -> invocation.getOwner() == getComponent()
                        .getElement().getNode())
                .map(invocation -> invocation.getInvocation().getParameters())
                .filter(parameters -> parameters.contains(function))
                .map(parameters -> parameters.subList(
                        parameters.indexOf(function) + 1, parameters.size()))
                .toList();
    }
}
