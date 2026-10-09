package com.example.uc13;

import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.util.Collections;

import com.example.ManualLatency;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.server.streams.DownloadEvent;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ThrottledDownloadHandlerTest {

    @Test
    void servesEverythingWithAPauseAfterEachChunk() throws Exception {
        byte[] content = new byte[2 * ThrottledDownloadHandler.CHUNK_SIZE
                + 100];
        for (int i = 0; i < content.length; i++) {
            content[i] = (byte) i;
        }
        Duration delay = Duration.ofMillis(150);
        ManualLatency latency = new ManualLatency();
        ThrottledDownloadHandler handler = new ThrottledDownloadHandler(content,
                "image/jpeg", delay, latency);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DownloadEvent event = mock(DownloadEvent.class);
        when(event.getOutputStream()).thenReturn(out);

        handler.handleDownloadRequest(event);

        assertArrayEquals(content, out.toByteArray());
        verify(event).setContentType("image/jpeg");
        verify(event).setContentLength(content.length);
        assertEquals(Collections.nCopies(3, delay), latency.blocked());
        assertEquals(delay.multipliedBy(3), handler.duration());
    }
}
