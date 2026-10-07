package com.example.uc22;

import com.example.KeyboardEventTrigger;
import com.example.views.MainLayout;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.trigger.internal.SetPropertyAction;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * UC22 — Live key event log.
 * <p>
 * A {@link KeyboardEventTrigger} fires on every {@code keydown} anywhere inside
 * the view. The trigger's typed inputs
 * ({@link KeyboardEventTrigger.EventData#key},
 * {@link KeyboardEventTrigger.EventData#code} and the four modifier flags) feed
 * {@link SetPropertyAction SetPropertyActions} that update the labels in real
 * time. No callback, no server round-trip per key.
 * <p>
 * Demonstrates {@code KeyboardEventTrigger} (a local port of the
 * feature/triggers-actions class) and its EventData inputs.
 */
@Route(value = "uc22", layout = MainLayout.class)
@PageTitle("UC22 — Key event log")
@Menu(order = 22, title = "UC22 — Key event log")
@StyleSheet("uc22.css")
public class KeyEventLogView extends VerticalLayout {

    public KeyEventLogView() {
        addClassName("uc22-view");
        add(new H1("UC22 — Key event log"));
        add(new Paragraph(
                "Type anything inside the view. The labels below mirror "
                        + "event.key, event.code, and the four modifier flags "
                        + "from the trigger's EventData inputs — no Java "
                        + "handler runs per keystroke."));

        Span keySpan = readout("key");
        Span codeSpan = readout("code");
        Span shiftSpan = readout("shift");
        Span ctrlSpan = readout("ctrl");
        Span altSpan = readout("alt");
        Span metaSpan = readout("meta");

        new KeyboardEventTrigger(this).triggers(
                new SetPropertyAction<>(keySpan, "textContent",
                        KeyboardEventTrigger.EventData.key),
                new SetPropertyAction<>(codeSpan, "textContent",
                        KeyboardEventTrigger.EventData.code),
                new SetPropertyAction<>(shiftSpan, "textContent",
                        KeyboardEventTrigger.EventData.shiftKey),
                new SetPropertyAction<>(ctrlSpan, "textContent",
                        KeyboardEventTrigger.EventData.ctrlKey),
                new SetPropertyAction<>(altSpan, "textContent",
                        KeyboardEventTrigger.EventData.altKey),
                new SetPropertyAction<>(metaSpan, "textContent",
                        KeyboardEventTrigger.EventData.metaKey));

        add(row("event.key", keySpan), row("event.code", codeSpan),
                row("shiftKey", shiftSpan), row("ctrlKey", ctrlSpan),
                row("altKey", altSpan), row("metaKey", metaSpan));
    }

    private static Span readout(String id) {
        Span span = new Span("—");
        span.setId(id);
        span.addClassName("value");
        return span;
    }

    private static HorizontalLayout row(String label, Span value) {
        HorizontalLayout row = new HorizontalLayout();
        row.addClassName("row");
        Span lbl = new Span(label);
        lbl.addClassName("label");
        row.add(lbl, value);
        return row;
    }
}
