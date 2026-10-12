package com.example.common;

import java.io.IOException;

import com.example.common.layout.About;
import com.example.common.layout.ParallelLoadingView;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.browserless.BrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.textfield.TextArea;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ViewPackages(classes = ParallelLoadingView.class)
class BaseMainLayoutTest extends BrowserlessTest {

    private FakeSlackWebhook webhook;

    @BeforeEach
    void startWebhook() throws IOException {
        webhook = new FakeSlackWebhook();
        TestMainLayout.slackFeedback = webhook.slackFeedback();
    }

    @AfterEach
    void stopWebhook() {
        webhook.close();
        TestMainLayout.slackFeedback = new SlackFeedback(null);
    }

    @Test
    void feedback_sendsTheOpenUseCaseToSlack() {
        navigate(ParallelLoadingView.class);
        test(feedbackButton()).click();

        Dialog dialog = $(Dialog.class).single();
        assertEquals("Feedback on Use Case 19: Parallel Loading",
                dialog.getHeaderTitle());
        test($(TextArea.class).single()).setValue("Loading never finishes");
        test(button("Send")).click();

        assertFalse(dialog.isOpened());
        assertEquals(1, webhook.texts().size());
        String text = webhook.texts().get(0);
        assertTrue(text.contains("*App:* Signal API"), text);
        assertTrue(text.contains(
                "<https://signals-cases.fly.dev/parallel-loading|Use Case 19: Parallel Loading>"),
                text);
        assertTrue(text.contains(ParallelLoadingView.class.getName()), text);
        assertTrue(text.endsWith("> Loading never finishes"), text);
    }

    @Test
    void feedback_slackFails_keepsTheDialogOpen() {
        webhook.respondWith(500);
        navigate(ParallelLoadingView.class);
        test(feedbackButton()).click();

        test($(TextArea.class).single()).setValue("Loading never finishes");
        test(button("Send")).click();

        assertTrue($(Dialog.class).single().isOpened());
    }

    @Test
    void feedback_isOnlyOfferedOnUseCaseViews() {
        navigate(About.class);

        assertTrue($(Button.class).withText("Feedback").all().isEmpty());
    }

    @Test
    void feedback_withoutWebhook_isNotOffered() {
        TestMainLayout.slackFeedback = new SlackFeedback(null);
        navigate(ParallelLoadingView.class);

        assertTrue($(Button.class).withText("Feedback").all().isEmpty());
    }

    private Button feedbackButton() {
        return button("Feedback");
    }

    private Button button(String text) {
        return $(Button.class).withText(text).single();
    }
}
