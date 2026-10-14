package com.example.pdfviewer;

import java.util.Arrays;

import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.DomEvent;
import com.vaadin.flow.component.EventData;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.server.streams.AbstractDownloadHandler;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.shared.Registration;

/**
 * A Flow wrapper for the prototype {@code <vaadin-pdf-viewer>} web component.
 * <p>
 * Flow Components has no PDF viewer yet (API-GAPS.md #1), so this module builds
 * the one it needs. It covers the web component's API: the document
 * ({@link #setSrc(DownloadHandler)}), the current page, the zoom, the sidebar,
 * the toolbar, printing, the texts and the load events. The npm package is the
 * pkg.pr.new preview build of the web components branch that adds the
 * component, at the commit that the Flow Components snapshot in this module's
 * pom takes its other web components from (API-GAPS.md #3).
 * <p>
 * Migrate by deleting this package and switching to the Flow component once it
 * exists. Its API is likely to differ in details (signal bindings, a typed
 * zoom), see API-GAPS.md.
 */
@Tag("vaadin-pdf-viewer")
@NpmPackage(value = "@vaadin/pdf-viewer", version = PdfViewer.PREVIEW_URL)
@JsModule("@vaadin/pdf-viewer/vaadin-pdf-viewer.js")
public class PdfViewer extends Component implements HasSize, HasStyle {

    /**
     * The pkg.pr.new preview of the web component, at the commit whose other
     * web components the Flow Components snapshot uses. Every preview package
     * depends on the others through URLs with the same commit, so the two must
     * match or npm installs a second copy of the shared packages.
     */
    static final String PREVIEW_URL = "https://pkg.pr.new/vaadin/web-components/@vaadin/pdf-viewer@dcd878f1286c04a77ca05219d530dcaa802a41dd";

    /** How the viewer fits the pages, see {@link #setZoom(Zoom)}. */
    public enum Zoom {
        /** Fits the width of the first page to the viewer (the default). */
        PAGE_WIDTH("page-width"),
        /** Fits the whole first page into the viewer. */
        PAGE_FIT("page-fit");

        private final String value;

        Zoom(String value) {
            this.value = value;
        }

        static @Nullable Zoom of(String value) {
            return Arrays.stream(values())
                    .filter(zoom -> zoom.value.equals(value)).findFirst()
                    .orElse(null);
        }
    }

    /** Why a document could not be loaded, see {@link DocumentErrorEvent}. */
    public enum ErrorReason {
        /** The file could not be fetched. */
        NETWORK,
        /** The file is not a PDF document, or is damaged. */
        INVALID,
        /** The document needs a password to open. */
        PASSWORD;

        static ErrorReason of(String reason) {
            return switch (reason) {
            case "network" -> NETWORK;
            case "password" -> PASSWORD;
            default -> INVALID;
            };
        }
    }

    private int pageCount;

    /**
     * Creates an empty viewer.
     */
    public PdfViewer() {
        // The page and the zoom change in the browser as the user scrolls and
        // zooms; synchronize them so getPage() and getZoom() are current.
        getElement().addPropertyChangeListener("page", "page-changed",
                event -> {
                });
        getElement().addPropertyChangeListener("zoom", "zoom-changed",
                event -> {
                });
        getElement().addPropertyChangeListener("sidebarOpened",
                "sidebar-opened-changed", event -> {
                });
        getElement().addPropertyChangeListener("toolbarCollapsed",
                "toolbar-collapsed-changed", event -> {
                });
        // pageCount is a read-only property of the web component. Keep a copy
        // from the load events instead of synchronizing it.
        addDocumentLoadListener(event -> pageCount = event.getPageCount());
        addDocumentErrorListener(event -> pageCount = 0);
    }

    /**
     * Shows a document served by the application. The handler is switched to
     * inline, so the response is not a download.
     *
     * @param handler
     *            the handler that writes the PDF document
     */
    public void setSrc(DownloadHandler handler) {
        if (handler instanceof AbstractDownloadHandler<?> download) {
            download.inline();
        }
        pageCount = 0;
        getElement().setAttribute("src", handler);
    }

    /**
     * Shows the document at the given URL.
     *
     * @param url
     *            the URL of the PDF document
     */
    public void setSrc(String url) {
        pageCount = 0;
        getElement().setAttribute("src", url);
    }

    /**
     * Gets the current page, starting from 1. The viewer updates it while the
     * user scrolls, to the page that takes up most of the visible area.
     *
     * @return the current page
     */
    public int getPage() {
        return getElement().getProperty("page", 1);
    }

    /**
     * Scrolls to the start of the given page.
     *
     * @param page
     *            the page, starting from 1
     */
    public void setPage(int page) {
        getElement().setProperty("page", page);
    }

    /**
     * Gets the number of pages of the loaded document.
     *
     * @return the number of pages, or 0 when no document has loaded
     */
    public int getPageCount() {
        return pageCount;
    }

    /**
     * Sets how the viewer fits the pages.
     *
     * @param zoom
     *            the zoom mode
     */
    public void setZoom(Zoom zoom) {
        getElement().setProperty("zoom", zoom.value);
    }

    /**
     * Scales the pages relative to their actual size.
     *
     * @param scale
     *            the scale, {@code 1} for 100%
     */
    public void setZoom(double scale) {
        getElement().setProperty("zoom", scale);
    }

