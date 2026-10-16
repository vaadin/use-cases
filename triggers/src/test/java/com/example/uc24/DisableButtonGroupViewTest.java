package com.example.uc24;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ViewPackages(classes = DisableButtonGroupView.class)
class DisableButtonGroupViewTest extends SpringBrowserlessTest {

    // Lets the test finish the slow decision by hand instead of waiting.
    @MockitoBean
    private TaskScheduler scheduler;

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

        assertEquals("UC24 — Disable button group",
                findInView(H1.class).single().getText());
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
        runPendingSignalsTasks();

        // The client-side trigger can't be observed here, but the server
        // mirrors it: every button in the group is disabled, not just the
        // clicked one.
        group.forEach(button -> assertFalse(button.isEnabled(),
                button.getText() + " should be disabled while processing"));
        assertEquals("Processing \"Reject\"…", status());

        ArgumentCaptor<Runnable> decision = ArgumentCaptor
                .forClass(Runnable.class);
        verify(scheduler).schedule(decision.capture(), any(Instant.class));
        decision.getValue().run();
        runPendingSignalsTasks();

        group.forEach(button -> assertTrue(button.isEnabled(),
                button.getText() + " should be enabled again"));
        assertTrue(status().startsWith("Request #1042: Rejected."));
    }
}
