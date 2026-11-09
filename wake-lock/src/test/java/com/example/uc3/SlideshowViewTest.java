package com.example.uc3;

import java.util.List;

import com.example.home.HomeView;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;

import static com.example.WakeLockTestSupport.RELEASE;
import static com.example.WakeLockTestSupport.REQUEST;
import static com.example.WakeLockTestSupport.answerWakeLockCalls;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = { SlideshowView.class, HomeView.class })
class SlideshowViewTest extends SpringBrowserlessTest {

    @Test
    void lockIsHeldFromStartUntilStop() {
        navigate(SlideshowView.class);
        assertFalse(answerWakeLockCalls().contains(REQUEST),
                "the lock is not requested before the presentation starts");
        runPendingSignalsTasks();
        assertBadge("Released");

        test(button("Start presentation")).click();
        assertEquals(List.of(REQUEST), answerWakeLockCalls());
        runPendingSignalsTasks();
        assertBadge("Holding — the screen stays on until you stop");

        test(button("Stop")).click();
        assertEquals(List.of(RELEASE), answerWakeLockCalls());
        runPendingSignalsTasks();
        assertBadge("Released");
    }

    @Test
    void nextGoesThroughTheDeckAndPastTheLastSlideStops() {
        navigate(SlideshowView.class);
        test(button("Start presentation")).click();
        answerWakeLockCalls();
        runPendingSignalsTasks();
        assertSlideTitle("Welcome");

        test(button("Next slide")).click();
        runPendingSignalsTasks();
        assertSlideTitle("Why hold the lock");

        // Three more clicks reach the last of the five slides, one more
        // goes past it.
        for (int i = 0; i < 4; i++) {
            test(button("Next slide")).click();
        }
        runPendingSignalsTasks();

        assertEquals(List.of(RELEASE), answerWakeLockCalls());
        assertSlideTitle("End");
        assertTrue(button("Start presentation").isEnabled());
        assertFalse(button("Stop").isEnabled());
    }

    @Test
    void leavingMidPresentationReleasesTheLock() {
        navigate(SlideshowView.class);
        answerWakeLockCalls();
        test(button("Start presentation")).click();
        assertEquals(List.of(REQUEST), answerWakeLockCalls());

        navigate(HomeView.class);

        assertEquals(List.of(RELEASE), answerWakeLockCalls());
    }

    private Button button(String text) {
        return findInView(Button.class).withText(text).single();
    }

    private void assertSlideTitle(String text) {
        assertEquals(text, findInView(Div.class)
                .withClassName("uc3-slide-title").single().getText());
    }

    private void assertBadge(String text) {
        assertEquals(text, findInView(Span.class).withClassName("status-badge")
                .single().getText());
    }
}
