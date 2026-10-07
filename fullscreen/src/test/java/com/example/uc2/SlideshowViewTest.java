package com.example.uc2;

import com.example.FullscreenTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.fullscreen.FullscreenState;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = SlideshowView.class)
class SlideshowViewTest extends SpringBrowserlessTest {

    @Test
    void viewRendersWithControls() {
        navigate(SlideshowView.class);

        assertTrue(findInView(H1.class).all().stream().anyMatch(h -> h.getText()
                .equals("UC2 — Slideshow / presentation mode")));
        assertTrue(findInView(Button.class).all().stream()
                .anyMatch(b -> "Previous".equals(b.getText())));
        assertTrue(findInView(Button.class).all().stream()
                .anyMatch(b -> "Next".equals(b.getText())));
        assertTrue(findInView(Button.class).all().stream()
                .anyMatch(b -> "Present".equals(b.getText())));
    }

    @Test
    void nextAdvancesAndWrapsSlideCounter() {
        navigate(SlideshowView.class);
        runPendingSignalsTasks();

        assertSlideCounterShows("Slide 1 of 5");

        Button next = findInView(Button.class).all().stream()
                .filter(b -> "Next".equals(b.getText())).findFirst()
                .orElseThrow();
        test(next).click();
        runPendingSignalsTasks();
        assertSlideCounterShows("Slide 2 of 5");

        for (int i = 0; i < 4; i++) {
            test(next).click();
            runPendingSignalsTasks();
        }
        // Wrapped back to the start (floorMod).
        assertSlideCounterShows("Slide 1 of 5");
    }

    @Test
    void stateBadgeReflectsFullscreenSignal() {
        navigate(SlideshowView.class);
        runPendingSignalsTasks();

        FullscreenTestSupport.setFullscreenState(FullscreenState.FULLSCREEN);
        runPendingSignalsTasks();
        // Badge is hidden while presenting, so the text stays on the idle
        // copy rather than flipping to a message no one sees.
        assertBadgeContains("Present to start");

        FullscreenTestSupport
                .setFullscreenState(FullscreenState.NOT_FULLSCREEN);
        runPendingSignalsTasks();
        assertBadgeContains("Present to start");
    }

    private void assertSlideCounterShows(String text) {
        assertTrue(
                findInView(Span.class).all().stream()
                        .anyMatch(s -> text.equals(s.getText())),
                "expected slide counter to read \"" + text + "\"");
    }

    private void assertBadgeContains(String fragment) {
        assertTrue(findInView(Span.class).all().stream()
                .anyMatch(s -> s.getClassNames().contains("status-badge")
                        && s.getText() != null
                        && s.getText().contains(fragment)),
                "expected status badge to contain \"" + fragment + "\"");
    }
}
