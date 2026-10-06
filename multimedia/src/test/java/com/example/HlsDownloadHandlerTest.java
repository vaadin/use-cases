package com.example;

import java.io.ByteArrayOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.streams.DownloadEvent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HlsDownloadHandlerTest {

    @Test
    void masterPlaylistPointsVariantsBackAtTheHandler() {
        String master = """
                #EXTM3U
                #EXT-X-STREAM-INF:BANDWIDTH=400000,RESOLUTION=426x240
                240p/index.m3u8
                """;

        assertEquals("""
                #EXTM3U
                #EXT-X-STREAM-INF:BANDWIDTH=400000,RESOLUTION=426x240
                ?file=240p%2Findex.m3u8
                """, HlsDownloadHandler.rewritePlaylist("index.m3u8", master));
    }

    @Test
    void variantPlaylistResolvesSegmentsAndInitAgainstItsOwnDirectory() {
        String variant = """
                #EXTM3U
                #EXT-X-MAP:URI="init_0.mp4"
                #EXTINF:6.000000,
                segment00.m4s
                #EXT-X-ENDLIST
                """;

        assertEquals("""
                #EXTM3U
                #EXT-X-MAP:URI="?file=240p%2Finit_0.mp4"
                #EXTINF:6.000000,
                ?file=240p%2Fsegment00.m4s
                #EXT-X-ENDLIST
                """,
                HlsDownloadHandler.rewritePlaylist("240p/index.m3u8", variant));
    }

    @ParameterizedTest
    @ValueSource(strings = { "../../application.properties",
            "240p/../../application.properties", "/etc/passwd",
            "240p/missing.m4s", "240p/segment00.ts" })
    void filesOutsideTheStreamAreNotServed(String file) throws Exception {
        VaadinRequest request = mock(VaadinRequest.class);
        when(request.getParameter("file")).thenReturn(file);
        VaadinResponse response = mock(VaadinResponse.class);
        DownloadEvent event = mock(DownloadEvent.class);
        when(event.getRequest()).thenReturn(request);
        when(event.getResponse()).thenReturn(response);
        when(event.getOutputStream()).thenReturn(new ByteArrayOutputStream());

        new HlsDownloadHandler("hls").handleDownloadRequest(event);

        verify(response).setStatus(404);
    }
}
