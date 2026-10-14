package com.example.uc3;

import com.example.ManualLatency;
import com.example.uc3.LongRunningJobView.JobState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.progressbar.ProgressBar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = LongRunningJobView.class)
class LongRunningJobViewTest extends SpringBrowserlessTest {

    @Autowired
    private ManualLatency latency;

    @BeforeEach
    void resetLatency() {
        latency.reset();
    }

    @Test
    void viewRendersIdle() {
        navigate(LongRunningJobView.class);
        runPendingSignalsTasks();

        assertEquals("UC3 — Job with progress and cancel",
                findInView(H1.class).single().getText());
        assertTrue(button("Import price list").isEnabled());
        assertFalse(button("Cancel").isEnabled());
        assertTrue(hasText("Not started"));
    }

    @Test
    void progressAdvancesBatchByBatchUntilDone() {
        LongRunningJobView view = navigate(LongRunningJobView.class);
        test(button("Import price list")).click();
        runPendingSignalsTasks();
        assertFalse(button("Import price list").isEnabled());

        completeBatches(3);
        assertEquals(3 * LongRunningJobView.BATCH_SIZE, view.importedRows());
        assertEquals(300, findInView(ProgressBar.class).single().getValue());
        assertTrue(hasText("Imported 300 of 2,000 rows…"));

        completeBatches(
                LongRunningJobView.TOTAL_ROWS / LongRunningJobView.BATCH_SIZE
                        - 3);
        assertEquals(JobState.DONE, view.jobState());
        assertTrue(latency.pending().isEmpty());
        assertTrue(hasText("Done: 2,000 rows imported"));
    }

    @Test
    void cancelStopsTheRemainingBatches() {
        LongRunningJobView view = navigate(LongRunningJobView.class);
        test(button("Import price list")).click();
        completeBatches(2);

        test(button("Cancel")).click();
        runPendingSignalsTasks();
        // The batch that was running when the user cancelled answers anyway.
        latency.completePending();
        runPendingSignalsTasks();

        assertEquals(JobState.CANCELLED, view.jobState());
        assertEquals(200, view.importedRows());
        assertTrue(latency.pending().isEmpty(), "no further batch starts");
        assertTrue(hasText("Cancelled after 200 rows"));
    }

    @Test
    void leavingTheViewCancelsTheJob() {
        LongRunningJobView view = navigate(LongRunningJobView.class);
        test(button("Import price list")).click();
        completeBatches(1);

        view.getElement().removeFromParent();
        latency.completePending();
        runPendingSignalsTasks();

        assertEquals(JobState.CANCELLED, view.jobState());
        assertTrue(latency.pending().isEmpty(), "no further batch starts");
    }

    @Test
    void failingBatchEndsTheJob() {
        LongRunningJobView view = navigate(LongRunningJobView.class);
        test(button("Import price list")).click();
        completeBatches(1);

        latency.failNext(new IllegalStateException("Disk full"));
        runPendingSignalsTasks();

        assertEquals(JobState.FAILED, view.jobState());
        assertTrue(latency.pending().isEmpty(), "no further batch starts");
        assertTrue(button("Import price list").isEnabled());
        assertTrue(hasText("Failed after 100 rows"));
    }

    private void completeBatches(int count) {
        for (int i = 0; i < count; i++) {
            latency.completeNext();
            runPendingSignalsTasks();
        }
    }

    private Button button(String text) {
        return findInView(Button.class).all().stream()
                .filter(button -> text.equals(button.getText())).findFirst()
                .orElseThrow();
    }

    private boolean hasText(String text) {
        return findInView(Span.class).all().stream()
                .anyMatch(span -> text.equals(span.getText()));
    }
}
