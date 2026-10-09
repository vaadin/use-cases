package com.example.uc8;

import com.example.ManualLatency;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.notification.Notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = OptimisticSaveView.class)
class OptimisticSaveViewTest extends SpringBrowserlessTest {

    @Autowired
    private ManualLatency latency;

    @BeforeEach
    void resetLatency() {
        latency.reset();
    }

    @Test
    void viewRendersTasks() {
        OptimisticSaveView view = navigate(OptimisticSaveView.class);

        assertEquals("UC8 — Optimistic save",
                findInView(H1.class).single().getText());
        assertEquals(5, view.rows().size());
    }

    @Test
    void changeShowsAtOnceAndIsConfirmedLater() {
        OptimisticSaveView view = navigate(OptimisticSaveView.class);
        OptimisticSaveView.TaskRow row = view.rows().getFirst();

        test(row.checkbox()).click();
        runPendingSignalsTasks();
        assertTrue(row.checkbox().getValue());
        assertEquals("Saving…", row.status());

        latency.completeNext();
        runPendingSignalsTasks();
        assertTrue(row.checkbox().getValue());
        assertEquals("Saved", row.status());
    }

    @Test
    void failedSaveIsRolledBackWithANotification() {
        OptimisticSaveView view = navigate(OptimisticSaveView.class);
        OptimisticSaveView.TaskRow row = view.rows().get(1);
        test(failToggle()).click();

        test(row.checkbox()).click();
        runPendingSignalsTasks();
        assertTrue(row.checkbox().getValue());

        latency.completeNext();
        runPendingSignalsTasks();
        assertFalse(row.checkbox().getValue(), "the change should be undone");
        assertEquals("", row.status());
        assertTrue($(Notification.class).all().stream()
                .anyMatch(n -> test(n).getText().contains(
                        "Couldn't save \"Renew the VAT certificate\"")));
    }

    @Test
    void onlyTheNewestSaveOfARowDecides() {
        OptimisticSaveView view = navigate(OptimisticSaveView.class);
        OptimisticSaveView.TaskRow row = view.rows().get(2);

        test(failToggle()).click();
        test(row.checkbox()).click(); // will fail
        test(failToggle()).click();
        test(row.checkbox()).click(); // untick again, will succeed
        test(row.checkbox()).click(); // tick again, will succeed
        runPendingSignalsTasks();

        // The first, failing save answers while newer ones are pending: the
        // row is not rolled back underneath the user.
        latency.completeNext();
        runPendingSignalsTasks();
        assertTrue(row.checkbox().getValue());

        latency.completePending();
        runPendingSignalsTasks();
        assertTrue(row.checkbox().getValue());
        assertEquals("Saved", row.status());
    }

    private Checkbox failToggle() {
        return findInView(Checkbox.class).all().stream()
                .filter(c -> "Make saving fail".equals(c.getLabel()))
                .findFirst().orElseThrow();
    }
}
