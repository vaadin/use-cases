package com.example.uc6;

import java.util.concurrent.Executor;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.context.bean.override.convention.TestBean;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.browserless.internal.MockVaadin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@WithAnonymousUser
@ViewPackages(classes = ReportView.class)
class ReportViewTest extends SpringBrowserlessTest {

    private static final ManualExecutor EXECUTOR = new ManualExecutor();

    /** Replaces the application's executor with the manual one. */
    @TestBean(name = "reportExecutor")
    private Executor reportExecutor;

    static Executor reportExecutor() {
        return EXECUTOR;
    }

    @Test
    void buttonWaitsUntilThePushedResultArrives() {
        ReportView view = navigate(ReportView.class);

        test(view.buildButton()).click();

        assertFalse(view.buildButton().isEnabled());
        assertTrue(view.progress().isVisible());
        assertEquals("Building…", view.result());
        assertEquals(1, EXECUTOR.pending());

        // The background job finishes now. Its UI.access command is queued
        // until the queue runs; the public roundTrip() does not run it, so
        // the internal helper does (see API-GAPS.md).
        EXECUTOR.runAll();
        MockVaadin.runUIQueue();

        assertTrue(view.buildButton().isEnabled());
        assertFalse(view.progress().isVisible());
        assertEquals("100,000 orders · top customer Kestrel Air",
                view.result());
    }
}
