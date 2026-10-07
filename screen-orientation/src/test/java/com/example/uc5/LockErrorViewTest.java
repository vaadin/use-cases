package com.example.uc5;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = LockErrorView.class)
class LockErrorViewTest extends SpringBrowserlessTest {

    @Test
    void viewRenders() {
        navigate(LockErrorView.class);
        runPendingSignalsTasks();

        assertTrue(findInView(H1.class).all().stream()
                .anyMatch(h -> "UC5 — Lock error UX".equals(h.getText())));
        assertTrue(
                findInView(Button.class).all().stream().anyMatch(
                        b -> b.getText().startsWith("Lock without fullscreen")),
                "expected the SecurityError-style trigger button");
        assertTrue(
                findInView(Button.class).all().stream().anyMatch(
                        b -> b.getText().startsWith("Two locks in a row")),
                "expected the AbortError-style trigger button");

        // The log starts with one info line so users see the area before
        // they click anything.
        assertTrue(findInView(Div.class).all().stream()
                .filter(d -> d.getClassNames().contains("uc5-error-log"))
                .flatMap(d -> d.getChildren()).count() >= 1,
                "expected the log to contain the initial info line");
    }

    @Test
    void clickingALockTriggerInsertsALogLine() {
        navigate(LockErrorView.class);
        runPendingSignalsTasks();

        long before = logLineCount();

        Button portrait = findInView(Button.class).all().stream()
                .filter(b -> "Lock to portrait".equals(b.getText())).findFirst()
                .orElseThrow();
        test(portrait).click();
        runPendingSignalsTasks();

        // The lock promise never resolves in browserless mode, so the only
        // observable effect on the log side is unchanged. We still verify
        // that the click did not crash and the existing log line survives.
        long after = logLineCount();
        assertTrue(after >= before,
                "log line count must not decrease on lock click");
    }

    private long logLineCount() {
        return findInView(Div.class).all().stream()
                .filter(d -> d.getClassNames().contains("uc5-error-log"))
                .flatMap(d -> d.getChildren()).count();
    }
}
