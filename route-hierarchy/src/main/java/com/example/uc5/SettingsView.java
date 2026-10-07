package com.example.uc5;

import com.example.views.MainLayout;

import com.vaadin.flow.component.breadcrumbs.Breadcrumbs;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;

/**
 * UC5 — Up-one-level button (root, {@code uc5}).
 * <p>
 * Each page in this use case carries a single "↑ Up" control built from
 * {@code getRouteParent}. On this root page {@code getRouteParent} finds no
 * parent within the use case, so the control shows a "top level" note instead
 * of a link.
 */
@Route(value = "uc5", layout = MainLayout.class)
@PageTitle("Settings")
@Menu(order = 5, title = "UC5 — Up-one-level button")
public class SettingsView extends VerticalLayout {

    public SettingsView() {
        add(new Breadcrumbs());
        add(new H1("Settings"));
        add(new Paragraph(
                "The \"↑ Up\" control below uses getRouteParent(...), which needs "
                        + "only the immediate parent rather than the whole "
                        + "chain. Drill into Security, then Sessions, and use "
                        + "the control to climb back one level at a time."));
        add(new UpLink());
        add(new RouterLink("Security settings →", SecurityView.class));
    }
}
