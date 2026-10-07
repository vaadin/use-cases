package com.example.uc5;

import com.example.MediaTester;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Video;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = BackgroundVideoView.class)
class BackgroundVideoViewTest extends SpringBrowserlessTest {

    @Test
    void clipAutoplaysMutedInALoopWithoutControls() {
        navigate(BackgroundVideoView.class);

        Video video = findInView(Video.class).single();
        assertTrue(video.isAutoplay());
        assertTrue(video.isMuted());
        assertTrue(video.isLoop());
        assertFalse(video.isControls());
        assertTrue(video.getElement().hasAttribute("playsinline"));
        assertEquals("true", video.getElement().getAttribute("aria-hidden"));
        assertEquals(BackgroundVideoView.CLIP,
                video.getSources().get(0).getSrc());
    }

    @Test
    void pauseButtonTogglesPlayback() {
        navigate(BackgroundVideoView.class);
        runPendingSignalsTasks();
        MediaTester media = new MediaTester(findInView(Video.class).single());
        media.takeJavaScript();

        Button button = find(Button.class).withText("Pause background")
                .single();
        button.click();
        runPendingSignalsTasks();
        assertEquals("Play background", button.getText());
        assertTrue(media.takeJavaScript().stream()
                .anyMatch(js -> js.contains("pause")));

        button.click();
        runPendingSignalsTasks();
        assertEquals("Pause background", button.getText());
        assertTrue(media.takeJavaScript().stream()
                .anyMatch(js -> js.contains("this.play()")));
    }
}
