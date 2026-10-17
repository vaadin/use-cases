package com.example.uc22;

import com.example.common.UseCaseDescription;
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
 * A {@link KeyboardEventTrigger} fires on every {@code keydown} anywhere on the
 * page while the view is open. The trigger's typed inputs
 * ({@link KeyboardEventTrigger.EventData#key},
 * {@link KeyboardEventTrigger.EventData#code} and the four modifier flags) feed
 * {@link SetPropertyAction SetPropertyActions} that update the labels in real
 * time. No callback, no server round-trip per key.
 * <p>
 * Flow has no keyboard trigger, so {@code KeyboardEventTrigger} is a local
 * class (API-GAPS.md #10).
 */
@Route(value = "uc22", layout = MainLayout.class)
@PageTitle("UC22 — Key event log")
@UseCaseDescription("Logging every key press with its modifiers as it happens")
@Menu(order = 22, title = "UC22 — Key event log")
@StyleSheet("uc22.css")
public class KeyEventLogView extends VerticalLayout {

    public KeyEventLogView() {
        addClassName("uc22-view");
        add(new H1("UC22 — Key event log"));
        add(new Paragraph("Press any key. The values below show the "
                + "event's key, code and modifier flags as the browser "
                + "reports them. They are written in the browser, so no key "
                + "press goes to the server."));

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
        span.addClassName("uc22-value");
        return span;
    }

    private static HorizontalLayout row(String label, Span value) {
        HorizontalLayout row = new HorizontalLayout();
        row.addClassName("uc22-row");
        Span lbl = new Span(label);
        lbl.addClassName("uc22-label");
        row.add(lbl, value);
        return row;
    }
}
