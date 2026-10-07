package com.example.uc4;

import com.example.views.MainLayout;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.trigger.internal.Action;
import com.vaadin.flow.component.trigger.internal.DoubleClickTrigger;
import com.vaadin.flow.component.trigger.internal.PropertyInput;
import com.vaadin.flow.component.trigger.internal.WriteToClipboardAction;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * UC4 — Double-click to copy.
 * <p>
 * Demonstrates {@link DoubleClickTrigger} — one of the typed trigger subclasses
 * ({@code ClickTrigger}, {@code DoubleClickTrigger}, {@code MouseEventTrigger},
 * {@code DomEventTrigger}, …) that ship in the framework. For events not
 * covered by a built-in subclass, application code can subclass {@code Trigger}
 * directly and emit the listener-installing JS — see
 * {@code com.example.ShortcutTrigger} for an example.
 */
@Route(value = "uc4", layout = MainLayout.class)
@PageTitle("UC4 — Double-click copy")
@Menu(order = 4, title = "UC4 — Double-click copy")
@StyleSheet("uc4.css")
public class JsTriggerView extends VerticalLayout {

    public JsTriggerView() {
        addClassName("uc4-view");
        add(new H1("UC4 — Double-click to copy"));
        add(new Paragraph(
                "Double-click the box below. DoubleClickTrigger is a built-in "
                        + "Trigger subclass — no custom JS or @JsModule. For "
                        + "events without a built-in subclass, write a tiny "
                        + "Trigger subclass (see com.example.ShortcutTrigger)."));

        Div copyTarget = new Div("Double-click me to copy this text");
        copyTarget.setId("target");
        copyTarget.addClassName("copy-target");

        Action.Input<String> text = new PropertyInput<>(copyTarget,
                "textContent", String.class);
        new DoubleClickTrigger(copyTarget)
                .triggers(new WriteToClipboardAction(text, null));

        add(copyTarget);
    }
}
