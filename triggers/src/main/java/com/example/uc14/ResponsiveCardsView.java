package com.example.uc14;

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
 * UC14 — Responsive card grid driven by SizeTrigger.
 * <p>
 * A grid holds six {@link Card}s. A {@link SizeTrigger} on the view (not the
 * grid) observes its width via {@code ResizeObserver}; the custom
 * {@link ClassByWidthAction} reads {@code event.width} at every fire and
 * applies one of three breakpoint classes ({@code w-narrow}, {@code w-medium},
 * {@code w-wide}) to the grid, which CSS turns into a 1, 2, or 3-column layout.
 * The whole responsive switch happens client-side without a server round-trip
 * on resize.
 * <p>
 * Tracking the view (not the grid) means the breakpoint reacts to the column
 * the view is rendered into, not the grid's own post-layout width.
 * Container-relative, not viewport-relative: the same widget would behave
 * correctly inside a sidebar narrower than the viewport.
 */
@Route(value = "uc14", layout = MainLayout.class)
@PageTitle("UC14 — Responsive cards")
@Menu(order = 14, title = "UC14 — Responsive cards")
@StyleSheet("uc14.css")
public class ResponsiveCardsView extends VerticalLayout {

    public ResponsiveCardsView() {
        addClassName("uc14-view");
        add(new H1("UC14 — Responsive card grid"));
        add(new Paragraph(
                "Resize the browser. Below ~520px the grid is one column; "
                        + "between 520 and 900px it's two; above 900px it's "
                        + "three. SizeTrigger fires on every resize; "
                        + "ClassByWidthAction picks a breakpoint class; CSS "
                        + "translates the class into a grid-template-columns "
                        + "value."));

        Div grid = new Div();
        grid.setId("grid");
        grid.addClassName("card-grid");
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
                "w-narrow", "w-medium", "w-wide"));

        add(grid);
    }
}
