package com.example.tour;

import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.popover.Popover;
import com.vaadin.flow.component.popover.PopoverVariant;
import com.vaadin.flow.function.SerializableRunnable;

/**
 * The popover of a tour: it shows one {@link TourStep} at a time in a
 * {@link TourCard} next to the step's target, and highlights the target.
 * <p>
 * The tour opens and closes it; clicks on the target or outside it do not.
 * Pressing Escape closes it in the browser, which counts as skipping the tour.
 * <p>
 * The browser opens it once its target is on screen, not the server
 * (API-GAPS.md #6): {@link #isOpened()} turns {@code true} only when the
 * browser reports it, while {@link #isShowing()} tells whether a step is shown.
 */
@JsModule("./tour-popover.ts")
public class TourPopover extends Popover {

    private static final String HIGHLIGHT = "tour-highlight";
    private static final String SPOTLIGHT = "tour-spotlight";

    private final TourCard card;
    private String highlightClass = HIGHLIGHT;
    private @Nullable Component highlighted;
    private boolean showing;

    public TourPopover(SerializableRunnable onBack, SerializableRunnable onNext,
            SerializableRunnable onSkip) {
        card = new TourCard(onBack, onNext, onSkip);
        getElement().getClassList().add("tour-popover");
        setOpenOnClick(false);
        setOpenOnHover(false);
        setOpenOnFocus(false);
        setCloseOnOutsideClick(false);
        addThemeVariants(PopoverVariant.ARROW);
        setAriaLabel("Tour");
        add(card);
        addOpenedChangeListener(e -> {
            if (e.isFromClient() && !e.isOpened() && showing) {
                onSkip.run();
            }
        });
    }

    /**
     * Dims the page around the target and blocks the rest of the page while a
     * step is shown. The popover's own backdrop would dim the target too
     * (API-GAPS.md #2), so the target casts a large shadow over the page
     * instead.
     */
    public void setSpotlight(boolean spotlight) {
        setModal(spotlight, false);
        highlightClass = spotlight ? SPOTLIGHT : HIGHLIGHT;
    }

    /**
     * Shows the given step, the one with the given zero-based index of a tour
     * with the given number of steps, with "Back" only if the tour can go back
     * from it.
     */
    public void show(TourStep step, int index, int total, boolean canGoBack) {
        highlight(step.target());
        card.show(step.title(), step.text(), index, total,
                step.waitsForAction(), canGoBack);
        setPosition(step.position());
        setTarget(step.target());
        showing = true;
        getElement().executeJs("window.walkthroughShowStep(this)");
    }

    /** Closes the popover and removes the highlight. */
    public void hide() {
        highlight(null);
        if (showing) {
            showing = false;
            getElement().executeJs("window.walkthroughHideStep(this)");
        }
        close();
    }

    /** Whether a step is shown, or waits in the browser to be shown. */
    boolean isShowing() {
        return showing;
    }

    private void highlight(@Nullable Component target) {
        if (highlighted != null) {
            highlighted.getElement().getClassList().remove(highlightClass);
        }
        highlighted = target;
        if (target != null) {
            target.getElement().getClassList().add(highlightClass);
        }
    }
}
