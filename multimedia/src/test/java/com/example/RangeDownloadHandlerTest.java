package com.example;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.streams.DownloadEvent;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RangeDownloadHandlerTest {

    private static final byte[] CONTENT = "0123456789"
            .getBytes(StandardCharsets.US_ASCII);

    @ParameterizedTest
    @CsvSource({ "bytes=0-3, 0, 3", "bytes=4-, 4, 9", "bytes=-3, 7, 9",
            "bytes=8-100, 8, 9", "bytes=-100, 0, 9" })
    void singleRangeIsResolved(String header, long start, long end) {
        assertArrayEquals(new long[] { start, end },
                RangeDownloadHandler.parseRange(header, CONTENT.length));
    }

    @ParameterizedTest
    @CsvSource({ "bytes=10-", "bytes=5-2", "bytes=-0" })
    void unsatisfiableRangeIsRejected(String header) {
        assertEquals(0,
                RangeDownloadHandler.parseRange(header, CONTENT.length).length);
    }

    @Test
    void missingOrMultiRangeServesEverything() {
        assertNull(RangeDownloadHandler.parseRange(null, CONTENT.length));
        assertNull(RangeDownloadHandler.parseRange("bytes=0-1,4-5",
                CONTENT.length));
        assertNull(
                RangeDownloadHandler.parseRange("items=0-1", CONTENT.length));
    }

    @Test
    void rangeRequestGetsPartialContent() throws Exception {
        Exchange exchange = new Exchange("bytes=2-5");

        new RangeDownloadHandler(() -> CONTENT, "video/mp4")
                .handleDownloadRequest(exchange.event);

        verify(exchange.response).setStatus(206);
        verify(exchange.response).setHeader("Content-Range", "bytes 2-5/10");
        verify(exchange.response).setHeader("Accept-Ranges", "bytes");
        assertEquals("2345", exchange.body.toString(StandardCharsets.US_ASCII));
    }

    @Test
    void plainRequestGetsWholeContentAndAdvertisesRanges() throws Exception {
        Exchange exchange = new Exchange(null);

        new RangeDownloadHandler(() -> CONTENT, "video/mp4")
                .handleDownloadRequest(exchange.event);

        verify(exchange.response, never()).setStatus(206);
        verify(exchange.response).setHeader("Accept-Ranges", "bytes");
        assertEquals("0123456789",
                exchange.body.toString(StandardCharsets.US_ASCII));
    }

    @Test
    void unsatisfiableRangeGets416() throws Exception {
        Exchange exchange = new Exchange("bytes=20-");

        new RangeDownloadHandler(() -> CONTENT, "video/mp4")
                .handleDownloadRequest(exchange.event);

        verify(exchange.response).setStatus(416);
        verify(exchange.response).setHeader("Content-Range", "bytes */10");
        assertEquals(0, exchange.body.size());
    }

    private static final class Exchange {
        final VaadinResponse response = mock(VaadinResponse.class);
        final DownloadEvent event = mock(DownloadEvent.class);
        final ByteArrayOutputStream body = new ByteArrayOutputStream();

        Exchange(String range) {
            VaadinRequest request = mock(VaadinRequest.class);
            when(request.getHeader("Range")).thenReturn(range);
            when(event.getRequest()).thenReturn(request);
            when(event.getResponse()).thenReturn(response);
            when(event.getOutputStream()).thenReturn(body);
        }
    }
}
