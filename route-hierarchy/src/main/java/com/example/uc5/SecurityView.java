package com.example.uc5;

import com.example.views.MainLayout;

import com.vaadin.flow.component.breadcrumbs.Breadcrumbs;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;

/**
 * UC5 — Up-one-level button (mid, {@code uc5/security}).
 * <p>
 * {@code getRouteParent} strips the {@code /security} segment and resolves
 * {@code uc5} (Settings) as the immediate parent.
 */
@Route(value = "uc5/security", layout = MainLayout.class)
@PageTitle("Security")
public class SecurityView extends VerticalLayout {

    public SecurityView() {
        add(new Breadcrumbs());
        add(new H1("Security"));
        add(new Paragraph("The \"↑ Up\" control points at Settings — the "
                + "immediate parent resolved by getRouteParent(...)."));
        add(new UpLink());
        add(new RouterLink("Active sessions →", SessionsView.class));
    }
}
