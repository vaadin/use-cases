package com.example.uc7;

import java.io.Serializable;
import java.util.List;

import com.example.tour.Tour.State;
import com.example.tour.Tour.Status;
import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * A tour whose steps are on three pages (routes). It lives as long as the
 * browser tab ({@link UI}), not as long as a page, so it survives navigation. A
 * step names its page and a target on it by key, because the components of the
 * next page do not exist until the tour navigates there.
 * <p>
 * When a step is on another page, the tour navigates there; the page then
 * {@link #attach(CrossViewPage) attaches} itself and shows the step.
 */
final class CrossViewTour implements Serializable {

    /** A step: the page it is on and the key of its target there. */
    record PageStep(Class<? extends CrossViewPage> page, String target,
            String title, String text) implements Serializable {
    }

    static final List<PageStep> STEPS = List.of(
            new PageStep(CrossViewTourView.class, CrossViewTourView.KPIS,
                    "Your day at a glance",
                    "The dashboard shows today's numbers."),
            new PageStep(CrossViewTourView.class, CrossViewPage.PAGES,
                    "Three pages", "Next takes you to the Orders page."),
            new PageStep(CrossViewOrdersView.class, CrossViewOrdersView.ORDERS,
                    "All orders", "Every order, newest first."),
            new PageStep(CrossViewReportsView.class,
                    CrossViewReportsView.REPORT, "Monthly report",
                    "Sales per month, ready to share."));

    private final ValueSignal<State> state = new ValueSignal<>(
            State.notStarted(STEPS.size()));
    private @Nullable CrossViewPage page;
    private boolean navigating;

    /** The tour of the given browser tab. */
    static CrossViewTour of(UI ui) {
        CrossViewTour tour = ComponentUtil.getData(ui, CrossViewTour.class);
        if (tour == null) {
            tour = new CrossViewTour();
            ComponentUtil.setData(ui, CrossViewTour.class, tour);
        }
        return tour;
    }

    Signal<State> state() {
        return state.asReadonly();
    }

    void start() {
        go(0);
    }

    void next() {
        int step = state.peek().step();
        if (state.peek().status() != Status.RUNNING) {
            return;
        }
        if (step + 1 < STEPS.size()) {
            go(step + 1);
        } else {
            end(Status.FINISHED);
        }
    }

    void back() {
        int step = state.peek().step();
        if (state.peek().status() == Status.RUNNING && step > 0) {
            go(step - 1);
        }
    }

    void skip() {
        end(Status.SKIPPED);
    }

    /** Called by a page when it is shown: shows the current step on it. */
    void attach(CrossViewPage page) {
        this.page = page;
        State current = state.peek();
        if (current.status() == Status.RUNNING
                && STEPS.get(current.step()).page() == page.getClass()) {
            page.showStep(STEPS.get(current.step()), current.step(),
                    STEPS.size());
        }
    }

    /**
     * Called by a page when it is left. Unless the tour itself moves to another
     * page, the user has left the tour: it goes back to "Not started", like the
     * tours of the other views.
     */
    void detach(CrossViewPage page) {
        if (this.page == page) {
            this.page = null;
        }
        if (!navigating) {
            state.set(State.notStarted(STEPS.size()));
        }
    }

    private void go(int index) {
        PageStep step = STEPS.get(index);
        state.set(new State(Status.RUNNING, index, STEPS.size()));
        if (page != null && page.getClass() == step.page()) {
            page.showStep(step, index, STEPS.size());
        } else {
            if (page != null) {
                page.hideStep();
            }
            // The page shows the step when it attaches.
            navigating = true;
            try {
                UI.getCurrentOrThrow().navigate(step.page());
            } finally {
                navigating = false;
            }
        }
    }

    private void end(Status status) {
        State current = state.peek();
        if (current.status() != Status.RUNNING) {
            return;
        }
        if (page != null) {
            page.hideStep();
        }
        state.set(new State(status, current.step(), STEPS.size()));
    }

}
