package com.example.uc5;

import com.example.MissingAPI;
import com.example.MissingAPI.TextSelection;
import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.Signal;

/**
 * UC5 — Insert template at cursor.
 * <p>
 * The classic snippet-insert pattern: read where the cursor is, splice the
 * snippet in — replacing any selected text — then either move the cursor past
 * the snippet or select its placeholder so the user can type over it. Moving
 * the cursor and selecting use {@code setCursorPosition} and
 * {@code setSelectionRange}. Reading the cursor has no API yet
 * ({@code API-GAPS.md} #1), so the view reads it from
 * {@link MissingAPI#selectionSignal}.
 */
@Route(value = "uc5", layout = MainLayout.class)
@PageTitle("UC5 — Insert template at cursor")
@UseCaseDescription("Inserting a snippet at the cursor or over a selection")
@Menu(order = 5, title = "UC5 — Insert template at cursor")
public class InsertTemplateView extends VerticalLayout {

    public InsertTemplateView() {
        add(new H1("UC5 — Insert template at cursor"));
        add(new Paragraph(
                "Click somewhere in the message body — or select a run of "
                        + "text to replace — then press a snippet button. The "
                        + "snippet is inserted at the cursor, or replaces the "
                        + "selection. If the snippet contains a placeholder "
                        + "(e.g. {name}), that placeholder is left selected "
                        + "so the next keystroke replaces it."));

        TextArea editor = new TextArea("Message");
        editor.setWidthFull();
        editor.setHeight("220px");
        editor.setValue("Hi,\n\n\n\nThanks,\n");
        editor.addClassName("uc-textarea");

        // Start following the selection now, so that the first click into the
        // text area is already reported when a snippet button is pressed.
        Signal<TextSelection> selection = MissingAPI.selectionSignal(editor);

        Button greeting = new Button("Greeting",
                e -> insert(editor, selection, "Hello {name}!", "{name}"));
        Button signature = new Button("Signature",
                e -> insert(editor, selection, "Best regards,\nJamie", ""));
        Button placeholder = new Button("Placeholder",
                e -> insert(editor, selection, "{TODO}", "{TODO}"));

        HorizontalLayout toolbar = new HorizontalLayout(greeting, signature,
                placeholder);
        toolbar.addClassName("uc-toolbar");

        add(toolbar, editor);
    }

    private static void insert(TextArea editor, Signal<TextSelection> selection,
            String snippet, String placeholder) {
        TextSelection sel = selection.peek();
        String value = editor.getValue() == null ? "" : editor.getValue();
        int start = Math.min(sel.start(), value.length());
        int end = Math.min(sel.end(), value.length());
        String newValue = value.substring(0, start) + snippet
                + value.substring(end);
        editor.setValue(newValue);

        if (!placeholder.isEmpty()) {
            int phStart = snippet.indexOf(placeholder);
            if (phStart >= 0) {
                editor.setSelectionRange(start + phStart,
                        start + phStart + placeholder.length());
                return;
            }
        }
        editor.setCursorPosition(start + snippet.length());
    }
}
