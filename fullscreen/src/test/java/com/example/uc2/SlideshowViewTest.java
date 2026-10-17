package com.example.uc2;

import com.example.FullscreenTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.fullscreen.FullscreenState;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = SlideshowView.class)
class SlideshowViewTest extends SpringBrowserlessTest {

    @Test
    void arrowKeysMoveThroughTheSlidesWhilePresenting() {
        navigate(SlideshowView.class);
        FullscreenTestSupport.setFullscreenState(FullscreenState.FULLSCREEN);
        runPendingSignalsTasks();
        assertSlideCounterShows("Slide 1 of 5");

        fireShortcut(Key.ARROW_RIGHT);
        assertSlideCounterShows("Slide 2 of 5");

        fireShortcut(Key.ARROW_LEFT);
        fireShortcut(Key.ARROW_LEFT);
        // Wraps around to the last slide.
        assertSlideCounterShows("Slide 5 of 5");
    }

    @Test
    void stageFillsTheScreenOnlyWhilePresenting() {
        navigate(SlideshowView.class);
        Div stage = findInView(Div.class).all().stream()
                .filter(d -> d.hasClassName("uc2-stage")).findFirst()
                .orElseThrow();

        FullscreenTestSupport.setFullscreenState(FullscreenState.FULLSCREEN);
        runPendingSignalsTasks();
        assertTrue(stage.hasClassName("uc2-stage-live"));

        FullscreenTestSupport
                .setFullscreenState(FullscreenState.NOT_FULLSCREEN);
        runPendingSignalsTasks();
        assertFalse(stage.hasClassName("uc2-stage-live"));
    }

    private void assertSlideCounterShows(String text) {
        assertTrue(
                findInView(Span.class).all().stream()
                        .anyMatch(s -> text.equals(s.getText())),
                "expected slide counter to read \"" + text + "\"");
    }
}
