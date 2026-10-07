package com.example.uc1;

import com.example.ShortcutTrigger;
import com.example.views.MainLayout;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.KeyModifier;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.trigger.internal.PropertyInput;
import com.vaadin.flow.component.trigger.internal.SetPropertyAction;
import com.vaadin.flow.component.trigger.internal.WriteToClipboardAction;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * UC1 — Ctrl+S writes a snapshot of the notes to the clipboard, fully
 * client-side.
 * <p>
 * One {@link ShortcutTrigger} fires two atomic actions in the original
 * user-gesture: {@link WriteToClipboardAction} reads the textarea's current
 * value via {@link PropertyInput} and calls {@code navigator.clipboard.write},
 * and a {@link SetPropertyAction} flips the status badge to "✓ Snapshot in
 * clipboard". Both run before the gesture's microtask queue drains; nothing
 * round-trips to the server.
 * <p>
 * The high-level alternative is Vaadin's {@code @Shortcut}, which can only
 * invoke a server-side method — and the clipboard API rejects writes that don't
 * happen inside the original user gesture, so a follow-up {@code executeJs}
 * from the server doesn't get to write at all.
 */
@Route(value = "uc1", layout = MainLayout.class)
@PageTitle("UC1 — Ctrl+S snapshot to clipboard")
@Menu(order = 1, title = "UC1 — Ctrl+S snapshot")
@StyleSheet("uc1.css")
public class ShortcutSaveView extends VerticalLayout {

    public ShortcutSaveView() {
        addClassName("uc1-view");
        add(new H1("UC1 — Ctrl+S writes a snapshot to the clipboard"));
        add(new Paragraph("Edit the notes and press Ctrl+S (or Cmd+S). One "
                + "ShortcutTrigger fires two pure client-side actions "
                + "atomically: WriteToClipboardAction copies the "
                + "current value, SetPropertyAction flips the badge. "
                + "preventDefault swallows the browser's Save Page "
                + "dialog. Nothing round-trips — paste anywhere to "
                + "confirm."));

        TextArea notes = new TextArea("Notes");
        notes.setId("notes");
        notes.addClassName("notes-field");
        notes.setValue("Draft notes — Ctrl+S backs them up to the clipboard.");
        notes.setWidthFull();
        notes.setMinHeight("9rem");

        Span status = new Span("(no snapshot yet)");
        status.setId("status");
        status.addClassName("status-badge");

        new ShortcutTrigger(this, Key.KEY_S, KeyModifier.CONTROL)
                .triggers(
                        new WriteToClipboardAction(new PropertyInput<>(notes,
                                "value", String.class), null),
                        new SetPropertyAction<>(status, "textContent",
                                "✓ Snapshot in clipboard"),
                        new SetPropertyAction<>(status, "className",
                                "status-badge copied"));

        add(notes, status);
    }
}
