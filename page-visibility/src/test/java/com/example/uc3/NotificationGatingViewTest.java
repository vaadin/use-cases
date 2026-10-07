package com.example.uc3;

import com.example.PageVisibilityTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.page.PageVisibility;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = NotificationGatingView.class)
class NotificationGatingViewTest extends SpringBrowserlessTest {

    @Test
    void viewRendersWithSubscribeAndSendButtons() {
        navigate(NotificationGatingView.class);

        assertTrue(findInView(Button.class).all().stream().anyMatch(
                b -> "Enable browser notifications".equals(b.getText())));
        assertTrue(findInView(Button.class).all().stream()
                .anyMatch(b -> "Send me a notification in 5 seconds"
                        .equals(b.getText())));
    }

    @Test
    void fireNotificationLogsChannelBasedOnPageVisibility() {
        NotificationGatingView view = navigate(NotificationGatingView.class);
        runPendingSignalsTasks();

        // Without a stored subscription, a hidden tab should hit the
        // "no subscription" fallback path; visible should hit the in-tab path.
        setPageVisibility(PageVisibility.VISIBLE);
        view.fireNotification(UI.getCurrent());
        runPendingSignalsTasks();
        assertLogContains("→  in-tab");

        setPageVisibility(PageVisibility.HIDDEN);
        view.fireNotification(UI.getCurrent());
        runPendingSignalsTasks();
        assertLogContains("→  no subscription");
    }

    private void assertLogContains(String fragment) {
        assertTrue(
                findInView(Div.class).all().stream()
                        .anyMatch(d -> d.getText() != null
                                && d.getText().contains(fragment)),
                "expected delivery log to contain \"" + fragment + "\"");
    }

    private void setPageVisibility(PageVisibility state) {
        PageVisibilityTestSupport.setPageVisibility(state);
        runPendingSignalsTasks();
    }
}
