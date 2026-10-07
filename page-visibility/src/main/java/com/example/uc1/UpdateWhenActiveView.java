package com.example.uc1;

import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ScheduledFuture;

import com.example.views.MainLayout;
import org.jspecify.annotations.Nullable;
import org.springframework.scheduling.TaskScheduler;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
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
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.Signal;

/**
 * UC1 — Update when active.
 * <p>
 * A live server clock updates every second while the tab is visible. The effect
 * of {@link com.vaadin.flow.component.page.Page#pageVisibilitySignal()} is
 * reactive: as soon as the user hides the tab the periodic task is cancelled,
 * so the websocket stays quiet. On return, ticking resumes.
 */
@Route(value = "uc1", layout = MainLayout.class)
@Menu(order = 1, title = "UC1 — Update when active")
@StyleSheet("uc1.css")
public class UpdateWhenActiveView extends VerticalLayout {

    private static final DateTimeFormatter TIME = DateTimeFormatter
            .ofPattern("HH:mm:ss");

    private final TaskScheduler taskScheduler;

    private final Span timeLabel = new Span("--:--:--");
    private final Span counterLabel = new Span("0");
    private final Span statusBadge = new Span();

    private @Nullable ScheduledFuture<?> tickTask;
    private int updates;

    public UpdateWhenActiveView(TaskScheduler taskScheduler) {
        this.taskScheduler = taskScheduler;

        addClassName("uc1-view");
        add(new H1("UC1 — Update when active"));
        add(new Paragraph("The card below updates the server clock every "
                + "second only while the tab is visible. Hide the tab "
                + "for a few seconds, then return — the counter has not "
                + "advanced and no traffic was sent in the meantime."));

        timeLabel.addClassName("uc1-time");
        counterLabel.addClassName("uc1-counter");
        statusBadge.addClassName("status-badge");

        HorizontalLayout clockRow = new HorizontalLayout(
                new Span("Server time:"), timeLabel);
        clockRow.setAlignItems(Alignment.BASELINE);
        HorizontalLayout counterRow = new HorizontalLayout(
                new Span("Updates received:"), counterLabel);
        counterRow.setAlignItems(Alignment.BASELINE);

        add(clockRow, counterRow, new H2("Status"), statusBadge);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        UI ui = attachEvent.getUI();
        Signal<PageVisibility> visibility = ui.getPage().pageVisibilitySignal();

        statusBadge.bindText(visibility.map(UpdateWhenActiveView::statusText));
        statusBadge.bindClassName("paused",
                visibility.map(s -> s == PageVisibility.VISIBLE_NOT_FOCUSED
                        || s == PageVisibility.UNKNOWN));
        statusBadge.bindClassName("hidden",
                visibility.map(s -> s == PageVisibility.HIDDEN));

        Signal.effect(this, () -> {
            if (visibility.get() == PageVisibility.VISIBLE) {
                startTicking(ui);
            } else {
                stopTicking();
            }
        });
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        stopTicking();
        super.onDetach(detachEvent);
    }

    private void startTicking(UI ui) {
        if (tickTask != null && !tickTask.isCancelled()) {
            return;
        }
        tickTask = taskScheduler.scheduleAtFixedRate(ui.accessLater(() -> {
            updates++;
            timeLabel.setText(LocalTime.now().format(TIME));
            counterLabel.setText(Integer.toString(updates));
        }, null), Duration.ofSeconds(1));
    }

    private void stopTicking() {
        if (tickTask != null) {
            tickTask.cancel(false);
            tickTask = null;
        }
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
