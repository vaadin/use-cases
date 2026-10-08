package com.example.home;

import java.util.List;

import com.example.common.BaseHomeView;
import com.example.uc1.CatalogView;
import com.example.uc2.OrdersView;
import com.example.uc3.UsersView;
import com.example.uc4.ProjectsView;
import com.example.uc5.SettingsView;
import com.example.uc6.DashboardView;
import com.example.uc7.SitemapView;
import com.example.views.MainLayout;

import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.Route;

@Route(value = "", layout = MainLayout.class)
@Menu(order = 0, title = "Home")
public class HomeView extends BaseHomeView {

    public HomeView() {
        super("Route Hierarchy — use cases",
                "The Breadcrumbs component (new in 25.2) renders a trail in its "
                        + "default ROUTER mode: on every navigation it walks the "
                        + "route hierarchy — @RouteParent first, URL-prefix "
                        + "matching as the fallback — and labels each crumb with "
                        + "the route's page title, including instance-free "
                        + "PageTitleGenerators. The view just does "
                        + "add(new Breadcrumbs()); there is no per-view plumbing. "
                        + "UC5 and UC7 build other consumers — an up-link and a "
                        + "sitemap — directly on the same route-hierarchy API.");
        addGroup("Finding a page's parents", VaadinIcon.SITEMAP, Accent.BLUE,
                List.of(CatalogView.class, OrdersView.class));
        addGroup("Getting each crumb right", VaadinIcon.LINK, Accent.GREEN,
                List.of(UsersView.class, ProjectsView.class));
        addGroup("Navigation beyond one view", VaadinIcon.COMPASS,
                Accent.PURPLE, List.of(SettingsView.class, DashboardView.class,
                        SitemapView.class));
    }
}
