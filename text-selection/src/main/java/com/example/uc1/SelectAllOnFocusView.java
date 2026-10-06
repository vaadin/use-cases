package com.example.uc1;

import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * UC1 — Select all on focus.
 * <p>
 * Each time the user focuses the field, the current value is selected so that
 * typing immediately overwrites it. This is the canonical select-all-on-focus
 * pattern — it doesn't need the new {@code HasTextSelection} API at all, the
 * existing {@code setAutoselect(true)} attribute on {@code TextField} handles
 * it. Included here as the baseline before the use cases that actually need
 * server-driven selection control.
 */
@Route(value = "uc1", layout = MainLayout.class)
@PageTitle("UC1 — Select all on focus")
@UseCaseDescription("Selecting a field's value when it gets focus")
@Menu(order = 1, title = "UC1 — Select all on focus")
public class SelectAllOnFocusView extends VerticalLayout {

    public SelectAllOnFocusView() {
        add(new H1("UC1 — Select all on focus"));
        add(new Paragraph(
                "Click into the quantity field. The current value is selected, "
                        + "so typing replaces it without first deleting. "
                        + "Tab away and back: the value re-selects every time."));

        TextField quantity = new TextField("Quantity");
        quantity.setValue("10");
        quantity.setAutoselect(true);
        quantity.setHelperText("Type to overwrite — no need to clear first");

        add(quantity);
    }
}
