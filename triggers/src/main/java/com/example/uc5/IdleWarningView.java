package com.example.uc5;

import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;

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
 * A custom {@link IdleTrigger} fires after five seconds without mouse,
 * keyboard, wheel or touch activity, and again when activity resumes. Its
 * {@link IdleTrigger.EventData#idle} input goes through a
 * {@link SetSignalAction} into a {@link ValueSignal}, and the badge is bound to
 * that signal. Only the two changes reach the server, not every mouse move.
 */
@Route(value = "uc5", layout = MainLayout.class)
@PageTitle("UC5 — Idle warning")
@UseCaseDescription("Warning users after a few seconds of inactivity")
@Menu(order = 5, title = "UC5 — Idle warning")
@StyleSheet("uc5.css")
public class IdleWarningView extends VerticalLayout {

    public IdleWarningView() {
        addClassName("uc5-view");
        add(new H1("UC5 — Idle warning"));
        add(new Paragraph("Do not touch the mouse, keyboard or screen for 5 "
                + "seconds. The badge changes to \"idle\". Any activity "
                + "changes it back to \"active\". The browser tracks the "
                + "activity and tells the server only when the state "
                + "changes."));

        ValueSignal<Boolean> idle = new ValueSignal<>(Boolean.FALSE);

        Span badge = new Span();
        badge.setId("status");
        badge.addClassName("uc5-status");
        badge.bindText(idle.map(value -> value ? "idle" : "active"));
        badge.getClassNames().bind("uc5-idle", idle);

        new IdleTrigger(this, 5_000).triggers(new SetSignalAction<>(idle,
                Boolean.class, IdleTrigger.EventData.idle));

        add(badge);
    }
}
