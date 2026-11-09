package com.example.uc1;

import java.util.List;

import com.example.home.HomeView;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.wakelock.WakeLockAvailability;
import com.vaadin.flow.dom.Element;

import static com.example.WakeLockTestSupport.RELEASE;
import static com.example.WakeLockTestSupport.REQUEST;
import static com.example.WakeLockTestSupport.answerWakeLockCalls;
import static com.example.WakeLockTestSupport.simulateAvailability;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = { ManualToggleView.class, HomeView.class })
class ManualToggleViewTest extends SpringBrowserlessTest {

    @Test
    void clickingRequestsTheLockAndClickingAgainReleasesIt() {
        navigate(ManualToggleView.class);
        assertEquals(List.of(), answerWakeLockCalls(),
                "opening the view does not touch the lock");
        assertBadge("Released — the screen may sleep");

        test(button("Keep screen awake")).click();
        runPendingSignalsTasks();
        assertBadge("Requested — waiting for the browser");

        assertEquals(List.of(REQUEST), answerWakeLockCalls());
        runPendingSignalsTasks();
        assertBadge("Holding the lock — the screen stays on");

        test(button("Allow screen to sleep")).click();
        assertEquals(List.of(RELEASE), answerWakeLockCalls());
        runPendingSignalsTasks();
        assertBadge("Released — the screen may sleep");
        button("Keep screen awake");
    }

    @Test
    void oneClickAfterReattachSendsOneRequest() {
        ManualToggleView view = navigate(ManualToggleView.class);
        Element parent = view.getElement().getParent();
        int index = parent.indexOfChild(view.getElement());
        view.getElement().removeFromParent();
        parent.insertChild(index, view.getElement());
        answerWakeLockCalls();

        test(button("Keep screen awake")).click();

        assertEquals(List.of(REQUEST), answerWakeLockCalls());
    }

    @Test
    void leavingTheViewReleasesTheLock() {
        navigate(ManualToggleView.class);
        test(button("Keep screen awake")).click();
        assertEquals(List.of(REQUEST), answerWakeLockCalls());

        navigate(HomeView.class);

        assertEquals(List.of(RELEASE), answerWakeLockCalls(),
                "the lock must not stay on in the other views");
    }

    @Test
    void unsupportedBrowserDisablesTheButton() {
        navigate(ManualToggleView.class);

        simulateAvailability(WakeLockAvailability.UNSUPPORTED);
        runPendingSignalsTasks();

        assertFalse(button("Keep screen awake").isEnabled());
        assertBadge("Not available — needs a supporting browser and HTTPS");

        simulateAvailability(WakeLockAvailability.SUPPORTED);
        runPendingSignalsTasks();
        assertTrue(button("Keep screen awake").isEnabled());
    }

    private Button button(String text) {
        return findInView(Button.class).withText(text).single();
    }

    private void assertBadge(String text) {
        assertEquals(text, findInView(Span.class).withClassName("status-badge")
                .single().getText());
    }
}
