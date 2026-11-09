package com.example.uc1;

import com.example.ShortcutTrigger;
import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.KeyModifier;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.trigger.internal.Action;
import com.vaadin.flow.component.trigger.internal.PropertyInput;
import com.vaadin.flow.component.trigger.internal.SetPropertyAction;
import com.vaadin.flow.component.trigger.internal.WriteToClipboardAction;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * UC1 — Ctrl+S snapshot.
 * <p>
 * Ctrl+S (Cmd+S on a Mac) copies the notes to the clipboard, without a server
 * round-trip. One {@link ShortcutTrigger} per modifier runs the same three
 * actions inside the key press: {@link WriteToClipboardAction} reads the text
 * area's current value through a {@link PropertyInput} and writes it to the
 * clipboard, and two {@link SetPropertyAction}s change the status badge. The
 * trigger also calls {@code preventDefault()}, so the browser's "Save Page"
 * dialog does not open.
 * <p>
 * A server-side shortcut ({@code Shortcuts.addShortcutListener}) could only
 * write to the clipboard after a round-trip, outside the key press, and
 * browsers such as Safari and Firefox reject a clipboard write that does not
 * happen during a user gesture.
 */
@Route(value = "uc1", layout = MainLayout.class)
@PageTitle("UC1 — Ctrl+S snapshot")
@UseCaseDescription("Saving a snapshot to the clipboard with Ctrl+S, fully client-side")
@Menu(order = 1, title = "UC1 — Ctrl+S snapshot")
@StyleSheet("uc1.css")
public class ShortcutSaveView extends VerticalLayout {

    public ShortcutSaveView() {
        addClassName("uc1-view");
        add(new H1("UC1 — Ctrl+S snapshot"));
        add(new Paragraph("Edit the notes and press Ctrl+S (Cmd+S on a Mac). "
                + "The notes are copied to the clipboard and the badge "
                + "changes, all in the browser: nothing goes to the server. "
                + "The browser's \"Save Page\" dialog does not open. Paste "
                + "anywhere to check."));

        TextArea notes = new TextArea("Notes");
        notes.setId("notes");
        notes.addClassName("uc1-notes");
        notes.setValue("Draft notes — Ctrl+S copies them to the clipboard.");
        notes.setWidthFull();
        notes.setMinHeight("9rem");

        Span status = new Span("(no snapshot yet)");
        status.setId("status");
        status.addClassName("uc1-status");

        Action[] snapshot = { new WriteToClipboardAction(
                new PropertyInput<>(notes, "value", String.class), null),
                new SetPropertyAction<>(status, "textContent",
                        "✓ Snapshot in clipboard"),
                new SetPropertyAction<>(status, "className",
                        "uc1-status uc1-copied") };
        // The modifiers must match exactly, so Ctrl+S and Cmd+S each need
        // their own trigger.
        new ShortcutTrigger(this, Key.KEY_S, KeyModifier.CONTROL)
                .triggers(snapshot);
        new ShortcutTrigger(this, Key.KEY_S, KeyModifier.META)
                .triggers(snapshot);

        add(notes, status);
    }
}
