package com.example.uc6;

import com.example.MissingAPI.NotificationPermission;
import com.example.WebNotificationSimulator;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = DeniedFallbackView.class)
class DeniedFallbackViewTest extends SpringBrowserlessTest {

    @Test
    void viewRendersHeadingAndHidesHelpUntilDenied() {
        navigate(DeniedFallbackView.class);
        runPendingSignalsTasks();

        assertTrue(findInView(H1.class).all().stream().anyMatch(
                h -> h.getText().startsWith("UC6 — Handle a denied")));
        assertFalse(helpPanelShown());
    }

    @Test
    void deniedShowsHelpAndFallsBackToInApp() {
        DeniedFallbackView view = navigate(DeniedFallbackView.class);
        WebNotificationSimulator.setPermission(NotificationPermission.DENIED);
        runPendingSignalsTasks();

        assertTrue(helpPanelShown());
        assertTrue(findInView(Button.class).all().stream()
                .noneMatch(b -> "Allow notifications".equals(b.getText())));

        test(button("Send me a notification")).click();
        assertLogContains("in-app fallback (permission denied)");
        assertTrue(WebNotificationSimulator.shown().isEmpty());

        // User re-enables notifications in site settings; the browser
        // reports the change and the view switches back.
        WebNotificationSimulator.setPermission(NotificationPermission.GRANTED);
        runPendingSignalsTasks();
        assertFalse(helpPanelShown());

        view.send(UI.getCurrent());
        assertLogContains("OS notification");
        assertEquals(1, WebNotificationSimulator.shown().size());
    }

    @Test
    void checkAgainButtonIsOffered() {
        navigate(DeniedFallbackView.class);
        WebNotificationSimulator.setPermission(NotificationPermission.DENIED);
        runPendingSignalsTasks();

        // The client re-reads the permission; nothing observable server-side
        // until it reports, but the click must not fail.
        test(button("Check again")).click();
        assertTrue(helpPanelShown());
    }

    // findInView only returns visible components, so the heading inside the
    // help panel is found only while the panel is shown.
    private boolean helpPanelShown() {
        return findInView(H3.class).all().stream()
                .anyMatch(h -> h.getText().equals("Notifications are blocked"));
    }

    private Button button(String text) {
        return findInView(Button.class).all().stream()
                .filter(b -> text.equals(b.getText())).findFirst()
                .orElseThrow();
    }

    private void assertLogContains(String fragment) {
        assertTrue(findInView(Div.class).all().stream()
                .anyMatch(d -> d.getText() != null
                        && d.getText().contains(fragment)),
                "expected log to contain \"" + fragment + "\"");
    }
}
