package com.example.home;

import com.example.common.BaseHomeView;
import com.example.uc1.CatalogView;
import com.example.uc2.OrdersView;
import com.example.uc3.UsersView;
import com.example.uc4.ProjectsView;
import com.example.uc5.SettingsView;
import com.example.uc6.DashboardView;
import com.example.uc7.SitemapView;
import com.example.uc8.HierarchicalMenuView;
import com.example.uc9.ReportsView;
import com.example.views.MainLayout;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route(value = "", layout = MainLayout.class)
@PageTitle("Home")
@Menu(order = 0, title = "Home")
public class HomeView extends BaseHomeView {

    public HomeView() {
        super("Route Hierarchy — use cases",
                "Each card below shows the Breadcrumbs component (new in 25.2) "
                        + "rendering a trail in its default ROUTER mode: on every "
                        + "navigation it walks the route hierarchy — @RouteParent "
                        + "first, URL-prefix matching as the fallback — and labels "
                        + "each crumb with the route's page title, including "
                        + "instance-free PageTitleGenerators. The view just does "
                        + "add(new Breadcrumbs()); there is no per-view plumbing. "
                        + "UC5 and UC7 build other consumers — an up-link and a "
                        + "sitemap — directly on the same route-hierarchy API.");

        Div cards = new Div();
        cards.addClassName("home-cards");
        cards.add(homeCard("UC1", "URL-prefix trail",
                "Nested routes whose URLs nest — ancestors found with zero "
                        + "annotations.",
                CatalogView.class));
        cards.add(homeCard("UC2", "@RouteParent override",
                "A detail page whose URL is not a prefix of its conceptual "
                        + "parent.",
                OrdersView.class));
        cards.add(homeCard("UC3", "Dynamic leaf label",
                "The current crumb shows a runtime title via a "
                        + "PageTitleGenerator — no view instance needed.",
                UsersView.class));
        cards.add(homeCard("UC4", "Parameter-preserving links",
                "Ancestor links carry the :projectId through the whole trail.",
                ProjectsView.class));
        cards.add(homeCard("UC5", "Up-one-level button",
                "A single \"↑ Up\" link built from getRouteParent(...).",
                SettingsView.class));
        cards.add(homeCard("UC6", "Layout-wide auto breadcrumbs",
                "One breadcrumb bar in a parent layout, rebuilt on every "
                        + "navigation.",
                DashboardView.class));
        cards.add(homeCard("UC7", "Route-tree sitemap",
                "getRouteHierarchy used as a graph-builder to render the whole "
                        + "hierarchy.",
                SitemapView.class));
        cards.add(homeCard("UC8", "Hierarchical menu",
                "MenuConfiguration.getMenuEntriesTree() nests the @Menu entries "
                        + "the flat nav lists.",
                HierarchicalMenuView.class));
        cards.add(homeCard("UC9", "Menu-only nesting",
                "@Menu(parent = ...) groups a top-level route under another "
                        + "entry in the menu without moving the route.",
                ReportsView.class));
        add(cards);
    }
}
