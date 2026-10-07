package com.example.uc5;

import com.example.views.MainLayout;

import com.vaadin.flow.component.breadcrumbs.Breadcrumbs;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * UC5 — Up-one-level button (leaf, {@code uc5/security/sessions}).
 * <p>
 * {@code getRouteParent} resolves {@code uc5/security} (Security) as the
 * immediate parent — one level up, not all the way to the root.
 */
@Route(value = "uc5/security/sessions", layout = MainLayout.class)
@PageTitle("Active sessions")
public class SessionsView extends VerticalLayout {

    public SessionsView() {
        add(new Breadcrumbs());
        add(new H1("Active sessions"));
        add(new Paragraph("The \"↑ Up\" control resolves to Security — the "
                + "immediate parent — even though the full breadcrumb above "
                + "spans three levels."));
        add(new UpLink());
    }
}
