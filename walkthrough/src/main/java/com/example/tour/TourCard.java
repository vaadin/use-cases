package com.example.tour;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.function.SerializableRunnable;

/**
 * The content of a tour step's popover: the progress ("Step 2 of 5"), the
 * step's title and text, and the "Skip tour", "Back" and "Next" buttons. The
 * last step's "Next" reads "Done".
 */
public class TourCard extends Div {

    private final Span progress = new Span();
    private final H3 title = new H3();
    private final Paragraph text = new Paragraph();
    private final Button back;
    private final Button next;

    public TourCard(SerializableRunnable onBack, SerializableRunnable onNext,
            SerializableRunnable onSkip) {
        addClassName("tour-card");
        progress.addClassName("tour-card-progress");
        title.addClassName("tour-card-title");
        text.addClassName("tour-card-text");

        Button skip = new Button("Skip tour", e -> onSkip.run());
        skip.addThemeVariants(ButtonVariant.TERTIARY);
        back = new Button("Back", e -> onBack.run());
        next = new Button("Next", e -> onNext.run());
        next.addThemeVariants(ButtonVariant.PRIMARY);

        Div buttons = new Div(skip, back, next);
        buttons.addClassName("tour-card-buttons");
        add(progress, title, text, buttons);
    }

    /**
     * Shows the step with the given zero-based index of a tour with the given
     * number of steps.
     */
    public void show(String title, String text, int index, int total,
            boolean waitsForAction) {
        progress.setText("Step " + (index + 1) + " of " + total);
        this.title.setText(title);
        this.text.setText(text);
        back.setVisible(index > 0);
        next.setVisible(!waitsForAction);
        next.setText(index == total - 1 ? "Done" : "Next");
    }
}
