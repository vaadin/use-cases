package com.example.pdfviewer;

import java.util.ArrayList;
import java.util.List;

import tools.jackson.databind.node.ObjectNode;

import com.vaadin.browserless.BrowserlessTestConfig;
import com.vaadin.browserless.ComponentTester;
import com.vaadin.browserless.ExecutedJavaScript;
import com.vaadin.browserless.JavaScriptHandler;
import com.vaadin.flow.component.ComponentUtil;
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
 * <p>
 * {@link #takeScrollToPage()} and {@link #takeDownloads()} need the
 * {@link Browser} recorder, registered with
 * {@code @BrowserlessTestConfig(javaScriptHandlers = PdfViewerTester.Browser.class)},
 * so that they also see the calls sent while the view was navigated to.
 */
public class PdfViewerTester extends ComponentTester<PdfViewer> {

    /**
     * Records the JavaScript sent to the browser of each UI, so that the
     * {@code take} methods can return it. Register it with
     * {@link BrowserlessTestConfig}.
     */
    public static final class Browser implements JavaScriptHandler {

        @Override
        public void handle(ExecutedJavaScript.Execution execution) {
            received(execution.getUI()).add(execution);
        }
    }

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
     * calls sent to the viewer are dropped.
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
     * calls sent to the viewer are dropped.
     *
     * @return the number of downloads
     */
    public int takeDownloads() {
        return takeCalls("download").size();
    }

    /**
     * Takes the arguments of each call of a function of the web component sent
     * with {@code callJsFunction}, and drops the other calls sent to the
     * viewer.
     */
    private List<List<Object>> takeCalls(String function) {
        UI ui = UI.getCurrent();
        // Starting a capture sends whatever is still pending to the browser
        ExecutedJavaScript.capture(ui).stop();
        List<ExecutedJavaScript.Execution> received = received(ui);
        List<ExecutedJavaScript.Execution> taken = received.stream()
                .filter(execution -> execution.getElement()
                        .equals(getComponent().getElement()))
                .toList();
        received.removeAll(taken);
        return taken.stream().map(ExecutedJavaScript.Execution::getParameters)
                .filter(parameters -> parameters.contains(function))
                .map(parameters -> parameters.subList(
                        parameters.indexOf(function) + 1, parameters.size()))
                .toList();
    }

    @SuppressWarnings("unchecked")
    private static List<ExecutedJavaScript.Execution> received(UI ui) {
        List<ExecutedJavaScript.Execution> received = (List<ExecutedJavaScript.Execution>) ComponentUtil
                .getData(ui, Browser.class.getName());
        if (received == null) {
            received = new ArrayList<>();
            ComponentUtil.setData(ui, Browser.class.getName(), received);
        }
        return received;
    }
}
