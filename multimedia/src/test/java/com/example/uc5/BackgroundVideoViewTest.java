package com.example.uc5;

import com.example.MediaTester;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Video;
import com.vaadin.flow.component.select.Select;

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
        assertFalse(video.isControlsVisible());
        assertTrue(video.getElement().hasAttribute("playsinline"));
        assertEquals("true", video.getElement().getAttribute("aria-hidden"));
        assertEquals(BackgroundVideoView.BACKGROUNDS.get(0).file(),
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

    @Test
    void selectSwitchesAndReloadsTheClip() {
        navigate(BackgroundVideoView.class);
        Video video = findInView(Video.class).single();
        MediaTester media = new MediaTester(video);
        media.takeJavaScript();

        var sunset = BackgroundVideoView.BACKGROUNDS.get(1);
        select().setValue(sunset);

        assertEquals(1, video.getSources().size());
        assertEquals(sunset.file(), video.getSources().get(0).getSrc());
        assertTrue(media.takeJavaScript().stream()
                .anyMatch(js -> js.contains("load")));
        assertTrue(video.isAutoplay());
    }

    @Test
    void pausedClipStaysPausedWhenTheBackgroundChanges() {
        navigate(BackgroundVideoView.class);
        runPendingSignalsTasks();
        Video video = findInView(Video.class).single();

        find(Button.class).withText("Pause background").single().click();
        select().setValue(BackgroundVideoView.BACKGROUNDS.get(2));
        assertFalse(video.isAutoplay());

        find(Button.class).withText("Play background").single().click();
        assertTrue(video.isAutoplay());
    }

    @SuppressWarnings("unchecked")
    private Select<BackgroundVideoView.Background> select() {
        return findInView(Select.class).single();
    }
}
