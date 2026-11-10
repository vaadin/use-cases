package com.example.tour;

import java.io.Serializable;

import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.popover.PopoverPosition;
import com.vaadin.flow.function.SerializableRunnable;

/**
 * One step of a {@link Tour}: the component it points at and what it says about
 * it.
 *
 * @param target
 *            the component the step points at
 * @param title
 *            the heading of the step
 * @param text
 *            what the step tells the user
 * @param position
 *            where the step's popover opens relative to the target
 * @param beforeShow
 *            what to do before the step is shown, such as opening the closed
 *            panel the target is in, or {@code null}
 * @param waitsForAction
 *            whether the step waits for the user to do what it says instead of
 *            offering a "Next" button
 */
public record TourStep(Component target, String title, String text,
        PopoverPosition position, @Nullable SerializableRunnable beforeShow,
        boolean waitsForAction) implements Serializable {

    /**
     * A step that opens below its target and moves on with "Next".
     */
    public static TourStep of(Component target, String title, String text) {
        return new TourStep(target, title, text, PopoverPosition.BOTTOM, null,
                false);
    }

    /** This step, opening at the given side of its target. */
    public TourStep at(PopoverPosition position) {
        return new TourStep(target, title, text, position, beforeShow,
                waitsForAction);
    }

    /** This step, running the given action before it is shown. */
    public TourStep beforeShow(SerializableRunnable action) {
        return new TourStep(target, title, text, position, action,
                waitsForAction);
    }

    /**
     * This step, without a "Next" button: the view moves the tour on with
     * {@link Tour#next()} once the user has done what the step asks.
     */
    public TourStep waitingForAction() {
        return new TourStep(target, title, text, position, beforeShow, true);
    }
}
