package com.example.uc23;

import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.trigger.internal.CallbackAction;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * UC23 — Konami code.
 * <p>
 * A {@link SequenceTrigger} watches for the key sequence ↑ ↑ ↓ ↓ ← → ← → B A in
 * the browser. Only a completed sequence runs the {@link CallbackAction}, which
 * counts the unlocks on the server. The keys pressed on the way never reach the
 * server.
 * <p>
 * Flow has no trigger for a key sequence, so {@code SequenceTrigger} is a local
 * class (API-GAPS.md #11).
 */
@Route(value = "uc23", layout = MainLayout.class)
@PageTitle("UC23 — Konami code")
@UseCaseDescription("Reacting to a key sequence tracked entirely in the browser")
@Menu(order = 23, title = "UC23 — Konami code")
@StyleSheet("uc23.css")
public class KonamiCodeView extends VerticalLayout {

    public KonamiCodeView() {
        addClassName("uc23-view");
        add(new H1("UC23 — Konami code"));
        add(new Paragraph("Type the Konami code anywhere on this page: "
                + "↑ ↑ ↓ ↓ ← → ← → B A. Only the full sequence reaches the "
                + "server. The browser keeps track of the keys on the way, "
                + "and extra presses before the sequence do not spoil it."));

        ValueSignal<Integer> unlocks = new ValueSignal<>(0);

        Span hint = new Span();
        hint.setId("hint");
        hint.addClassName("uc23-hint");
        hint.bindText(unlocks.map(count -> count == 0 ? "(no unlocks yet)"
                : "🎉 Unlocked " + count + " time" + (count == 1 ? "" : "s")));

        new SequenceTrigger(this, Key.ARROW_UP, Key.ARROW_UP, Key.ARROW_DOWN,
                Key.ARROW_DOWN, Key.ARROW_LEFT, Key.ARROW_RIGHT, Key.ARROW_LEFT,
                Key.ARROW_RIGHT, Key.KEY_B, Key.KEY_A).triggers(
                        new CallbackAction<>(() -> unlocks.update(c -> c + 1)));

        add(hint);
    }
}
