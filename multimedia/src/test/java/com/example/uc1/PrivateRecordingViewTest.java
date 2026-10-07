package com.example.uc1;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Media;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Video;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = PrivateRecordingView.class)
class PrivateRecordingViewTest extends SpringBrowserlessTest {

    @Test
    void firstRecordingIsServedWithPosterThroughHandlers() {
        navigate(PrivateRecordingView.class);
        runPendingSignalsTasks();

        assertEquals("UC1 — Private recording",
                findInView(H1.class).single().getText());
        Video video = findInView(Video.class).single();
        assertTrue(video.isControls());
        assertEquals(Media.Preload.METADATA, video.getPreload());
        assertEquals(1, video.getSources().size());
        // Handler URLs live under the dynamic resource path, not a static one.
        assertTrue(video.getSources().get(0).getSrc()
                .startsWith("VAADIN/dynamic/resource/"));
        assertTrue(video.getPoster()
                .startsWith("VAADIN/dynamic/resource/"));
    }

    @Test
    void pickingAnotherRecordingReplacesThePlayer() {
        navigate(PrivateRecordingView.class);
        runPendingSignalsTasks();
        Video first = findInView(Video.class).single();

        test(find(Button.class).withText("Sprint 42 review").single()).click();
        runPendingSignalsTasks();

        Video second = findInView(Video.class).single();
        assertNotSame(first, second);
        assertTrue(findInView(Paragraph.class).all().stream()
                .anyMatch(p -> p.getText().contains("Dark mode")));
        assertTrue(find(Button.class).withText("Sprint 42 review").single()
                .hasThemeName("primary"));
    }
}
