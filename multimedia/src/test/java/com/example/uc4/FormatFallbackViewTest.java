package com.example.uc4;

import java.util.List;

import com.example.MediaTester;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.html.Source;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.Video;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = FormatFallbackView.class)
class FormatFallbackViewTest extends SpringBrowserlessTest {

    @Test
    void listsWebmBeforeMp4() {
        navigate(FormatFallbackView.class);

        Video video = findInView(Video.class).single();
        assertEquals(List.of(FormatFallbackView.WEBM, FormatFallbackView.MP4),
                video.getSources().stream().map(Source::getSrc).toList());
        assertTrue(video.getSources().get(0).getType().orElseThrow()
                .startsWith("video/webm"));
    }

    @Test
    void badgeReportsTheSourceTheBrowserChose() {
        navigate(FormatFallbackView.class);
        runPendingSignalsTasks();
        assertBadge("Browser chose: not loaded yet");

        new MediaTester(findInView(Video.class).single()).fire("loadedmetadata",
                "element.currentSrc",
                "http://localhost:8080/media/trailer.mp4");
        runPendingSignalsTasks();

        assertBadge("Browser chose: MP4 (H.264)");
    }

    private void assertBadge(String text) {
        assertTrue(
                findInView(Span.class).all().stream()
                        .anyMatch(s -> text.equals(s.getText())),
                "expected badge \"" + text + "\"");
    }
}
