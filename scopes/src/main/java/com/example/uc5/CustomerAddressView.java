package com.example.uc5;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * Second page of the UC5 editor. It receives the same {@link CustomerDraft}
 * instance as {@link CustomerDetailsView} because both are inside
 * {@link CustomerEditorLayout}.
 */
@Route(value = "uc5/address", layout = CustomerEditorLayout.class)
@PageTitle("UC5 — Customer draft")
public class CustomerAddressView extends VerticalLayout {

    public CustomerAddressView(CustomerDraft draft) {
        setPadding(false);
        Span badge = new Span(draft.describe());
        badge.addClassName("scope-badge");
        add(new H2("Address"), badge);

        String customer = draft.getName().isBlank() ? "the new customer"
                : draft.getName();
        add(new Span("Where should we ship orders for " + customer + "?"));

        TextField street = new TextField("Street", draft.getStreet(), "");
        street.addValueChangeListener(e -> draft.setStreet(e.getValue()));
        TextField city = new TextField("City", draft.getCity(), "");
        city.addValueChangeListener(e -> draft.setCity(e.getValue()));
        add(street, city);
    }
}
