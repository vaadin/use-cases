package com.example.uc5;

import java.util.List;

import com.example.ScreenOrientationTestSupport;
import com.example.ScreenOrientationTestSupport.PendingLock;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.screenorientation.ScreenOrientationLockErrorCode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = LockErrorView.class)
class LockErrorViewTest extends SpringBrowserlessTest {

    @Test
    void aRejectedLockAddsOneLineWithTheCodeAndTheUserMessage() {
        navigate(LockErrorView.class);
        int before = logLines().size();

        test(button("Lock without fullscreen")).click();
        List<PendingLock> locks = ScreenOrientationTestSupport.pendingLocks();
        assertEquals(1, locks.size());
        locks.get(0).fail(ScreenOrientationLockErrorCode.NOT_SUPPORTED,
                "lock() is not available on this device.");

        List<Div> lines = logLines();
        assertEquals(before + 1, lines.size());
        String newest = lines.get(0).getText();
        assertTrue(lines.get(0).hasClassName("uc5-err"));
        assertTrue(newest.endsWith("✗ landscape-primary — NOT_SUPPORTED: "
                + LockErrorView.userMessage(
                        ScreenOrientationLockErrorCode.NOT_SUPPORTED)
                + " (browser: lock() is not available on this device.)"),
                newest);
    }

    @Test
    void twoLocksInARowAbortTheFirstAndKeepTheSecond() {
        navigate(LockErrorView.class);
        int before = logLines().size();

        test(button("Two locks in a row")).click();
        List<PendingLock> locks = ScreenOrientationTestSupport.pendingLocks();
        assertEquals(List.of("landscape-primary", "portrait-primary"),
                locks.stream().map(PendingLock::orientation).toList());
        locks.get(0).fail(ScreenOrientationLockErrorCode.ABORT, "Aborted");
        locks.get(1).succeed();

        List<Div> lines = logLines();
        assertEquals(before + 2, lines.size());
        assertTrue(
                lines.get(0).getText().endsWith("✓ Locked to portrait-primary"),
                lines.get(0).getText());
        assertTrue(lines.get(1).getText().contains(
                "✗ landscape-primary — ABORT: a newer lock replaced this one."),
                lines.get(1).getText());
    }

    private Button button(String text) {
        return findInView(Button.class).all().stream()
                .filter(b -> text.equals(b.getText())).findFirst()
                .orElseThrow();
    }

    private List<Div> logLines() {
        Component log = findInView(Div.class).all().stream()
                .filter(d -> d.hasClassName("uc5-log")).findFirst()
                .orElseThrow();
        return log.getChildren().map(Div.class::cast).toList();
    }
}
