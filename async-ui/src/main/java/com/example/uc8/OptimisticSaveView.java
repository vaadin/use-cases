package com.example.uc8;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import com.example.MissingAPI;
import com.example.backend.SimulatedLatency;
import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;
import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * UC8 — Optimistic save.
 * <p>
 * Ticking off a task should feel instant even though saving it takes the
 * backend almost a second. The checkbox therefore changes at once and the save
 * runs in the background, with a small "Saving…" marker on the row. When the
 * save fails, the row goes back to the last state the server confirmed and a
 * notification says so. If the user changes the same row again while a save is
 * still running, only the newest save decides the outcome.
 */
@Route(value = "uc8", layout = MainLayout.class)
@PageTitle("UC8 — Optimistic save")
@UseCaseDescription("Showing a change at once and rolling it back if the save fails")
@Menu(order = 8, title = "UC8 — Optimistic save")
@StyleSheet("uc8.css")
public class OptimisticSaveView extends VerticalLayout {

    static final Duration SAVE_LATENCY = Duration.ofMillis(900);

    private final SimulatedLatency latency;
    private final Checkbox failSaves = new Checkbox("Make saving fail");
    private final List<TaskRow> rows = new ArrayList<>();

    public OptimisticSaveView(SimulatedLatency latency) {
        this.latency = latency;
        addClassName("uc8-view");

        add(new H1("UC8 — Optimistic save"));
        add(new Paragraph("Tick a task: it is done immediately, and saved "
                + "in the background. Turn on failures and tick another one "
                + "— a second later it unticks itself and tells you why."));

        Div list = new Div();
        list.addClassName("task-list");
        for (String task : List.of("Order espresso beans",
                "Renew the VAT certificate",
                "Call Kestrel Air about the " + "delivery",
                "Send the April invoices", "Book the grinder service")) {
            TaskRow row = new TaskRow(task);
            rows.add(row);
            list.add(row);
        }
        add(failSaves, list);
    }

    // Package-private test seam.
    List<TaskRow> rows() {
        return rows;
    }

    final class TaskRow extends Div {

        private final String task;
        private final ValueSignal<Boolean> done = new ValueSignal<>(false);
        private final ValueSignal<String> status = new ValueSignal<>("");
        private final Checkbox checkbox;
        private boolean confirmed;
        private long version;

        TaskRow(String task) {
            this.task = task;
            addClassName("task-row");
            checkbox = new Checkbox(task);
            checkbox.bindValue(done, this::changedByUser);
            Span marker = new Span();
            marker.addClassName("save-status");
            marker.bindText(status);
            add(checkbox, marker);
        }

        private void changedByUser(Boolean value) {
            done.set(value);
            status.set("Saving…");
            long save = ++version;
            boolean fail = failSaves.getValue();
            UI ui = UI.getCurrent();
            latency.after(SAVE_LATENCY, () -> {
                if (fail) {
                    throw new IllegalStateException("The server is busy");
                }
                return value;
            }).whenComplete((saved, error) -> ui
                    .accessLater(() -> saveFinished(save, value, error), null)
                    .run());
        }

        private void saveFinished(long save, boolean value,
                @Nullable Throwable error) {
            if (error == null) {
                confirmed = value;
            }
            if (save != version) {
                // A newer change of this row is on its way; it decides.
                return;
            }
            if (error == null) {
                status.set("Saved");
                return;
            }
            done.set(confirmed);
            status.set("");
            Notification notification = Notification.show("Couldn't save \""
                    + task + "\": " + MissingAPI.unwrap(error).getMessage()
                    + ". The change was undone.");
            notification.addThemeVariants(NotificationVariant.ERROR);
        }

        Checkbox checkbox() {
            return checkbox;
        }

        String status() {
            return status.peek();
        }
    }
}
