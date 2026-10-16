package com.example.uc5;

import com.example.common.BuiltWith;
import com.example.home.HomeView;
import com.example.views.MainLayout;

import com.vaadin.flow.component.HasElement;
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
        add(new BuiltWith("A @RouteScope bean owned by the editor layout")
                .point("@RouteScope ties the draft to navigation. @RouteScopeOwner(CustomerEditorLayout.class) keeps it while this layout is part of the route.")
                .point("Both pages are routes inside the layout and get the same draft through their constructors.")
                .point("Leaving the editor removes the layout, and the draft with it. The next visit gets a new one.")
                .java("uc5/CustomerDraft", "scope")
                .java("uc5/CustomerDetailsView", "scope")
                .java("uc5/CustomerAddressView", "scope"));
    }

    @Override
    public void showRouterLayoutContent(HasElement content) {
        // The page goes above "How it is built".
        getElement().insertChild(getElement().getChildCount() - 1,
                content.getElement());
    }
}
