package com.example.uc13;

import java.util.List;

import com.example.views.MainLayout;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.trigger.internal.DomEventTrigger;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * UC13 — Search field + server-loaded list, filtered client-side.
 * <p>
 * The full list comes from the server (loaded once at view construction). A
 * {@link DomEventTrigger} on the search field's {@code input} event fires a
 * {@link FilterListAction} that hides non-matching rows entirely in JS. No
 * server round-trip on each keystroke; the server only sees the data once.
 * <p>
 * Contrast with {@code SetSignalAction} (UC3) — there the keystrokes would push
 * to a signal and the server would react; here the goal is the opposite, keep
 * the server out of the loop.
 */
@Route(value = "uc13", layout = MainLayout.class)
@PageTitle("UC13 — Client-side filter")
@Menu(order = 13, title = "UC13 — Client-side filter")
@StyleSheet("uc13.css")
public class ClientFilterView extends VerticalLayout {

    private static final List<String> ITEMS = List.of("Apricot", "Banana",
            "Blueberry", "Cherry", "Cranberry", "Date", "Elderberry", "Fig",
            "Grape", "Grapefruit", "Honeydew", "Kiwi", "Lemon", "Lime", "Mango",
            "Nectarine", "Orange", "Papaya", "Peach", "Pear", "Persimmon",
            "Pineapple", "Plum", "Pomegranate", "Quince", "Raspberry",
            "Strawberry", "Tangerine", "Watermelon");

    public ClientFilterView() {
        addClassName("uc13-view");
        add(new H1("UC13 — Client-side filter"));
        add(new Paragraph(
                "The list below is rendered once from the server. Typing in "
                        + "the search field fires a custom FilterListAction "
                        + "that hides non-matching rows purely in JS — no "
                        + "round-trip on each keystroke."));

        TextField search = new TextField();
        search.setId("search");
        search.setPlaceholder("Filter…");
        search.setClearButtonVisible(true);
        search.addClassName("search-field");

        Div list = new Div();
        list.setId("list");
        list.addClassName("filter-list");
        for (String item : ITEMS) {
            Div row = new Div(item);
            row.addClassName("row");
            list.add(row);
        }

        new DomEventTrigger(search, "input")
                .triggers(new FilterListAction(list));

        add(search, list);
    }
}
