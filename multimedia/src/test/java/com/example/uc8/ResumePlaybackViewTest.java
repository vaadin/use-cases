package com.example.uc8;

import com.example.MediaTester;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.Video;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = ResumePlaybackView.class)
class ResumePlaybackViewTest extends SpringBrowserlessTest {

    @Test
    void firstVisitStartsFromTheBeginning() {
        navigate(ResumePlaybackView.class);
        runPendingSignalsTasks();

        assertStatus("Starting from the beginning");
        assertFalse(
                find(Button.class).withText("Start over").single().isEnabled());
    }

    @Test
    void pausingSavesThePosition() {
        navigate(ResumePlaybackView.class);
        runPendingSignalsTasks();
        MediaTester media = new MediaTester(findInView(Video.class).single());

        media.fire("timeupdate", "element.currentTime", 12.0);
        assertEquals(12.0, savedPosition());
        media.fire("pause", "element.currentTime", 23.4);
        assertEquals(23.4, savedPosition());
    }

    @Test
    void startOverForgetsThePosition() {
        PlaybackPositions.current().save(ResumePlaybackView.RECORDING, 40);
        navigate(ResumePlaybackView.class);
        runPendingSignalsTasks();
        assertStatus("Resuming from 0:40");

        test(find(Button.class).withText("Start over").single()).click();
        runPendingSignalsTasks();

        assertStatus("Starting from the beginning");
        assertTrue(PlaybackPositions.current().get(ResumePlaybackView.RECORDING)
                .isEmpty());
    }

    @Test
    void finishingTheRecordingForgetsThePosition() {
        navigate(ResumePlaybackView.class);
        runPendingSignalsTasks();
        MediaTester media = new MediaTester(findInView(Video.class).single());

        media.fire("pause", "element.currentTime", 60.0);
        media.fire("ended");
        // A late, throttled timeupdate must not bring the position back.
        media.fire("timeupdate", "element.currentTime", 60.0);

        assertTrue(PlaybackPositions.current().get(ResumePlaybackView.RECORDING)
                .isEmpty());
    }

    @Test
    void positionsAreNotSharedBetweenSessions() {
        navigate(ResumePlaybackView.class);
        PlaybackPositions.current().save(ResumePlaybackView.RECORDING, 30);

        cleanVaadinEnvironment();
        initVaadinEnvironment();
        navigate(ResumePlaybackView.class);
        runPendingSignalsTasks();

        assertStatus("Starting from the beginning");
        assertTrue(PlaybackPositions.current().get(ResumePlaybackView.RECORDING)
                .isEmpty());
    }

    private double savedPosition() {
        return PlaybackPositions.current().get(ResumePlaybackView.RECORDING)
                .orElseThrow();
    }

    private void assertStatus(String text) {
        assertTrue(
                findInView(Span.class).all().stream()
                        .anyMatch(s -> text.equals(s.getText())),
                "expected status \"" + text + "\"");
    }
}
