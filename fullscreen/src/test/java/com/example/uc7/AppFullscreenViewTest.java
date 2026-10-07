package com.example.uc7;

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
@ViewPackages(classes = AppFullscreenView.class)
class AppFullscreenViewTest extends SpringBrowserlessTest {

    @Test
    void viewRendersWithEnterExitAndBadge() {
        navigate(AppFullscreenView.class);

        assertTrue(findInView(H1.class).all().stream().anyMatch(
                h -> h.getText().equals("UC7 — View this app fullscreen")));
        assertTrue(findInView(Button.class).all().stream()
                .anyMatch(b -> "Enter fullscreen".equals(b.getText())));
        assertTrue(findInView(Button.class).all().stream()
                .anyMatch(b -> "Exit fullscreen".equals(b.getText())));
    }

    @Test
    void badgeReflectsFullscreenSignal() {
        navigate(AppFullscreenView.class);
        runPendingSignalsTasks();

        FullscreenTestSupport
                .setFullscreenState(FullscreenState.NOT_FULLSCREEN);
        runPendingSignalsTasks();
        assertBadgeContains("Enter fullscreen");

        FullscreenTestSupport.setFullscreenState(FullscreenState.FULLSCREEN);
        runPendingSignalsTasks();
        assertBadgeContains("App is fullscreen");

        FullscreenTestSupport.setFullscreenState(FullscreenState.UNSUPPORTED);
        runPendingSignalsTasks();
        assertBadgeContains("not supported");
    }

    private void assertBadgeContains(String fragment) {
        assertTrue(findInView(Span.class).all().stream()
                .anyMatch(s -> s.getClassNames().contains("status-badge")
                        && s.getText() != null
                        && s.getText().contains(fragment)),
                "expected status badge to contain \"" + fragment + "\"");
    }
}
