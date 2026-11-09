package com.example.uc5;

import com.example.TriggerCallbacks;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = IdleWarningView.class)
class IdleWarningViewTest extends SpringBrowserlessTest {

    @Test
    void badgeFollowsTheIdleStateTheBrowserReports() {
        IdleWarningView view = navigate(IdleWarningView.class);
        assertEquals("UC5 — Idle warning",
                findInView(H1.class).single().getText());
        Span badge = findInView(Span.class).id("status");
        assertEquals("active", badge.getText());

        TriggerCallbacks.fire(view, true);
        runPendingSignalsTasks();
        assertEquals("idle", badge.getText());
        assertTrue(badge.hasClassName("uc5-idle"));

        TriggerCallbacks.fire(view, false);
        runPendingSignalsTasks();
        assertEquals("active", badge.getText());
        assertFalse(badge.hasClassName("uc5-idle"));
    }
}
