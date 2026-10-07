package com.example.uc3;

import com.example.WakeLockTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = SlideshowView.class)
class SlideshowViewTest extends SpringBrowserlessTest {

    @Test
    void viewRendersAndStartsIdle() {
        navigate(SlideshowView.class);
        runPendingSignalsTasks();

        assertTrue(
                findInView(H1.class).all().stream()
                        .anyMatch(h -> "UC3 — Presentation slideshow"
                                .equals(h.getText())),
                "view should render heading");

        Button start = findInView(Button.class).withText("Start presentation")
                .single();
        Button next = findInView(Button.class).withText("Next slide").single();
        Button stop = findInView(Button.class).withText("Stop").single();
        assertTrue(start.isEnabled(),
                "Start should be enabled before presenting");
        assertFalse(next.isEnabled(),
                "Next should be disabled before presenting");
        assertFalse(stop.isEnabled(),
                "Stop should be disabled before presenting");
    }

    @Test
    void startEnablesNextAndStop_advancesSlide() {
        navigate(SlideshowView.class);
        runPendingSignalsTasks();

        test(findInView(Button.class).withText("Start presentation").single())
                .click();
        runPendingSignalsTasks();

        assertSlideBodyContains("quick tour");

        test(findInView(Button.class).withText("Next slide").single()).click();
        runPendingSignalsTasks();
        assertSlideBodyContains("Wake Lock API prevents");
    }

    @Test
    void reachingLastSlideStopsPresentation() {
        navigate(SlideshowView.class);
        runPendingSignalsTasks();

        Button start = findInView(Button.class).withText("Start presentation")
                .single();
        test(start).click();
        runPendingSignalsTasks();

        Button next = findInView(Button.class).withText("Next slide").single();
        // 5 slides → click Next 5 times to fall off the end.
        for (int i = 0; i < 5; i++) {
            test(next).click();
            runPendingSignalsTasks();
        }
        // After advancing past the last slide, the presentation should stop.
        Button stop = findInView(Button.class).withText("Stop").single();
        assertFalse(stop.isEnabled(),
                "Stop should be disabled once the deck ends");
        assertTrue(start.isEnabled(),
                "Start should be re-enabled after the deck ends");
    }

    @Test
    void badgeReflectsSimulatedLockState() {
        navigate(SlideshowView.class);
        runPendingSignalsTasks();

        assertBadgeContains("Released");

        test(findInView(Button.class).withText("Start presentation").single())
                .click();
        runPendingSignalsTasks();
        // Server has called request(); the browser hasn't confirmed yet.
        WakeLockTestSupport.simulateAcquired();
        runPendingSignalsTasks();
        assertBadgeContains("Holding");

        test(findInView(Button.class).withText("Stop").single()).click();
        runPendingSignalsTasks();
        // Server has called release(); the browser confirms.
        WakeLockTestSupport.simulateReleased();
        runPendingSignalsTasks();
        assertBadgeContains("Released");
    }

    private void assertSlideBodyContains(String fragment) {
        assertTrue(
                findInView(Div.class).all().stream()
                        .anyMatch(d -> d.getText() != null
                                && d.getText().contains(fragment)),
                "expected a slide body div containing \"" + fragment + "\"");
    }

    private void assertBadgeContains(String fragment) {
        assertTrue(
                findInView(Span.class).all().stream()
                        .anyMatch(s -> s.getText() != null
                                && s.getText().contains(fragment)),
                "expected status badge to contain \"" + fragment + "\"");
    }
}
