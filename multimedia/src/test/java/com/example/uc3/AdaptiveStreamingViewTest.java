package com.example.uc3;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.Video;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = AdaptiveStreamingView.class)
class AdaptiveStreamingViewTest extends SpringBrowserlessTest {

    @Test
    void servesTheMasterPlaylistAsAnHlsSource() {
        navigate(AdaptiveStreamingView.class);
        runPendingSignalsTasks();

        assertEquals("UC3 — Adaptive streaming",
                findInView(H1.class).single().getText());
        Video video = findInView(Video.class).single();
        var source = video.getSources().get(0);
        assertEquals(AdaptiveStreamingView.HLS_TYPE,
                source.getType());
        assertTrue(source.getSrc().endsWith("/index.m3u8"),
                "handler URL should end with the playlist name, was "
                        + source.getSrc());
        // The playback path is only known once the browser has answered.
        assertTrue(findInView(Span.class).all().stream()
                .anyMatch(s -> s.getText().equals("Playback: checking…")));
    }
}
