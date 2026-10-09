package com.example.uc3;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import com.example.MissingAPI;
import com.example.backend.SimulatedLatency;
import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;
import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * UC3 — Long-running job with progress and cancel.
 * <p>
 * Importing a supplier's price list takes a while: the job processes the file
 * in batches on a background thread and reports after each one, so the progress
 * bar moves through server push while the rest of the UI stays responsive. The
 * user can cancel at any time; leaving the view cancels the job too, so nobody
 * keeps paying for work whose result has no one to see it.
 * <p>
 * Flow does not tie background work to a component's lifecycle, so the view
 * does that itself in {@link #onDetach}.
 */
@Route(value = "uc3", layout = MainLayout.class)
@PageTitle("UC3 — Job with progress and cancel")
@UseCaseDescription("Reporting the progress of a background job and stopping it when no longer needed")
@Menu(order = 3, title = "UC3 — Job with progress")
public class LongRunningJobView extends VerticalLayout {

    static final int TOTAL_ROWS = 2_000;
    static final int BATCH_SIZE = 100;
    static final Duration BATCH_DELAY = Duration.ofMillis(400);

    enum JobState {
        IDLE, RUNNING, CANCELLED, DONE
    }

    private final SimulatedLatency latency;
    private final ValueSignal<JobState> state = new ValueSignal<>(
            JobState.IDLE);
    private final ValueSignal<Integer> imported = new ValueSignal<>(0);
    private @Nullable CompletableFuture<Integer> currentBatch;

    public LongRunningJobView(SimulatedLatency latency) {
        this.latency = latency;

        add(new H1("UC3 — Job with progress and cancel"));
        add(new Paragraph("Import a price list of %,d rows. The import "
                .formatted(TOTAL_ROWS)
                + "runs in batches in the background and reports after each "
                + "one. Cancel it, or leave the view while it runs: either "
                + "way the remaining batches are never started."));

        Signal<Boolean> running = state.map(s -> s == JobState.RUNNING);
        Button start = new Button("Import price list", event -> start());
        start.addThemeVariants(ButtonVariant.PRIMARY);
        start.bindEnabled(Signal.not(running));
        Button cancel = new Button("Cancel", event -> cancel());
        cancel.bindEnabled(running);

        ProgressBar progress = new ProgressBar(0, TOTAL_ROWS);
        progress.setWidth("24rem");
        Signal.effect(progress, () -> progress.setValue(imported.get()));

        Span status = new Span();
        status.bindText(() -> switch (state.get()) {
        case IDLE -> "Not started";
        case RUNNING ->
            "Imported %,d of %,d rows…".formatted(imported.get(), TOTAL_ROWS);
        case CANCELLED -> "Cancelled after %,d rows".formatted(imported.get());
        case DONE -> "Done: %,d rows imported".formatted(imported.get());
        });

        add(new HorizontalLayout(start, cancel), progress, status);
    }

    private void start() {
        imported.set(0);
        state.set(JobState.RUNNING);
        runNextBatch(UI.getCurrent());
    }

    private void runNextBatch(UI ui) {
        int from = imported.peek();
        // Stands in for parsing and storing one batch of the price list; the
        // answer is the number of rows the batch stored.
        CompletableFuture<Integer> batch = latency.after(BATCH_DELAY,
                () -> Math.min(BATCH_SIZE, TOTAL_ROWS - from));
        currentBatch = batch;
        batch.whenComplete((count, error) -> {
            if (error != null && MissingAPI.isCancellation(error)) {
                return;
            }
            ui.accessLater(() -> {
                if (currentBatch != batch || state.peek() != JobState.RUNNING) {
                    return;
                }
                currentBatch = null;
                imported.set(from + count);
                if (from + count >= TOTAL_ROWS) {
                    state.set(JobState.DONE);
                } else {
                    runNextBatch(ui);
                }
            }, null).run();
        });
    }

    private void cancel() {
        if (currentBatch != null) {
            currentBatch.cancel(false);
            currentBatch = null;
        }
        if (state.peek() == JobState.RUNNING) {
            state.set(JobState.CANCELLED);
        }
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        cancel();
        super.onDetach(detachEvent);
    }

    // Package-private test seams.
    JobState jobState() {
        return state.peek();
    }

    int importedRows() {
        return imported.peek();
    }
}
