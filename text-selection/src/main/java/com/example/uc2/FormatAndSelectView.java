package com.example.uc2;

import java.util.regex.Pattern;

import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * UC2 — Post-transform select-all.
 * <p>
 * A one-shot, server-driven {@code selectAll()} after a server-side value
 * transform. The Format button steals focus, the server rewrites the value, and
 * {@code selectAll()} atomically returns focus and selects the new value. The
 * user can Tab to accept, type to replace, or click into the field to position
 * the cursor for a single-character fix — a UX that {@code setAutoselect(true)}
 * cannot deliver because it would re-select on every subsequent focus.
 */
@Route(value = "uc2", layout = MainLayout.class)
@PageTitle("UC2 — Post-transform select-all")
@UseCaseDescription("Selecting a value after the server reformats it")
@Menu(order = 2, title = "UC2 — Post-transform select-all")
public class FormatAndSelectView extends VerticalLayout {

    private static final Pattern NON_SLUG = Pattern.compile("[^a-z0-9]+");

    public FormatAndSelectView() {
        add(new H1("UC2 — Post-transform select-all"));
        add(new Paragraph(
                "Type a title and press \"Format\". The server slugifies the "
                        + "value and calls selectAll() so you can Tab to "
                        + "accept or start typing to replace. Click anywhere "
                        + "inside the formatted value to position the cursor "
                        + "— your click is preserved because the field does "
                        + "not have autoselect enabled. This UX cannot be "
                        + "achieved with setAutoselect(true): the Format "
                        + "button steals focus, the server rewrites the "
                        + "value, and selectAll() atomically returns focus + "
                        + "selects. Compare with UC1, where every click "
                        + "re-selects."));

        TextField title = new TextField("Title");
        title.setValue("Hello, Awesome World!");
        title.setWidth("360px");

        Button format = new Button("Format", e -> {
            String value = title.getValue() == null ? "" : title.getValue();
            String slug = NON_SLUG.matcher(value.toLowerCase()).replaceAll("-")
                    .replaceAll("^-|-$", "");
            title.setValue(slug);
            title.selectAll();
        });

        HorizontalLayout row = new HorizontalLayout(title, format);
        row.setAlignItems(Alignment.END);
        add(row);
    }
}
