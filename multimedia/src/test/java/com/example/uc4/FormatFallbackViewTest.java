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
    void listsAv1BeforeH264() {
        navigate(FormatFallbackView.class);

        Video video = findInView(Video.class).single();
        assertEquals(List.of(FormatFallbackView.AV1, FormatFallbackView.H264),
                video.getSources().stream().map(Source::getSrc).toList());
        assertTrue(video.getSources().get(0).getType()
                .contains("av01"));
    }

    @Test
    void badgeReportsTheSourceTheBrowserChose() {
        navigate(FormatFallbackView.class);
        runPendingSignalsTasks();
        assertBadge("Browser chose: not loaded yet");

        MediaTester media = new MediaTester(findInView(Video.class).single());
        media.fire("loadedmetadata", "element.currentSrc",
                "http://localhost:8080/media/trailer-av1.mp4");
        runPendingSignalsTasks();
        assertBadge("Browser chose: AV1");

        media.fire("loadedmetadata", "element.currentSrc",
                "http://localhost:8080/media/trailer.mp4");
        runPendingSignalsTasks();
        assertBadge("Browser chose: H.264");
    }

    private void assertBadge(String text) {
        assertTrue(
                findInView(Span.class).all().stream()
                        .anyMatch(s -> text.equals(s.getText())),
                "expected badge \"" + text + "\"");
    }
}
