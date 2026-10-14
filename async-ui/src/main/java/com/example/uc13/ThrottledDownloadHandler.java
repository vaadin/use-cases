package com.example.uc13;

import java.io.IOException;
import java.io.OutputStream;
import java.time.Duration;

import com.example.backend.SimulatedLatency;

import com.vaadin.flow.server.streams.DownloadEvent;
import com.vaadin.flow.server.streams.DownloadHandler;

/**
 * Serves bytes the way a slow network delivers them: one chunk at a time, with
 * a pause after each. The browser receives, and paints, the file bit by bit.
 * <p>
 * The pause goes through {@link SimulatedLatency#block}, so tests run it
 * without waiting. The session is not locked while a download handler runs, so
 * the rest of the UI stays responsive.
 */
class ThrottledDownloadHandler implements DownloadHandler {

    static final int CHUNK_SIZE = 8 * 1024;

    private final byte[] content;
    private final String contentType;
    private final Duration chunkDelay;
    private final SimulatedLatency latency;

    ThrottledDownloadHandler(byte[] content, String contentType,
            Duration chunkDelay, SimulatedLatency latency) {
        this.content = content;
        this.contentType = contentType;
        this.chunkDelay = chunkDelay;
        this.latency = latency;
    }

    @Override
    public void handleDownloadRequest(DownloadEvent event) throws IOException {
        event.setContentType(contentType);
        event.setContentLength(content.length);
        OutputStream out = event.getOutputStream();
        for (int offset = 0; offset < content.length; offset += CHUNK_SIZE) {
            out.write(content, offset,
                    Math.min(CHUNK_SIZE, content.length - offset));
            out.flush();
            latency.block(chunkDelay);
        }
    }

    /** How long serving the whole content takes. */
    Duration duration() {
        int chunks = (content.length + CHUNK_SIZE - 1) / CHUNK_SIZE;
        return chunkDelay.multipliedBy(chunks);
    }
}
