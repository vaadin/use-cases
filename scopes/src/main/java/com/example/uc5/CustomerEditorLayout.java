package com.example.uc5;

import com.example.home.HomeView;
import com.example.views.MainLayout;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.ParentLayout;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.router.RouterLink;

/**
 * Frame of the multi-page "new customer" editor. It owns the route-scoped
 * {@link CustomerDraft}: as long as this layout is on screen, the pages inside
 * it share one draft.
 */
@ParentLayout(MainLayout.class)
public class CustomerEditorLayout extends Div implements RouterLayout {

    public CustomerEditorLayout() {
        getStyle().set("padding", "var(--vaadin-padding-m)");
        add(new H1("UC5 — Customer draft across pages"));
        add(new Paragraph("The two pages below share a @RouteScope bean "
                + "owned by this editor layout. Switch between them and the "
                + "draft stays. Leave the editor and come back, and you get "
                + "a new, empty draft."));
        add(new HorizontalLayout(
                new RouterLink("1. Contact details", CustomerDetailsView.class),
                new RouterLink("2. Address", CustomerAddressView.class),
                new RouterLink("Leave the editor", HomeView.class)));
    }
}
