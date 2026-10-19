package com.example.components;

import java.util.ArrayList;
import java.util.List;

import com.vaadin.flow.component.AbstractCompositeField;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.shared.HasThemeVariant;
import com.vaadin.flow.shared.Registration;

/**
 * A one-to-five star rating (UC3).
 * <p>
 * It is a field: {@code HasValue<Integer>} through
 * {@link AbstractCompositeField}, so it works with Binder and signals, and 0
 * means "not rated". On top of the value change it fires an event of its own,
 * {@link LowRatingEvent}, when a user gives one or two stars, which is what the
 * application actually wants to react to.
 * <p>
 * It is built for keyboard and screen reader users: a radio group of buttons,
 * each announced as "3 of 5 stars", reachable with Tab. Its look comes from
 * {@code rating-stars.css}, with CSS variables and theme variants for size and
 * color (UC8).
 */
@StyleSheet("rating-stars.css")
public class RatingStars
        extends AbstractCompositeField<RatingStars.Stars, RatingStars, Integer>
        implements HasStyle, HasThemeVariant<RatingStarsVariant> {

    public static final int MAX = 5;

    /** The element the stars live in. */
    @Tag("rating-stars")
    public static class Stars extends Component implements HasComponents {
    }

    /** Fired when a user gives one or two stars. */
    public static class LowRatingEvent extends ComponentEvent<RatingStars> {

        private final int rating;

        public LowRatingEvent(RatingStars source, int rating) {
            super(source, true);
            this.rating = rating;
        }

        public int getRating() {
            return rating;
        }
    }

    private final List<Button> stars = new ArrayList<>();

    public RatingStars() {
        super(0);
        getContent().getElement().setAttribute("role", "radiogroup");
        for (int i = 1; i <= MAX; i++) {
            int rating = i;
            Button star = new Button("★");
            star.addClassName("star");
            star.getElement().setAttribute("role", "radio");
            star.setAriaLabel(rating + " of " + MAX + " stars");
            star.addClickListener(e -> rate(rating));
            stars.add(star);
            getContent().add(star);
        }
        render(0);
    }

    /** A rating field with an accessible name for the whole group. */
    public RatingStars(String label) {
        this();
        getContent().getElement().setAttribute("aria-label", label);
    }

    private void rate(int rating) {
        if (isReadOnly()) {
            return;
        }
        // A value from the user is already "shown" as far as
        // AbstractCompositeField is concerned, so the stars are drawn here
        // (API-GAPS.md #2, vaadin/flow#11392).
        render(rating);
        setModelValue(rating, true);
        if (rating <= 2) {
            fireEvent(new LowRatingEvent(this, rating));
        }
    }

    public Registration addLowRatingListener(
            ComponentEventListener<LowRatingEvent> listener) {
        return addListener(LowRatingEvent.class, listener);
    }

    @Override
    protected void setPresentationValue(Integer value) {
        render(value == null ? 0 : value);
    }

    private void render(int value) {
        for (int i = 0; i < stars.size(); i++) {
            Button star = stars.get(i);
            boolean on = i < value;
            star.setClassName("on", on);
            star.getElement().setAttribute("aria-checked",
                    String.valueOf(i + 1 == value));
        }
    }

    @Override
    public void setReadOnly(boolean readOnly) {
        super.setReadOnly(readOnly);
        stars.forEach(star -> star.setEnabled(!readOnly));
        getContent().getElement().setAttribute("readonly", readOnly);
    }

    // Package-private, for the tester.
    List<Button> stars() {
        return stars;
    }
}
