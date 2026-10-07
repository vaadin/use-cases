package com.example.uc4;

import java.time.Duration;
import java.util.concurrent.ScheduledFuture;

import com.example.views.MainLayout;
import org.jspecify.annotations.Nullable;
import org.springframework.scheduling.TaskScheduler;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.wakelock.WakeLock;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * UC4 — Workout interval timer.
 * <p>
 * A simple HIIT-style interval timer alternating between <em>Work</em> and
 * <em>Rest</em> phases. While the timer is running the screen must not dim —
 * the user has their hands full and won't tap the screen for tens of seconds at
 * a time. The lock is coupled to a {@code running} {@link Signal} via
 * {@link Signal#effect(Object, Runnable)}, so the lock is requested whenever
 * the timer starts and released whenever the timer pauses or resets.
 */
@Route(value = "uc4", layout = MainLayout.class)
@Menu(order = 4, title = "UC4 — Workout timer")
@StyleSheet("uc4.css")
public class WorkoutTimerView extends VerticalLayout {

    private static final int WORK_SECONDS = 30;
    private static final int REST_SECONDS = 10;

    private enum Phase {
        WORK, REST
    }

    private final TaskScheduler taskScheduler;

    private final ValueSignal<Boolean> running = new ValueSignal<>(
            Boolean.FALSE);
    private final ValueSignal<Phase> phase = new ValueSignal<>(Phase.WORK);
    private final ValueSignal<Integer> remaining = new ValueSignal<>(
            WORK_SECONDS);

    private final Span clockLabel = new Span();
    private final Span phaseLabel = new Span();
    private final Span lockBadge = new Span();
    private final Button startPauseButton = new Button();
    private final Button resetButton = new Button("Reset");

    private @Nullable ScheduledFuture<?> tickTask;

    public WorkoutTimerView(TaskScheduler taskScheduler) {
        this.taskScheduler = taskScheduler;

        addClassName("uc4-view");
        add(new H1("UC4 — Workout interval timer"));
        add(new Paragraph("30s Work, 10s Rest, repeat. The wake lock is "
                + "requested while the timer is running and released the "
                + "moment you pause or reset — no manual toggling between "
                + "sets."));

        clockLabel.addClassName("timer-clock");
        phaseLabel.addClassName("timer-phase");
        lockBadge.addClassName("status-badge");

        add(phaseLabel, clockLabel);
        add(new HorizontalLayout(startPauseButton, resetButton));
        add(new HorizontalLayout(new Span("Wake lock:"), lockBadge));

        clockLabel.bindText(remaining.map(WorkoutTimerView::formatSeconds));
        phaseLabel.bindText(phase.map(p -> p.name()));
        startPauseButton.bindText(
                running.map(r -> Boolean.TRUE.equals(r) ? "Pause" : "Start"));

        startPauseButton.addClickListener(
                e -> running.set(!Boolean.TRUE.equals(running.peek())));
        resetButton.addClickListener(e -> {
            running.set(Boolean.FALSE);
            phase.set(Phase.WORK);
            remaining.set(WORK_SECONDS);
        });
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        UI ui = attachEvent.getUI();
        Signal<Boolean> active = WakeLock.activeSignal();

        lockBadge.bindText(active.map(held -> Boolean.TRUE.equals(held)
                ? "Holding — screen will stay on between sets"
                : "Released"));
        lockBadge.bindClassName("active", active);

        Signal.effect(this, () -> {
            if (Boolean.TRUE.equals(running.get())) {
                WakeLock.request();
                startTicking(ui);
            } else {
                WakeLock.release();
                stopTicking();
            }
        });
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        stopTicking();
        WakeLock.release();
        super.onDetach(detachEvent);
    }

    private void startTicking(UI ui) {
        if (tickTask != null && !tickTask.isCancelled()) {
            return;
        }
        tickTask = taskScheduler.scheduleAtFixedRate(
                ui.accessLater(this::tick, null), Duration.ofSeconds(1));
    }

    private void stopTicking() {
        if (tickTask != null) {
            tickTask.cancel(false);
            tickTask = null;
        }
    }

    private void tick() {
        // A tick may already be queued on the UI thread by the time the
        // user pauses or resets. Skip the mutation rather than racing the
        // reset back to its starting value.
        if (!Boolean.TRUE.equals(running.peek())) {
            return;
        }
        int next = remaining.peek() - 1;
        if (next > 0) {
            remaining.set(next);
            return;
        }
        // Phase rollover — flip and reset the countdown.
        Phase nextPhase = phase.peek() == Phase.WORK ? Phase.REST : Phase.WORK;
        phase.set(nextPhase);
        remaining.set(nextPhase == Phase.WORK ? WORK_SECONDS : REST_SECONDS);
    }

    private static String formatSeconds(int seconds) {
        return String.format("%02d:%02d", seconds / 60, seconds % 60);
    }
}
