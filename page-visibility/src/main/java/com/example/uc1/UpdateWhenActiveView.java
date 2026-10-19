package com.example.uc1;

import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ScheduledFuture;

import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;
import org.jspecify.annotations.Nullable;
import org.springframework.scheduling.TaskScheduler;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.PageVisibility;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * UC1 — Update when active.
 * <p>
 * A live server clock updates every second while the tab is visible. The effect
 * of {@link com.vaadin.flow.component.page.Page#pageVisibilitySignal()} is
 * reactive: as soon as the user hides the tab the periodic task is cancelled,
 * so the websocket stays quiet. On return, ticking resumes.
 */
@Route(value = "uc1", layout = MainLayout.class)
@PageTitle("UC1 — Update when active")
@UseCaseDescription("Pausing periodic updates while the browser tab is hidden")
@Menu(order = 1, title = "UC1 — Update when active")
@StyleSheet("uc1.css")
public class UpdateWhenActiveView extends VerticalLayout {

    private static final DateTimeFormatter TIME = DateTimeFormatter
            .ofPattern("HH:mm:ss");

    private final TaskScheduler taskScheduler;

    private final ValueSignal<LocalTime> time = new ValueSignal<>(
            LocalTime.now());
    private final ValueSignal<Integer> updates = new ValueSignal<>(0);

    private @Nullable ScheduledFuture<?> tickTask;

    public UpdateWhenActiveView(TaskScheduler taskScheduler) {
        this.taskScheduler = taskScheduler;

        addClassName("uc1-view");
        add(new H1("UC1 — Update when active"));
        add(new Paragraph("The card below updates the server clock every "
                + "second only while the tab is visible. Hide the tab "
                + "for a few seconds, then return — the counter has not "
                + "advanced and no traffic was sent in the meantime."));

        Span timeLabel = new Span();
        timeLabel.addClassName("uc1-time");
        timeLabel.bindText(time.map(TIME::format));
        Span counterLabel = new Span();
        counterLabel.addClassName("uc1-counter");
        counterLabel.bindText(updates.map(String::valueOf));

        Signal<PageVisibility> visibility = UI.getCurrent().getPage()
                .pageVisibilitySignal();
        Span statusBadge = new Span();
        statusBadge.addClassName("status-badge");
        statusBadge.bindText(visibility.map(UpdateWhenActiveView::statusText));
        statusBadge.bindClassName("paused",
                visibility.map(s -> s == PageVisibility.VISIBLE_NOT_FOCUSED
                        || s == PageVisibility.UNKNOWN));
        statusBadge.bindClassName("hidden",
                visibility.map(s -> s == PageVisibility.HIDDEN));

        HorizontalLayout clockRow = new HorizontalLayout(
                new Span("Server time:"), timeLabel);
        clockRow.setAlignItems(Alignment.BASELINE);
        HorizontalLayout counterRow = new HorizontalLayout(
                new Span("Updates received:"), counterLabel);
        counterRow.setAlignItems(Alignment.BASELINE);

        add(clockRow, counterRow, new H2("Status"), statusBadge);

        Signal.effect(this, () -> {
            if (visibility.get() == PageVisibility.VISIBLE) {
                startTicking();
            } else {
                stopTicking();
            }
        });
        // The effect is paused on detach, but that does not cancel a task it
        // has already started.
        whenAttached(ui -> this::stopTicking);
    }

    private void startTicking() {
        if (tickTask != null) {
            return;
        }
        // Signals can be set from the scheduler thread; the bindings above
        // update the UI under the session lock and push the change.
        tickTask = taskScheduler.scheduleAtFixedRate(() -> {
            time.set(LocalTime.now());
            updates.update(n -> n + 1);
        }, Duration.ofSeconds(1));
    }

    private void stopTicking() {
        if (tickTask != null) {
            tickTask.cancel(false);
            tickTask = null;
        }
    }

    // Package-private test seam: the number of ticks so far, also after the
    // view is detached and its labels no longer update.
    int updates() {
        return updates.peek();
    }

    private static String statusText(PageVisibility state) {
        return switch (state) {
        case VISIBLE -> "Updating…";
        case VISIBLE_NOT_FOCUSED -> "Paused — window not focused";
        case HIDDEN -> "Paused — tab hidden";
        case UNKNOWN -> "Visibility unknown";
        };
    }
}
