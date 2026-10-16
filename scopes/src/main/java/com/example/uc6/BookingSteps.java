package com.example.uc6;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

/**
 * Step links of the booking wizard. They are plain links with router-ignore, so
 * every step change is a full page load — the harshest case for keeping state.
 * The reload button does the same for the current step.
 */
class BookingSteps extends HorizontalLayout {

    BookingSteps() {
        setDefaultVerticalComponentAlignment(Alignment.BASELINE);
        add(step("uc6", "1. Seat"), step("uc6/passenger", "2. Passenger"),
                step("uc6/confirm", "3. Confirm"),
                new Button("Reload this page",
                        e -> UI.getCurrentOrThrow().getPage().reload()));
    }

    private static Anchor step(String href, String text) {
        Anchor anchor = new Anchor(href, text);
        anchor.setRouterIgnore(true);
        return anchor;
    }
}
