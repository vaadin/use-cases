package com.example.uc4;

import java.util.List;

import com.example.ScreenOrientationTestSupport;
import com.example.ScreenOrientationTestSupport.PendingLock;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.fullscreen.Fullscreen;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.screenorientation.ScreenOrientationLockErrorCode;
import com.vaadin.flow.component.screenorientation.ScreenOrientationType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = LockForVideoView.class)
class LockForVideoViewTest extends SpringBrowserlessTest {

    @Test
    void playLocksToLandscapeAndStopReleasesIt() {
        navigate(LockForVideoView.class);
        enterFullscreenAndPlay().succeed();
        ScreenOrientationTestSupport.setScreenOrientation(
                ScreenOrientationType.LANDSCAPE_PRIMARY, 90);
        runPendingSignalsTasks();

        assertEquals("Locked to landscape", badge().getText());
        assertEquals("Playing — orientation locked to landscape-primary",
                stageLabel());

        test(button("Stop")).click();
        runPendingSignalsTasks();
        assertEquals("Idle", badge().getText());
        assertEquals("Press Play to start", stageLabel());
    }

    @Test
    void aRejectedLockShowsTheErrorCode() {
        navigate(LockForVideoView.class);
        enterFullscreenAndPlay().fail(
                ScreenOrientationLockErrorCode.NOT_SUPPORTED,
                "screen.orientation.lock() is not available on this device.");
        runPendingSignalsTasks();

        assertEquals("Lock failed: NOT_SUPPORTED", badge().getText());
        assertTrue(badge().hasClassName("error"));
        assertEquals("Press Play to start", stageLabel());
    }

    @Test
    void leavingFullscreenWithoutStopShowsTheLockAsReleased() {
        navigate(LockForVideoView.class);
        enterFullscreenAndPlay().succeed();
        runPendingSignalsTasks();
        assertEquals("Locked to landscape", badge().getText());

        // The user presses Esc: the browser leaves fullscreen and drops the
        // lock.
        Fullscreen.setStateFromClient(UI.getCurrent(), "NOT_FULLSCREEN");
        runPendingSignalsTasks();

        assertEquals("Released — fullscreen was closed", badge().getText());
        assertFalse(badge().hasClassName("error"));
        assertEquals("Press Play to start", stageLabel());
    }

    private PendingLock enterFullscreenAndPlay() {
        // Fullscreen is entered on the client, which browserless can not
        // simulate; report it as the browser would.
        Fullscreen.setStateFromClient(UI.getCurrent(), "FULLSCREEN");
        test(button("Play")).click();
        List<PendingLock> locks = ScreenOrientationTestSupport.pendingLocks();
        assertEquals(List.of("landscape-primary"),
                locks.stream().map(PendingLock::orientation).toList());
        return locks.get(0);
    }

    private Button button(String prefix) {
        return findInView(Button.class).all().stream()
                .filter(b -> b.getText().startsWith(prefix)).findFirst()
                .orElseThrow();
    }

    private Span badge() {
        return findInView(Span.class).all().stream()
                .filter(s -> s.hasClassName("status-badge")).findFirst()
                .orElseThrow();
    }

    private String stageLabel() {
        Div stage = findInView(Div.class).all().stream()
                .filter(d -> d.hasClassName("uc4-stage")).findFirst()
                .orElseThrow();
        return stage.getChildren().filter(Span.class::isInstance)
                .map(c -> ((Span) c).getText())
                .filter(text -> text.startsWith("P")).findFirst().orElseThrow();
    }
}
