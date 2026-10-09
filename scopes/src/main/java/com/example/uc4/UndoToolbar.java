package com.example.uc4;

import org.springframework.context.annotation.Scope;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.spring.annotation.SpringComponent;

/**
 * A toolbar that knows nothing about the editor; it only depends on the
 * UI-scoped {@link UndoHistory}. In a real application it would sit in the main
 * layout's navbar.
 */
@SpringComponent
@Scope("prototype")
public class UndoToolbar extends HorizontalLayout {

    public UndoToolbar(UndoHistory history) {
        Button undo = new Button("Undo", e -> history.undo());
        undo.bindEnabled(history.entries().map(list -> !list.isEmpty()));

        Span last = new Span();
        last.addClassName("hint");
        last.bindText(history.entries()
                .map(list -> list.isEmpty() ? "Nothing to undo"
                        : "Last change: " + list.getLast().description() + " ("
                                + list.size() + " in history)"));

        setDefaultVerticalComponentAlignment(Alignment.BASELINE);
        add(undo, last);
    }
}
