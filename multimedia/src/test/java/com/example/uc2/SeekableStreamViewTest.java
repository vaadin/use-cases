package com.example.uc2;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Video;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = SeekableStreamView.class)
class SeekableStreamViewTest extends SpringBrowserlessTest {

    @Test
    void recordingIsServedThroughAHandler() {
        navigate(SeekableStreamView.class);

        assertEquals("UC2 — Seekable streaming",
                findInView(H1.class).single().getText());
        Video video = findInView(Video.class).single();
        assertTrue(video.getSources().get(0).getSrc()
                .startsWith("VAADIN/dynamic/resource/"));
        assertEquals("video/mp4", video.getSources().get(0).getType());
    }
}
