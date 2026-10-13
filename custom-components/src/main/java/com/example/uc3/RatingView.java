package com.example.uc3;

import com.example.common.UseCaseDescription;
import com.example.components.RatingStars;
import com.example.views.BuiltWith;
import com.example.views.MainLayout;
import com.example.views.Sample;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * UC3 — A component with its own API and events.
 * <p>
 * A satisfaction survey: the {@link RatingStars} value is bound to a signal
 * like any field, and its own {@link RatingStars.LowRatingEvent} opens a "What
 * went wrong?" box when someone gives one or two stars. The stars are buttons
 * in a radio group, so Tab and Enter work and screen readers announce "3 of 5
 * stars".
 */
@Route(value = "uc3", layout = MainLayout.class)
@PageTitle("UC3 — Own API and events")
@UseCaseDescription("Giving a component a typed value, its own events and keyboard support")
@Menu(order = 3, title = "UC3 — Own API and events")
public class RatingView extends VerticalLayout {

    private final RatingStars rating = new RatingStars("Your rating");
    private final ValueSignal<Integer> value = new ValueSignal<>(0);
    private final TextArea feedback = new TextArea("What went wrong?");
    private final Span status = new Span();

    public RatingView() {
        add(new H1("UC3 — Own API and events"));
        add(new Paragraph("Rate the support you got. One or two stars asks "
                + "what went wrong."));

        rating.bindValue(value, value::set);
        status.addClassName("status");
        status.bindText(value.map(v -> v == 0 ? "Not rated yet"
                : v + " of " + RatingStars.MAX + " stars"));
        feedback.setVisible(false);
        feedback.setWidth("24rem");
        rating.addLowRatingListener(e -> {
            feedback.setVisible(true);
            feedback.focus();
        });
        rating.addValueChangeListener(e -> {
            if (e.getValue() > 2) {
                feedback.setVisible(false);
            }
        });

        add(new Sample("How did we do?", rating, status, feedback));
        add(new BuiltWith("RatingStars",
                "Field on AbstractCompositeField · Java and CSS")
                .point("A HasValue<Integer>, so Binder and signals work with it; 0 means not rated.")
                .point("Its own LowRatingEvent fires for one or two stars, which is what the application reacts to.")
                .point("Buttons in a radio group with aria-checked and \"3 of 5 stars\" labels: Tab and Enter work, and screen readers announce it.")
                .java("components/RatingStars")
                .java("components/RatingStarsVariant")
                .resource("META-INF/resources/rating-stars.css"));
    }

    // Package-private test seams.
    RatingStars rating() {
        return rating;
    }

    TextArea feedback() {
        return feedback;
    }

    String status() {
        return status.getText();
    }
}
