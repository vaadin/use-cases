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
 * UC2 — Select after formatting.
 * <p>
 * The Format button rewrites the value on the server, then calls
 * {@code selectAll()}, which returns focus to the field and selects the new
 * value. The user can Tab to accept it, type to replace it, or click into it to
 * place the cursor for a small fix. {@code setAutoselect(true)} can not do
 * this: it selects on every focus, so the click would select everything again.
 */
@Route(value = "uc2", layout = MainLayout.class)
@PageTitle("UC2 — Select after formatting")
@UseCaseDescription("Selecting a value after the server reformats it")
@Menu(order = 2, title = "UC2 — Select after formatting")
public class FormatAndSelectView extends VerticalLayout {

    private static final Pattern NON_SLUG = Pattern.compile("[^a-z0-9]+");

    public FormatAndSelectView() {
        add(new H1("UC2 — Select after formatting"));
        add(new Paragraph(
                "Type a title and press \"Format\". The server turns the "
                        + "value into a slug and calls selectAll(), which "
                        + "focuses the field and selects the new value. Press "
                        + "Tab to accept it or type to replace it. Or click "
                        + "inside it to place the cursor: the field has no "
                        + "autoselect, so the click is not undone. Compare "
                        + "with UC1, where every focus selects the value."));

        TextField title = new TextField("Title");
        title.setValue("Hello, Awesome World!");
        title.setWidth("360px");
        title.setMaxWidth("100%");

        Button format = new Button("Format", e -> {
            String value = title.getValue() == null ? "" : title.getValue();
            String slug = NON_SLUG.matcher(value.toLowerCase()).replaceAll("-")
                    .replaceAll("^-|-$", "");
            title.setValue(slug);
            title.selectAll();
        });

        HorizontalLayout row = new HorizontalLayout(title, format);
        row.setAlignItems(Alignment.END);
        row.addClassName("uc-toolbar");
        add(row);
    }
}
