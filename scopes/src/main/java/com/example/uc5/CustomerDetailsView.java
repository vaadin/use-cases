package com.example.uc5;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * UC5 — Customer draft shared by the pages of a multi-page editor (route
 * scope).
 * <p>
 * A sales assistant registers a new customer. Contact details and address are
 * on separate pages with their own URLs, so the assistant can move back and
 * forth between them. Whatever they have typed must be kept while they are in
 * the editor, and must be thrown away once they leave it — the next customer
 * starts from a blank form. Session or UI scope would leak the old draft into
 * the next registration; {@code @RouteScope} with
 * {@code @RouteScopeOwner(CustomerEditorLayout.class)} matches the lifetime of
 * the editor exactly.
 */
@Route(value = "uc5", layout = CustomerEditorLayout.class)
@PageTitle("UC5 — Customer draft")
@Menu(order = 5, title = "UC5 — Customer draft")
public class CustomerDetailsView extends VerticalLayout {

    public CustomerDetailsView(CustomerDraft draft) {
        setPadding(false);
        Span badge = new Span(draft.describe());
        badge.addClassName("scope-badge");
        add(new H2("Contact details"), badge);

        TextField name = new TextField("Name", draft.getName(), "");
        name.addValueChangeListener(e -> draft.setName(e.getValue()));
        EmailField email = new EmailField("Email");
        email.setValue(draft.getEmail());
        email.addValueChangeListener(e -> draft.setEmail(e.getValue()));
        add(name, email);
    }
}
