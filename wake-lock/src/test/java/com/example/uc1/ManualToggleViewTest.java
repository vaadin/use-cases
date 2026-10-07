package com.example.uc1;

import com.example.WakeLockTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = ManualToggleView.class)
class ManualToggleViewTest extends SpringBrowserlessTest {

    @Test
    void viewRendersHeadingAndToggleButton() {
        navigate(ManualToggleView.class);

        assertTrue(findInView(H1.class).all().stream().anyMatch(
                h -> "UC1 — Manual keep-awake toggle".equals(h.getText())),
                "Heading should render");
        // The toggle button starts as "Keep screen awake".
        assertEquals(1,
                findInView(Button.class).withText("Keep screen awake").all()
                        .size(),
                "Toggle button should render with initial label");
    }

    @Test
    void badgeAndButtonReflectSimulatedState() {
        navigate(ManualToggleView.class);
        runPendingSignalsTasks();

        assertBadgeContains("Released");

        WakeLockTestSupport.simulateAcquired();
        runPendingSignalsTasks();
        assertBadgeContains("Holding lock");
        assertTrue(
                findInView(Button.class).withText("Allow screen to sleep").all()
                        .size() == 1,
                "Button label should switch when the lock is held");

        WakeLockTestSupport.simulateReleased();
        runPendingSignalsTasks();
        assertBadgeContains("Released");
    }

    @Test
    void clickingButtonInvokesWakeLock() {
        navigate(ManualToggleView.class);
        runPendingSignalsTasks();

        Button toggle = findInView(Button.class).withText("Keep screen awake")
                .single();
        test(toggle).click();
        // Click triggers request() (executeJs to the client); confirm via the
        // signal once the client reports ACTIVE.
        WakeLockTestSupport.simulateAcquired();
        runPendingSignalsTasks();
        assertBadgeContains("Holding lock");

        Button release = findInView(Button.class)
                .withText("Allow screen to sleep").single();
        test(release).click();
        WakeLockTestSupport.simulateReleased();
        runPendingSignalsTasks();
        assertBadgeContains("Released");
    }

    private void assertBadgeContains(String fragment) {
        assertTrue(
                findInView(Span.class).all().stream()
                        .anyMatch(s -> s.getText() != null
                                && s.getText().contains(fragment)),
                "expected status badge to contain \"" + fragment + "\"");
    }
}
