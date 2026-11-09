package com.example;

import com.vaadin.flow.component.pdfviewer.PdfViewer;

/**
 * Workarounds for properties of the {@code <vaadin-pdf-viewer>} web component
 * that the Flow {@link PdfViewer} does not expose (API-GAPS.md #12). Each one
 * sets the web component's property on the element. Migrate by replacing the
 * calls with the Flow component's setters once it has them, and delete this
 * class.
 */
public final class MissingAPI {

    private MissingAPI() {
    }

    /**
     * Scrolls the viewer to the start of a page. The web component keeps a page
     * past the end as it is, without scrolling (API-GAPS.md #6).
     * <p>
     * {@link PdfViewer#getPage()}, {@link PdfViewer#pageSignal()} and the page
     * change listeners see the new page, as the Flow component reads the same
     * property. {@link PdfViewer#setSrc} resets the page to 1, so set the page
     * after the source.
     *
     * @param viewer
     *            the viewer
     * @param page
     *            the page, starting from 1
     */
    public static void setPage(PdfViewer viewer, int page) {
        viewer.getElement().setProperty("page", page);
    }

    /**
     * Sets the title of the viewer's header, in place of the file name of the
     * document.
     *
     * @param viewer
     *            the viewer
     * @param title
     *            the title
     */
    public static void setHeaderTitle(PdfViewer viewer, String title) {
        viewer.getElement().setProperty("headerTitle", title);
    }

    /**
     * Collapses or expands the toolbar under the header. The user can still
     * expand it by clicking the header.
     *
     * @param viewer
     *            the viewer
     * @param collapsed
     *            whether the toolbar is collapsed
     */
    public static void setToolbarCollapsed(PdfViewer viewer,
            boolean collapsed) {
        viewer.getElement().setProperty("toolbarCollapsed", collapsed);
    }
}
