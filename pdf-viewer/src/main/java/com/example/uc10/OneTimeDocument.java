package com.example.uc10;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import com.example.documents.Pdf;
import org.jspecify.annotations.Nullable;

import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;

/**
 * Serves a confidential document to the user of a session once. The first
 * request gets the file; every later one, from a reload, a second tab or the
 * file's address pasted elsewhere, gets 410 Gone.
 * <p>
 * Session-scoped so that each reader of the demo gets their own one view. A
 * real application would store that the user opened the document, next to the
 * document, so that it also holds after the session ends.
 */
@SpringComponent
@VaadinSessionScope
public class OneTimeDocument {

    private final AtomicReference<@Nullable Instant> openedAt = new AtomicReference<>();

    /**
     * When the file was sent.
     *
     * @return the time, or {@code null} when it has not been sent yet
     */
    public @Nullable Instant openedAt() {
        return openedAt.get();
    }

    /**
     * A handler that sends the document if it has not been sent before.
     *
     * @param pdf
     *            the document
     * @return a handler for {@code PdfViewer#setSrc(DownloadHandler)}
     */
    public DownloadHandler handler(Pdf pdf) {
        return DownloadHandler.fromInputStream(event -> {
            if (!take()) {
                return DownloadResponse.error(410);
            }
            return new DownloadResponse(new ByteArrayInputStream(pdf.bytes()),
                    pdf.fileName(), Pdf.CONTENT_TYPE, pdf.bytes().length);
        }, pdf.fileName());
    }

    /**
     * Records that the document is sent now, unless it was sent before.
     *
     * @return whether the document may be sent
     */
    boolean take() {
        return openedAt.compareAndSet(null, Instant.now());
    }

    /** Lets the document be opened again. Only the demo controls do this. */
    void reset() {
        openedAt.set(null);
    }
}
