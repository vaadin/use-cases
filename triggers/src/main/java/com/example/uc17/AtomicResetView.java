package com.example.uc17;

import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.trigger.internal.ClickTrigger;
import com.vaadin.flow.component.trigger.internal.SetPropertyAction;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * UC17 — Atomic reset.
 * <p>
 * A small form with three fields, plus a Reset button. The Reset button's
 * {@link ClickTrigger} runs three {@link SetPropertyAction}s, one per field,
 * each writing an empty value. All three fields change in the same click,
 * without a server round-trip and without a callback.
 * <p>
 * A property written by an action reaches the server only if the component
 * synchronizes it on an event that the write fires (API-GAPS.md #8). The
 * checkbox synchronizes {@code checked} on {@code checked-changed}, which
 * fires. A text field synchronizes {@code value} on {@code change}, which a
 * script write does not fire, so a {@link FireChangeAction} fires it after the
 * writes. The "Server sees" line shows the values on the server.
 */
@Route(value = "uc17", layout = MainLayout.class)
@PageTitle("UC17 — Atomic reset")
@UseCaseDescription("Resetting several fields at once in the browser")
@Menu(order = 17, title = "UC17 — Atomic reset")
@StyleSheet("uc17.css")
public class AtomicResetView extends VerticalLayout {

    private final TextField name = new TextField("Name");
    private final TextField email = new TextField("Email");
    private final Checkbox subscribe = new Checkbox("Subscribe");
    private final Span server = new Span();

    public AtomicResetView() {
        addClassName("uc17-view");
        add(new H1("UC17 — Atomic reset"));
        add(new Paragraph("Edit the fields, then click Reset. One click "
                + "trigger clears all three fields in the browser, in the "
                + "same click, without a server round-trip. The line at the "
                + "bottom shows the values the server has, so you can check "
                + "that it learns about the reset too."));

        name.setId("name");
        name.setValue("Ada Lovelace");
        email.setId("email");
        email.setValue("ada@example.com");
        subscribe.setId("subscribe");
        subscribe.setValue(true);

        server.setId("server");
        server.addClassName("uc17-server");
        showServerValues();
        name.addValueChangeListener(e -> showServerValues());
        email.addValueChangeListener(e -> showServerValues());
        subscribe.addValueChangeListener(e -> showServerValues());

        Button reset = new Button("Reset");
        reset.setId("reset");

        new ClickTrigger(reset).triggers(
                new SetPropertyAction<>(name, "value", ""),
                new SetPropertyAction<>(email, "value", ""),
                new SetPropertyAction<>(subscribe, "checked", false),
                // Without these, the server would keep the old text.
                new FireChangeAction(name), new FireChangeAction(email));

        HorizontalLayout fields = new HorizontalLayout(name, email, subscribe);
        fields.addClassName("uc17-fields");
        add(fields, reset, server);
    }

    private void showServerValues() {
        server.setText("Server sees: name=\"" + name.getValue() + "\", email=\""
                + email.getValue() + "\", subscribe=" + subscribe.getValue());
    }
}
