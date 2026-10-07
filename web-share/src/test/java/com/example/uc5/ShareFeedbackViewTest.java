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
@ViewPackages(classes = ShareFeedbackView.class)
class ShareFeedbackViewTest extends SpringBrowserlessTest {

    @Test
    void viewRendersHeadingAndButtons() {
        navigate(ShareFeedbackView.class);

        assertTrue(findInView(H1.class).all().stream()
                .anyMatch(h -> "UC5 — Share with completion feedback"
                        .equals(h.getText())));
        assertTrue(findInView(Button.class).all().stream()
                .anyMatch(b -> "Share with feedback".equals(b.getText())));
        assertTrue(findInView(Button.class).all().stream()
                .anyMatch(b -> "Simulate success".equals(b.getText())));
    }

    @Test
    void successAndErrorOutcomesAppendToLog() {
        ShareFeedbackView view = navigate(ShareFeedbackView.class);
        runPendingSignalsTasks();

        view.handleSuccess();
        runPendingSignalsTasks();
        assertLogContains("✓ shared");

        view.handleError("AbortError: Share canceled");
        runPendingSignalsTasks();
        assertLogContains("AbortError");
    }

    private void assertLogContains(String fragment) {
        assertTrue(
                findInView(Div.class).all().stream()
                        .anyMatch(d -> d.getText() != null
                                && d.getText().contains(fragment)),
                "expected outcome log to contain \"" + fragment + "\"");
    }
}
