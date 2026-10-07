package com.example.acme;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;

/**
 * The "What Observability Kit sees" half of a use case: the readout that
 * explains what the reader has just experienced in the {@link AppWindow}
 * above it.
 * <p>
 * It starts hidden and is {@link #reveal() revealed} by the view at the
 * moment the story's problem has just been felt — the first slow query, the
 * first failure — so its explanation arrives with the experience rather than
 * before it. Its steps are collapsible {@link Details}; the view opens the
 * first one and leaves the rest closed, so the reader takes them one at a
 * time.
 * <p>
 * There is no refresh button in this pattern. The view registers a
 * {@link #onRefresh(Runnable) refresher} that recomputes the readout from the
 * kit's current state; {@link #reveal()} and {@link #refreshSoon()} run it at
 * the right moments. The scheduled run happens {@code beforeClientResponse},
 * after the kit's own listeners have recorded the interaction or query that is
 * being handled right now — recording happens after the view's own code has
 * returned, so a refresh from inside a listener would miss the very thing the
 * interaction produced.
 */
public class Investigation extends Div {

    private final Div steps = new Div();
    private Runnable refresher = () -> {
    };
    private boolean refreshScheduled;

    /**
     * @param lead
     *            the sentence that greets the reader when the investigation
     *            appears, e.g. "Noticed the wait? …"
     */
    public Investigation(String lead) {
        addClassName("investigation");
        setVisible(false);
        setWidthFull();

        H2 lens = new H2("What Observability Kit sees");
        lens.addClassName("lens-divider");
        lens.setWidthFull();
        add(lens, new Paragraph(lead), steps);
    }

    /**
     * Adds a collapsible step.
     *
     * @param title
     *            the step's title, numbered in the story's sequence (the
     *            story's own first step lives above the window)
     * @param opened
     *            whether the step starts expanded — normally only the first
     * @param content
     *            the step's content
     * @return the step, should the view need to open or close it later
     */
    public Details step(String title, boolean opened, Component... content) {
        Details step = new Details(title, content);
        step.setOpened(opened);
        step.addClassName("investigation-step");
        steps.add(step);
        return step;
    }

    /**
     * Registers the code that recomputes the readout from the kit's current
     * state. Runs on {@link #reveal()} and on every {@link #refreshSoon()}.
     */
    public void onRefresh(Runnable refresher) {
        this.refresher = refresher;
    }

    /**
     * Shows the investigation and brings it up to date, twice: now, with what
     * the kit has recorded so far, and again before the response is written,
     * with what it records about the interaction being handled right now.
     * Idempotent, so the view can call it from every place the story's problem
     * can be felt.
     */
    public void reveal() {
        setVisible(true);
        refresher.run();
        refreshSoon();
    }

    /**
     * Recomputes the readout before the current response is written. Coalesces
     * repeated calls within one round-trip into a single refresh.
     */
    public void refreshSoon() {
        if (refreshScheduled) {
            return;
        }
        // The flag is armed only once a callback exists to clear it. Armed
        // before the ifPresent, a call while detached would leave it set with
        // nothing scheduled, and every later refresh would return early
        // forever.
        getUI().ifPresent(ui -> {
            refreshScheduled = true;
            ui.beforeClientResponse(this, context -> {
                refreshScheduled = false;
                refresher.run();
            });
        });
    }

    /** Recomputes the readout immediately. */
    public void refreshNow() {
        refresher.run();
    }
}
