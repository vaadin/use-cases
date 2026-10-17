package com.example.uc13;

import java.util.List;

import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.trigger.internal.DomEventTrigger;
import com.vaadin.flow.component.trigger.internal.PropertyInput;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * UC13 — Client-side filter.
 * <p>
 * The full list comes from the server (loaded once at view construction). A
 * {@link DomEventTrigger} on the search field's {@code input} event fires a
 * {@link FilterListAction} that hides non-matching rows entirely in JS. No
 * server round-trip on each keystroke; the server only sees the data once.
 * <p>
 * Compare with UC18, where every keystroke goes to a signal on the server. Here
 * the goal is the opposite: keep the server out of the loop.
 */
@Route(value = "uc13", layout = MainLayout.class)
@PageTitle("UC13 — Client-side filter")
@UseCaseDescription("Filtering a server-loaded list in the browser on every keystroke")
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
        add(new Paragraph("The server sends the list below once. Type in "
                + "the search field: a custom FilterListAction hides the "
                + "rows that do not match, in the browser, without a "
                + "round-trip on each keystroke."));

        TextField search = new TextField();
        search.setId("search");
        search.setPlaceholder("Filter…");
        search.setClearButtonVisible(true);
        search.addClassName("uc13-search");

        Div list = new Div();
        list.setId("list");
        list.addClassName("uc13-list");
        for (String item : ITEMS) {
            Div row = new Div(item);
            row.addClassName("uc13-row");
            list.add(row);
        }

        new DomEventTrigger(search, "input").triggers(new FilterListAction(list,
                new PropertyInput<>(search, "value", String.class)));

        add(search, list);
    }
}
