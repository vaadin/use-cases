package com.example.uc4;

import org.springframework.context.annotation.Scope;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.OrderedList;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.spring.annotation.SpringComponent;

/**
 * The full undo history, oldest change first. Like {@link UndoToolbar}, it only
 * depends on the UI-scoped {@link UndoHistory}, so it shows the same changes
 * that the toolbar undoes. In a real application it could be a side panel.
 */
@SpringComponent
@Scope("prototype")
public class UndoHistoryList extends Div {

    public UndoHistoryList(UndoHistory history) {
        Paragraph empty = new Paragraph("No changes yet in this UI.");
        empty.addClassName("hint");
        empty.bindVisible(history.entries().map(list -> list.isEmpty()));

        OrderedList list = new OrderedList();
        list.addClassName("undo-history");
        Signal.effect(list, () -> {
            list.removeAll();
            history.entries().get().forEach(
                    entry -> list.add(new ListItem(entry.description())));
        });

        add(new H2("Undo history"), empty, list);
    }
}
