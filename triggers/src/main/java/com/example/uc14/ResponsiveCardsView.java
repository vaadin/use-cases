package com.example.uc14;

import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;

import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.card.CardVariant;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.trigger.internal.SizeTrigger;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * UC14 — Responsive cards.
 * <p>
 * A grid holds six {@link Card}s. A {@link SizeTrigger} on the view (not the
 * grid) observes its width via {@code ResizeObserver}; the custom
 * {@link ClassByWidthAction} reads {@code event.width} at every fire and
 * applies one of three breakpoint classes ({@code uc14-narrow},
 * {@code uc14-medium}, {@code uc14-wide}) to the grid, which CSS turns into a
 * 1, 2, or 3-column layout. The whole responsive switch happens client-side
 * without a server round-trip on resize.
 * <p>
 * Tracking the view (not the grid) means the breakpoint reacts to the column
 * the view is rendered into, not the grid's own post-layout width.
 * Container-relative, not viewport-relative: the same widget would behave
 * correctly inside a sidebar narrower than the viewport.
 */
@Route(value = "uc14", layout = MainLayout.class)
@PageTitle("UC14 — Responsive cards")
@UseCaseDescription("Adapting a card grid to its width with breakpoint classes")
@Menu(order = 14, title = "UC14 — Responsive cards")
@StyleSheet("uc14.css")
public class ResponsiveCardsView extends VerticalLayout {

    public ResponsiveCardsView() {
        addClassName("uc14-view");
        add(new H1("UC14 — Responsive cards"));
        add(new Paragraph("Resize the browser window. When this view is "
                + "narrower than 520 px, the cards are in one column; from "
                + "520 to 899 px, in two; from 900 px, in three. The width "
                + "is the view's, not the window's: SizeTrigger reports it "
                + "on every resize, and ClassByWidthAction sets a class on "
                + "the grid in the browser, without a server round-trip."));

        Div grid = new Div();
        grid.setId("grid");
        grid.addClassName("uc14-grid");
        for (int i = 1; i <= 6; i++) {
            Card card = new Card();
            card.addThemeVariants(CardVariant.OUTLINED);
            card.setTitle(new Div("Card " + i));
            card.add(new Paragraph("Body of card " + i + "."));
            grid.add(card);
        }

        // Track the view's width — the column the view is rendered in —
        // and apply the breakpoint class to the grid inside. Tracking the
        // grid itself would observe its post-layout width, which depends
        // on the breakpoint class we're about to set; tracking the view
        // observes the "space we have to work with".
        new SizeTrigger(this).triggers(new ClassByWidthAction(grid, 520, 900,
                "uc14-narrow", "uc14-medium", "uc14-wide"));

        add(grid);
    }
}
