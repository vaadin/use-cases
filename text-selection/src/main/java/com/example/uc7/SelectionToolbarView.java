package com.example.uc7;

import java.util.function.UnaryOperator;

import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.shared.SelectionRange;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.Signal;

/**
 * UC7 — Selection-driven transform toolbar.
 * <p>
 * The toolbar buttons combine the read and write sides of the API: enabled
 * state is bound to a signal derived from {@code selectionSignal()}, and each
 * action replaces the current selection in the value, then re-selects the
 * result so transforms can chain (UPPERCASE → "Quote" with one click each).
 */
@Route(value = "uc7", layout = MainLayout.class)
@PageTitle("UC7 — Selection-driven transform toolbar")
@UseCaseDescription("Enabling toolbar actions that transform the selection")
@Menu(order = 7, title = "UC7 — Selection toolbar")
public class SelectionToolbarView extends VerticalLayout {

    public SelectionToolbarView() {
        add(new H1("UC7 — Selection-driven transform toolbar"));
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
        editor.addClassName("uc-fixed-textarea");

        Signal<SelectionRange> selection = editor.selectionSignal();
        Signal<Boolean> hasSelection = selection.map(s -> !s.isEmpty());

        Button upper = new Button("UPPERCASE",
                e -> transform(editor, String::toUpperCase));
        Button lower = new Button("lowercase",
                e -> transform(editor, String::toLowerCase));
        Button quote = new Button("\"Quote\"",
                e -> transform(editor, s -> "\"" + s + "\""));
        Button trim = new Button("Trim", e -> transform(editor, String::strip));

        Signal.effect(this, () -> {
            boolean enabled = hasSelection.get();
            upper.setEnabled(enabled);
            lower.setEnabled(enabled);
            quote.setEnabled(enabled);
            trim.setEnabled(enabled);
        });

        HorizontalLayout toolbar = new HorizontalLayout(upper, lower, quote,
                trim);
        toolbar.addClassName("uc-toolbar");

        add(toolbar, editor);
    }

    private static void transform(TextArea editor, UnaryOperator<String> fn) {
        SelectionRange sel = editor.selectionSignal().peek();
        if (sel.isEmpty()) {
            return;
        }
        String value = editor.getValue() == null ? "" : editor.getValue();
        String replaced = fn.apply(sel.content());
        editor.setValue(value.substring(0, sel.start()) + replaced
                + value.substring(sel.end()));
        editor.setSelectionRange(sel.start(), sel.start() + replaced.length());
    }
}
