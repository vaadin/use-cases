package com.example.uc7;

import java.util.function.UnaryOperator;

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
 * UC7 — Selection toolbar.
 * <p>
 * The toolbar combines reading and writing the selection: the buttons are
 * enabled with {@code bindEnabled} only while text is selected, and each action
 * replaces the selected text, then selects the result with
 * {@code setSelectionRange} so actions can be chained (UPPERCASE, then "Quote",
 * one click each). {@code HasTextSelection} has no way to read the selection
 * yet ({@code API-GAPS.md} #1); the signal comes from
 * {@link MissingAPI#selectionSignal}.
 */
@Route(value = "uc7", layout = MainLayout.class)
@PageTitle("UC7 — Selection toolbar")
@UseCaseDescription("Enabling toolbar actions that transform the selection")
@Menu(order = 7, title = "UC7 — Selection toolbar")
public class SelectionToolbarView extends VerticalLayout {

    public SelectionToolbarView() {
        add(new H1("UC7 — Selection toolbar"));
        add(new Paragraph(
                "Select some text in the editor below. The toolbar buttons "
                        + "are enabled only while a selection exists, and "
                        + "they replace the selection in place. The "
                        + "transformed text stays selected, so you can chain "
                        + "actions."));

        TextArea editor = new TextArea();
        editor.setValue(
                "Click and drag to select some text, then transform it.");
        editor.setWidthFull();
        editor.setHeight("220px");
        editor.addClassName("uc-textarea");

        Signal<TextSelection> selection = MissingAPI.selectionSignal(editor);
        Signal<Boolean> hasSelection = selection.map(s -> !s.isEmpty());

        Button upper = new Button("UPPERCASE",
                e -> transform(editor, selection, String::toUpperCase));
        Button lower = new Button("lowercase",
                e -> transform(editor, selection, String::toLowerCase));
        Button quote = new Button("\"Quote\"",
                e -> transform(editor, selection, s -> "\"" + s + "\""));
        Button trim = new Button("Trim",
                e -> transform(editor, selection, String::strip));
        for (Button button : new Button[] { upper, lower, quote, trim }) {
            button.bindEnabled(hasSelection);
        }

        HorizontalLayout toolbar = new HorizontalLayout(upper, lower, quote,
                trim);
        toolbar.addClassName("uc-toolbar");

        add(toolbar, editor);
    }

    private static void transform(TextArea editor,
            Signal<TextSelection> selection, UnaryOperator<String> fn) {
        TextSelection sel = selection.peek();
        String value = editor.getValue() == null ? "" : editor.getValue();
        // The range can be ahead of the value the server has, so it is
        // clamped to the value before it is used.
        int start = Math.min(sel.start(), value.length());
        int end = Math.min(sel.end(), value.length());
        if (start == end) {
            return;
        }
        String replaced = fn.apply(value.substring(start, end));
        editor.setValue(
                value.substring(0, start) + replaced + value.substring(end));
        editor.setSelectionRange(start, start + replaced.length());
    }
}