    /**
     * Gets the zoom mode, when the pages are fitted to the viewer.
     *
     * @return the zoom mode, or {@code null} when the user picked a percentage
     */
    public @Nullable Zoom getZoom() {
        return Zoom.of(getElement().getProperty("zoom", "page-width"));
    }

    /**
     * Shows or hides the sidebar with the page thumbnails and the outline.
     *
     * @param sidebarOpened
     *            whether the sidebar is shown
     */
    public void setSidebarOpened(boolean sidebarOpened) {
        getElement().setProperty("sidebarOpened", sidebarOpened);
    }

    /**
     * Gets whether the sidebar is shown.
     *
     * @return whether the sidebar is shown
     */
    public boolean isSidebarOpened() {
        return getElement().getProperty("sidebarOpened", false);
    }

    /**
     * Collapses or expands the toolbar. The toolbar only collapses while the
     * file name is shown, see {@link #setFileNameVisible(boolean)}.
     *
     * @param toolbarCollapsed
     *            whether the toolbar is collapsed
     */
    public void setToolbarCollapsed(boolean toolbarCollapsed) {
        getElement().setProperty("toolbarCollapsed", toolbarCollapsed);
    }

    /**
     * Sets the file name used when the user downloads the document.
     *
     * @param fileName
     *            the file name
     */
    public void setFileName(String fileName) {
        getElement().setProperty("fileName", fileName);
    }

    /**
     * Shows the name of the document above the toolbar.
     *
     * @param fileNameVisible
     *            whether the name is shown
     */
    public void setFileNameVisible(boolean fileNameVisible) {
        getElement().setProperty("fileNameVisible", fileNameVisible);
    }

    /**
     * Sets the texts of the viewer. Texts left {@code null} keep their default.
     *
     * @param i18n
     *            the texts
     */
    public void setI18n(PdfViewerI18n i18n) {
        getElement().setPropertyJson("i18n", JacksonUtils.beanToJson(i18n));
    }

    /**
     * Prints the document. The browser shows the progress and the print dialog;
     * the server is not told whether the user printed or cancelled (API-GAPS.md
     * #5).
     */
    public void print() {
        getElement().callJsFunction("print");
    }

    /**
     * Adds a listener for when the user scrolls to another page, or the page is
     * set from the server.
     *
     * @param listener
     *            the listener
     * @return a registration for removing the listener
     */
    public Registration addPageChangeListener(
            ComponentEventListener<PageChangeEvent> listener) {
        return getElement().addPropertyChangeListener("page",
                event -> listener.onComponentEvent(new PageChangeEvent(this,
                        event.isUserOriginated(), getPage())));
    }

    /**
     * Adds a listener for when a document has loaded.
     *
     * @param listener
     *            the listener
     * @return a registration for removing the listener
     */
    public Registration addDocumentLoadListener(
            ComponentEventListener<DocumentLoadEvent> listener) {
        return addListener(DocumentLoadEvent.class, listener);
    }

    /**
     * Adds a listener for when a document could not be loaded.
     *
     * @param listener
     *            the listener
     * @return a registration for removing the listener
     */
    public Registration addDocumentErrorListener(
            ComponentEventListener<DocumentErrorEvent> listener) {
        return addListener(DocumentErrorEvent.class, listener);
    }

    /** Fired when the current page changes. */
    public static class PageChangeEvent extends ComponentEvent<PdfViewer> {

        private final int page;

        /**
         * Creates an event.
         *
         * @param source
         *            the viewer
         * @param fromClient
         *            whether the user changed the page
         * @param page
         *            the new page
         */
        public PageChangeEvent(PdfViewer source, boolean fromClient, int page) {
            super(source, fromClient);
            this.page = page;
        }

        /**
         * Gets the new page.
         *
         * @return the page, starting from 1
         */
        public int getPage() {
            return page;
        }
    }

    /** Fired when a document has loaded. */
    @DomEvent("document-load")
    public static class DocumentLoadEvent extends ComponentEvent<PdfViewer> {

        private final int pageCount;
        private final String title;

        /**
         * Creates an event.
         *
         * @param source
         *            the viewer
         * @param fromClient
         *            whether the event came from the browser
         * @param pageCount
         *            the number of pages
         * @param title
         *            the title of the document, or an empty string
         */
        public DocumentLoadEvent(PdfViewer source, boolean fromClient,
                @EventData("event.detail.pageCount") int pageCount,
                @EventData("event.detail.title || ''") String title) {
            super(source, fromClient);
            this.pageCount = pageCount;
            this.title = title;
        }

        /**
         * Gets the number of pages.
         *
         * @return the number of pages
         */
        public int getPageCount() {
            return pageCount;
        }

        /**
         * Gets the title from the document's metadata.
         *
         * @return the title, or an empty string when it has none
         */
        public String getTitle() {
            return title;
        }
    }

    /** Fired when a document could not be loaded. */
    @DomEvent("document-error")
    public static class DocumentErrorEvent extends ComponentEvent<PdfViewer> {

        private final ErrorReason reason;

        /**
         * Creates an event.
         *
         * @param source
         *            the viewer
         * @param fromClient
         *            whether the event came from the browser
         * @param reason
         *            {@code network}, {@code invalid} or {@code password}
         */
        public DocumentErrorEvent(PdfViewer source, boolean fromClient,
                @EventData("event.detail.reason") String reason) {
            super(source, fromClient);
            this.reason = ErrorReason.of(reason);
        }

        /**
         * Gets why the document could not be loaded.
         *
         * @return the reason
         */
        public ErrorReason getReason() {
            return reason;
        }
    }
}
