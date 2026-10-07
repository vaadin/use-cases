package com.example.uc3;

import com.example.views.MainLayout;

import com.vaadin.flow.component.breadcrumbs.Breadcrumbs;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.router.RouterLink;

/**
 * UC3 — Dynamic leaf label (parent, {@code uc3}).
 * <p>
 * The breadcrumb builder works from route classes and their parameters, never a
 * live view instance — yet the leaf crumb is still dynamic. The profile page
 * declares {@code @DynamicPageTitle(...)}; the breadcrumb resolves that
 * generator from the class and the {@code :userId} parameter, so the crumb
 * reads the person's name with no instance involved.
 */
@Route(value = "uc3", layout = MainLayout.class)
@PageTitle("Users")
@Menu(order = 3, title = "UC3 — Dynamic leaf label")
public class UsersView extends VerticalLayout {

    public UsersView() {
        add(new Breadcrumbs());
        add(new H1("Users"));
        add(new Paragraph(
                "Open a profile below. Its breadcrumb leaf is the person's "
                        + "name, resolved at runtime from the :userId by a "
                        + "PageTitleGenerator — no HasDynamicTitle, no view "
                        + "instance. The Users crumb is found by URL-prefix "
                        + "walking."));
        Directory.USERS.forEach((id, name) -> add(new RouterLink("View " + name,
                UserProfileView.class, new RouteParameters("userId", id))));
    }
}
