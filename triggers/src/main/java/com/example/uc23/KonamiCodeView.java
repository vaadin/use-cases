package com.example.uc23;

import com.example.SequenceTrigger;
import com.example.views.MainLayout;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.trigger.internal.CallbackAction;
import com.vaadin.flow.component.trigger.internal.LiteralInput;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * UC23 — Konami code Easter egg via SequenceTrigger.
 * <p>
 * A {@link SequenceTrigger} tracks the ordered sequence ↑ ↑ ↓ ↓ ← → ← → B A
 * client-side. Only when the user completes the full sequence does the trigger
 * fire a {@link CallbackAction} that bumps a counter and reveals an Easter-egg
 * message. Partial progress never crosses the network — the state lives in a
 * closure inside the install JS.
 * <p>
 * Demonstrates {@code SequenceTrigger} (a local port of the
 * feature/triggers-actions class).
 */
@Route(value = "uc23", layout = MainLayout.class)
@PageTitle("UC23 — Konami code")
@Menu(order = 23, title = "UC23 — Konami code")
@StyleSheet("uc23.css")
public class KonamiCodeView extends VerticalLayout {

    private int unlocks = 0;

    public KonamiCodeView() {
        addClassName("uc23-view");
        add(new H1("UC23 — Konami code"));
        add(new Paragraph("Type the Konami code anywhere on this page: "
                + "↑ ↑ ↓ ↓ ← → ← → B A. Only the full match fires the "
                + "callback; partial progress stays in the browser and "
                + "the server never sees the wrong keys."));

        Span hint = new Span("(no unlocks yet)");
        hint.setId("hint");
        hint.addClassName("hint");

        new SequenceTrigger(this, Key.ARROW_UP, Key.ARROW_UP, Key.ARROW_DOWN,
                Key.ARROW_DOWN, Key.ARROW_LEFT, Key.ARROW_RIGHT, Key.ARROW_LEFT,
                Key.ARROW_RIGHT, Key.KEY_B, Key.KEY_A)
                .triggers(new CallbackAction<>(String.class, ignored -> {
                    unlocks++;
                    hint.setText("🎉 Unlocked " + unlocks + " time"
                            + (unlocks == 1 ? "" : "s"));
                }, new LiteralInput<>("konami")));

        add(hint);
    }
}
