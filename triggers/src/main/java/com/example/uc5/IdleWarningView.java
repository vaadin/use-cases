package com.example.uc5;

import com.example.views.MainLayout;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.trigger.internal.SetSignalAction;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * UC5 — Idle warning.
 * <p>
 * A custom {@link IdleTrigger} fires after a few seconds without keyboard,
 * pointer, or scroll activity, and again when activity resumes. The trigger's
 * {@link IdleTrigger.EventData#idle} input is wired through
 * {@link SetSignalAction} to a server-side {@link ValueSignal Boolean signal}
 * that the view's badge is bound to.
 */
@Route(value = "uc5", layout = MainLayout.class)
@PageTitle("UC5 — Idle warning")
@Menu(order = 5, title = "UC5 — Idle warning")
@StyleSheet("uc5.css")
public class IdleWarningView extends VerticalLayout {

    public IdleWarningView() {
        addClassName("uc5-view");
        add(new H1("UC5 — Idle warning"));
        add(new Paragraph(
                "Don't touch your mouse or keyboard for 5 seconds. The status "
                        + "below flips to \"idle\"; any activity flips it back to "
                        + "\"active\". The state lives in a server-side ValueSignal, "
                        + "driven entirely by a custom IdleTrigger that wraps "
                        + "window-level activity listeners and a setTimeout."));

        ValueSignal<Boolean> idleSignal = new ValueSignal<>(Boolean.FALSE);

        Span badge = new Span();
        badge.setId("status");
        badge.addClassName("status-badge");
        badge.bindText(idleSignal.map(idle -> idle ? "idle" : "active"));
        badge.getClassNames().bind("idle", idleSignal);

        Button resetButton = new Button("Trigger an event");
        resetButton.setId("reset");
        resetButton.addClickListener(e -> {
            /* the click itself is activity — the JS observer will fire false */ });

        new IdleTrigger(this, 5_000).triggers(new SetSignalAction<>(idleSignal,
                Boolean.class, IdleTrigger.EventData.idle));

        add(badge, resetButton);
    }
}
