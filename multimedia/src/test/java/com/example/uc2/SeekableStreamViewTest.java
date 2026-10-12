package com.example.uc2;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.html.Code;
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

    @Test
    void explainsWhichHandlersAnswerRanges() {
        navigate(SeekableStreamView.class);

        List<String> apis = findInView(Code.class).all().stream()
                .map(Code::getText).toList();
        assertTrue(apis.contains("DownloadHandler.forFile(file)"));
        assertTrue(apis.stream().anyMatch(
                api -> api.startsWith("DownloadHandler.fromInputStream")
                        && api.endsWith(".enableRangeRequests()")));
    }
}
