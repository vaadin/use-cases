package com.example.uc5;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * There is no browserless simulator that fires the share's {@code onShared} or
 * {@code onError} callback (API-GAPS.md #3), so the test drives the log through
 * the view's "Simulate" buttons, which call the same handlers.
 */
@SpringBootTest
@ViewPackages(classes = ShareFeedbackView.class)
class ShareFeedbackViewTest extends SpringBrowserlessTest {

    @Test
    void outcomesAreLoggedNewestFirst() {
        navigate(ShareFeedbackView.class);
        assertEquals(List.of("No share yet."), logLines());

        click("Simulate success");
        click("Simulate cancel");
        click("Simulate failure");

        List<String> lines = logLines();
        assertEquals(3, lines.size(), lines.toString());
        assertTrue(
                lines.get(0).endsWith("✗ NotAllowedError: Permission denied"),
                lines.get(0));
        assertTrue(lines.get(1).endsWith("✗ AbortError: Share canceled"),
                lines.get(1));
        assertTrue(lines.get(2).endsWith("✓ shared"), lines.get(2));
    }

    private void click(String text) {
        test(findInView(Button.class).all().stream()
                .filter(b -> text.equals(b.getText())).findFirst()
                .orElseThrow()).click();
    }

    private List<String> logLines() {
        Div log = findInView(Div.class).all().stream()
                .filter(d -> d.hasClassName("uc5-log")).findFirst()
                .orElseThrow();
        return log.getChildren().map(c -> c.getElement().getText()).toList();
    }
}
