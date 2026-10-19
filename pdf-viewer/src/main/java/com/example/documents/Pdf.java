package com.example.documents;

import java.io.ByteArrayInputStream;

import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;
import com.vaadin.flow.server.streams.InputStreamDownloadHandler;

/**
 * A document the application serves, as a file name and its bytes.
 *
 * @param fileName
 *            the name the document is served and downloaded under
 * @param bytes
 *            the content, a PDF document or, for the damaged sample, not
 */
public record Pdf(String fileName, byte[] bytes) {

    /** The content type documents are served with. */
    public static final String CONTENT_TYPE = "application/pdf";

    /**
     * A handler that serves the document. Each call creates a new handler, so
     * the viewer gets a new URL and loads the document again.
     *
     * @return a handler for {@code PdfViewer#setSrc(DownloadHandler)}
     */
    public InputStreamDownloadHandler handler() {
        return DownloadHandler.fromInputStream(
                event -> new DownloadResponse(new ByteArrayInputStream(bytes),
                        fileName, CONTENT_TYPE, bytes.length),
                fileName);
    }

    /**
     * A handler for a document that is no longer there: it answers 404 Not
     * Found, like a link to a deleted file.
     *
     * @param fileName
     *            the name of the missing document
     * @return a handler that always fails
     */
    public static DownloadHandler missing(String fileName) {
        return DownloadHandler.fromInputStream(
                event -> DownloadResponse.error(404), fileName);
    }
}
