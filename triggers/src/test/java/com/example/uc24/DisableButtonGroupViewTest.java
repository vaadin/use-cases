package com.example.uc24;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.server.VaadinSession;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = DisableButtonGroupView.class)
class DisableButtonGroupViewTest extends SpringBrowserlessTest {

    private List<Button> group() {
        return List.of(findInView(Button.class).id("approve"),
                findInView(Button.class).id("reject"),
                findInView(Button.class).id("escalate"));
    }

    private String status() {
        return findInView(Span.class).id("status").getText();
    }

    @Test
    void viewRendersThreeEnabledDecisionButtons() {
        navigate(DisableButtonGroupView.class);

        assertTrue(findInView(H1.class).all().stream()
                .anyMatch(h -> "UC24 — Disable a button group on click"
                        .equals(h.getText())));
        List<Button> group = group();
        assertEquals(List.of("Approve", "Reject", "Escalate"),
                group.stream().map(Button::getText).toList());
        group.forEach(button -> {
            assertTrue(button.isEnabled());
            assertTrue(button.isDisableOnClick());
        });
        assertEquals("Request #1042 is waiting for a decision.", status());
    }

    @Test
    void clickDisablesWholeGroupThenReenablesIt() {
        navigate(DisableButtonGroupView.class);
        List<Button> group = group();

        test(group.get(1)).click();

        // The client-side trigger can't be observed here, but the server
        // mirrors it: every button in the group is disabled, not just the
        // clicked one.
        group.forEach(button -> assertFalse(button.isEnabled(),
                button.getText() + " should be disabled while processing"));
        assertEquals("Processing \"Reject\"…", status());

        VaadinSession session = VaadinSession.getCurrent();
        await().pollInSameThread().atMost(Duration.ofSeconds(10)).until(() -> {
            session.getService().runPendingAccessTasks(session);
            return group.stream().allMatch(Button::isEnabled);
        });
        assertTrue(status().startsWith("Request #1042: Rejected."));
    }
}
