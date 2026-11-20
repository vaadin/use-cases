package com.example.uc8;

import com.example.MediaTester;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import com.vaadin.browserless.SpringBrowserlessApplicationContext;
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

    @Autowired
    private ApplicationContext applicationContext;

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
        try (var app = SpringBrowserlessApplicationContext.create(
                applicationContext,
                b -> b.withViewPackages(ResumePlaybackView.class)
                        .withSessionLockIsolation(true))) {
            var alice = app.newUser().newWindow();
            var bob = app.newUser().newWindow();
            alice.navigate(ResumePlaybackView.class);
            alice.runPendingSignalsTasks();

            // Alice watches half a minute and pauses.
            alice.activate();
            new MediaTester(alice.findInView(Video.class).single())
                    .fire("pause", "element.currentTime", 30.0);

            // Bob, in his own browser, opens the recording.
            bob.navigate(ResumePlaybackView.class);
            bob.runPendingSignalsTasks();
            assertTrue(
                    bob.findInView(Span.class).all().stream()
                            .anyMatch(s -> "Starting from the beginning"
                                    .equals(s.getText())),
                    "another browser must not resume from Alice's position");

            // Alice, reloading, still gets her own position back.
            alice.reload();
            alice.runPendingSignalsTasks();
            assertTrue(alice.findInView(Span.class).all().stream()
                    .anyMatch(s -> "Resuming from 0:30".equals(s.getText())));
        }
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
