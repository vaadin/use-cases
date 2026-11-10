package com.example;

import java.util.List;

import com.example.tour.TourPopover;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.popover.Popover;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Reads and drives the step of a tour that is shown in an open popover, and the
 * tour status line of a view.
 */
public abstract class TourTestSupport extends SpringBrowserlessTest {

    /**
     * The popovers that are open. A tour's popover opens in the browser once
     * its target is on screen, so for it this means it shows a step.
     */
    protected List<Popover> openPopovers() {
        return find(Popover.class)
                .withCondition(p -> p instanceof TourPopover tourPopover
                        ? tourPopover.isShowing()
                        : p.isOpened())
                .all();
    }

    /** The one open popover. */
    protected Popover openPopover() {
        List<Popover> open = openPopovers();
        assertEquals(1, open.size(), "open popovers");
        return open.get(0);
    }

    /** The title of the step that is shown. */
    protected String stepTitle() {
        return find(H3.class).from(openPopover()).single().getText();
    }

    /** The progress of the step that is shown, e.g. "Step 2 of 5". */
    protected String stepProgress() {
        return find(Span.class).from(openPopover())
                .withClassName("tour-card-progress").single().getText();
    }

    /** The component the step that is shown points at. */
    protected Component stepTarget() {
        Component target = openPopover().getTarget();
        assertNotNull(target, "the open popover has no target");
        return target;
    }

    /** Clicks a button of the step that is shown. */
    protected void clickInStep(String text) {
        test(find(Button.class).from(openPopover()).withText(text).single())
                .click();
        runPendingSignalsTasks();
    }

    /** Whether the step that is shown has a visible button. */
    protected boolean stepHasButton(String text) {
        return find(Button.class).from(openPopover()).withText(text).exists();
    }

    /** The text of the view's tour status line. */
    protected String status() {
        runPendingSignalsTasks();
        return findInView(Span.class).withClassName("tour-status").single()
                .getText();
    }

    /** Clicks a button of the view. */
    protected void clickInView(String text) {
        test(findInView(Button.class).withText(text).single()).click();
        runPendingSignalsTasks();
    }
}
