package com.example.tour;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.function.SerializableRunnable;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * A guided tour built from one {@link TourPopover} that moves from target to
 * target. Vaadin has no tour component (API-GAPS.md #1), so this class does
 * what one would: it keeps the steps in order, shows "Step 2 of 5" with "Back",
 * "Next" and "Skip tour", highlights the current target, and tells listeners
 * whether the user finished or skipped the tour.
 * <p>
 * Once Vaadin ships a tour component, replace this class with it.
 */
public class Tour implements Serializable {

    /** Where the tour is. */
    public enum Status {
        NOT_STARTED, RUNNING, FINISHED, SKIPPED
    }

    /**
     * The status of the tour and the zero-based index of the step it is at, or
     * was at when it ended.
     */
    public record State(Status status, int step,
            int total) implements Serializable {

        /** A tour of the given number of steps that has not started. */
        public static State notStarted(int total) {
            return new State(Status.NOT_STARTED, 0, total);
        }

        /** The state as the views show it, e.g. "Skipped at step 2 of 5". */
        public String describe() {
            return switch (status) {
            case NOT_STARTED -> "Not started";
            case RUNNING -> "At step " + (step + 1) + " of " + total;
            case FINISHED -> "Finished";
            case SKIPPED -> "Skipped at step " + (step + 1) + " of " + total;
            };
        }
    }

    private final TourPopover popover = new TourPopover(this::back, this::next,
            this::skip);
    private final List<SerializableConsumer<State>> endListeners = new ArrayList<>();
    private List<TourStep> steps;
    private final ValueSignal<State> state;

    public Tour(List<TourStep> steps) {
        this.steps = List.copyOf(steps);
        state = new ValueSignal<>(State.notStarted(this.steps.size()));
    }

    /**
     * Dims the page around the current target and blocks the rest of the page
     * while the tour runs.
     *
     * @see TourPopover#setSpotlight(boolean)
     */
    public Tour withSpotlight() {
        popover.setSpotlight(true);
        return this;
    }

    /** Starts the tour at its first step. */
    public void start() {
        startAt(0);
    }

    /**
     * Starts the tour at the step with the given zero-based index. A tour
     * without steps does not start.
     */
    public void startAt(int step) {
        if (!steps.isEmpty()) {
            show(step);
        }
    }

    /** Shows the next step, or finishes the tour at the last step. */
    public void next() {
        if (!isRunning()) {
            return;
        }
        int step = state.peek().step();
        if (step + 1 < steps.size()) {
            show(step + 1);
        } else {
            end(Status.FINISHED);
        }
    }

    /**
     * Shows the previous step, unless the current step or the previous one
     * waits for an action: the user can not do an action again, nor take it
     * back.
     */
    public void back() {
        int step = state.peek().step();
        if (isRunning() && canGoBack(step)) {
            show(step - 1);
        }
    }

    /** Ends the tour before its last step. */
    public void skip() {
        end(Status.SKIPPED);
    }

    /**
     * Replaces the steps of the tour, for a tour whose steps depend on the
     * user. A running tour is {@link #reset()} first.
     */
    public void setSteps(List<TourStep> steps) {
        reset();
        this.steps = List.copyOf(steps);
        state.set(State.notStarted(this.steps.size()));
    }

    /**
     * Closes the tour without ending it, for example when the view is left: end
     * listeners are not called and the state goes back to "Not started".
     */
    public void reset() {
        popover.hide();
        state.set(State.notStarted(steps.size()));
    }

    public boolean isRunning() {
        return state.peek().status() == Status.RUNNING;
    }

    /** Whether the tour is running and shows the given step. */
    public boolean isAt(TourStep step) {
        return isRunning() && steps.get(state.peek().step()) == step;
    }

    /** The state of the tour, for views to bind to. */
    public Signal<State> state() {
        return state.asReadonly();
    }

    /**
     * Adds a listener called with the final state when the tour is finished or
     * skipped.
     */
    public Registration addEndListener(SerializableConsumer<State> listener) {
        endListeners.add(listener);
        return () -> endListeners.remove(listener);
    }

    private void show(int index) {
        TourStep step = steps.get(index);
        SerializableRunnable beforeShow = step.beforeShow();
        if (beforeShow != null) {
            beforeShow.run();
        }
        popover.show(step, index, steps.size(), canGoBack(index));
        state.set(new State(Status.RUNNING, index, steps.size()));
    }

    private boolean canGoBack(int index) {
        return index > 0 && !steps.get(index).waitsForAction()
                && !steps.get(index - 1).waitsForAction();
    }

    private void end(Status status) {
        if (!isRunning()) {
            return;
        }
        popover.hide();
        State ended = new State(status, state.peek().step(), steps.size());
        state.set(ended);
        List.copyOf(endListeners).forEach(listener -> listener.accept(ended));
    }
}
