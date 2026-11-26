package com.example.pdfviewer;

import java.util.List;

import org.jspecify.annotations.Nullable;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.browserless.ComponentTester;
import com.vaadin.browserless.ExecutedJavaScript;
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
 * {@link #takeScrollToPage()} and {@link #takeDownloads()} read the JavaScript
 * from a capture passed to
 * {@link #PdfViewerTester(PdfViewer, ExecutedJavaScript)}. A test starts it
 * before it navigates, typically in a {@code @BeforeEach} method, so that it
 * also holds the calls sent while the view was navigated to.
 */
public class PdfViewerTester extends ComponentTester<PdfViewer> {

    private final @Nullable ExecutedJavaScript executed;
    private int read;

    /**
     * Creates a tester that only simulates the browser side.
     *
     * @param viewer
     *            the viewer
     */
    public PdfViewerTester(PdfViewer viewer) {
        this(viewer, null);
    }

    /**
     * Creates a tester that also reads what the server sent to the viewer.
     *
     * @param viewer
     *            the viewer
     * @param executed
     *            the capture of the JavaScript sent to the UI, or {@code null}
     *            when the test does not read it
     */
    public PdfViewerTester(PdfViewer viewer,
            @Nullable ExecutedJavaScript executed) {
        super(viewer);
        this.executed = executed;
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
        if (executed == null) {
            throw new IllegalStateException(
                    "Create the tester with a capture to read its JavaScript");
        }
        List<ExecutedJavaScript.Execution> all = executed.getExecutions();
        List<ExecutedJavaScript.Execution> sent = all.subList(read, all.size())
                .stream().filter(execution -> execution.getElement()
                        .equals(getComponent().getElement()))
                .toList();
        read = all.size();
        return sent.stream().map(ExecutedJavaScript.Execution::getParameters)
                .filter(parameters -> parameters.contains(function))
                .map(parameters -> parameters.subList(
                        parameters.indexOf(function) + 1, parameters.size()))
                .toList();
    }
}
