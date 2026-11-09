package com.example.uc4;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ScheduledFuture;

import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;
import org.jspecify.annotations.Nullable;
import org.springframework.scheduling.TaskScheduler;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.wakelock.WakeLock;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * UC4 — Workout timer.
 * <p>
 * A simple interval timer alternating between <em>Work</em> and <em>Rest</em>
 * phases. While the timer is running the screen must not dim — the user has
 * their hands full and does not touch the screen for tens of seconds at a time.
 * The lock follows a {@code running} signal through
 * {@code Signal.effect(this, ...)}: it is requested whenever the timer starts
 * and released whenever the timer pauses or resets, or the user leaves the
 * view.
 * <p>
 * The ticks run on Spring's {@link TaskScheduler} and only set a signal; the
 * labels are bound to it, so no {@code UI.access} is needed.
 */
@Route(value = "uc4", layout = MainLayout.class)
@PageTitle("UC4 — Workout timer")
@UseCaseDescription("Keeping the screen awake only while a timer is running")
@Menu(order = 4, title = "UC4 — Workout timer")
@StyleSheet("uc4.css")
public class WorkoutTimerView extends VerticalLayout {

    static final int WORK_SECONDS = 30;
    static final int REST_SECONDS = 10;

    enum Phase {
        WORK, REST
    }

    /**
     * The timer's phase and the seconds left in it, as one immutable value so a
     * tick and a reset can not mix.
     */
    record Countdown(Phase phase, int remaining) {

        static final Countdown START = new Countdown(Phase.WORK, WORK_SECONDS);

        Countdown tick() {
            if (remaining > 1) {
                return new Countdown(phase, remaining - 1);
            }
            Phase next = phase == Phase.WORK ? Phase.REST : Phase.WORK;
            return new Countdown(next,
                    next == Phase.WORK ? WORK_SECONDS : REST_SECONDS);
        }
    }

    private static final Duration TICK = Duration.ofSeconds(1);

    private final TaskScheduler taskScheduler;

    private final ValueSignal<Boolean> running = new ValueSignal<>(
            Boolean.FALSE);
    private final ValueSignal<Countdown> countdown = new ValueSignal<>(
            Countdown.START);

    private @Nullable ScheduledFuture<?> tickTask;

    public WorkoutTimerView(TaskScheduler taskScheduler) {
        this.taskScheduler = taskScheduler;

        addClassName("uc4-view");
        add(new H1("UC4 — Workout timer"));
        add(new Paragraph("30 s work, 10 s rest, repeat. The wake lock is "
                + "requested while the timer is running and released as "
                + "soon as you pause, reset or leave this page — nothing "
                + "to switch on or off between sets."));

        Span phaseLabel = new Span();
        phaseLabel.addClassName("uc4-phase");
        phaseLabel.bindText(countdown.map(c -> c.phase().name()));
        Span clockLabel = new Span();
        clockLabel.addClassName("uc4-clock");
        clockLabel.bindText(countdown
                .map(c -> WorkoutTimerView.formatSeconds(c.remaining())));

        Button startPauseButton = new Button();
        startPauseButton.bindText(
                running.map(r -> Boolean.TRUE.equals(r) ? "Pause" : "Start"));
        startPauseButton.addClickListener(
                e -> running.set(!Boolean.TRUE.equals(running.peek())));
        Button resetButton = new Button("Reset");
        resetButton.addClickListener(e -> {
            running.set(Boolean.FALSE);
            countdown.set(Countdown.START);
        });

        Signal<Boolean> active = WakeLock.activeSignal();
        Span lockBadge = new Span();
        lockBadge.addClassName("status-badge");
        lockBadge.bindText(active.map(held -> Boolean.TRUE.equals(held)
                ? "Holding — the screen stays on between sets"
                : "Released"));
        lockBadge.bindClassName("active", active);

        add(phaseLabel, clockLabel);
        add(new HorizontalLayout(startPauseButton, resetButton));
        add(new HorizontalLayout(new Span("Wake lock:"), lockBadge));

        Signal.effect(this, () -> {
            if (Boolean.TRUE.equals(running.get())) {
                WakeLock.request();
                startTicking();
            } else {
                WakeLock.release();
                stopTicking();
            }
        });
        // The effect pauses on detach, but that neither cancels the task it
        // started nor releases the lock.
        whenAttached(ui -> () -> {
            stopTicking();
            WakeLock.release(ui);
        });
    }

    private void startTicking() {
        if (tickTask != null) {
            return;
        }
        // The scheduler thread only sets the signal; the bindings above
        // update the UI under the session lock and push the change. A tick
        // that was already running when the user paused or reset leaves the
        // countdown alone.
        tickTask = taskScheduler.scheduleAtFixedRate(() -> countdown.update(
                c -> Boolean.TRUE.equals(running.peek()) ? c.tick() : c),
                Instant.now().plus(TICK), TICK);
    }

    private void stopTicking() {
        if (tickTask != null) {
            tickTask.cancel(false);
            tickTask = null;
        }
    }

    private static String formatSeconds(int seconds) {
        return String.format("%02d:%02d", seconds / 60, seconds % 60);
    }
}
