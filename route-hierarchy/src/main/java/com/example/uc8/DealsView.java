package com.example.uc8;

import com.example.common.UnreleasedApi;
import com.example.common.UseCaseDescription;
import com.example.uc1.CatalogView;
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
 * UC8 — a page the menu nests somewhere its route does not.
 * <p>
 * Deals lives at the short top-level URL {@code /deals}, so the route hierarchy
 * — and with it the breadcrumb above — puts it straight under Home. In the menu
 * it belongs with the catalog, and {@code @Menu(parent = CatalogView.class)}
 * says so: it moves the entry in
 * {@link com.vaadin.flow.server.menu.MenuConfiguration#getMenuEntriesTree()}
 * only, leaving navigation and the breadcrumb untouched.
 * <p>
 * That is the difference from UC2's {@code @RouteParent}, which relocates the
 * route itself and so moves the breadcrumb with it. Use {@code @RouteParent}
 * when the page really does live under another page; use
 * {@code @Menu(parent = ...)} when only the menu should group it that way.
 */
@UnreleasedApi("https://github.com/vaadin/flow/pull/24556")
@Route(value = "deals", layout = MainLayout.class)
@PageTitle("Deals")
@UseCaseDescription("A top-level page that the menu groups under Catalog")
@Menu(order = 15, title = "Deals", parent = CatalogView.class)
public class DealsView extends VerticalLayout {

    public DealsView() {
        add(new Breadcrumbs());
        add(new H1("Deals"));
        add(new Paragraph("The trail above reads Home › Deals: the URL is "
                + "/deals, so the route hierarchy has no Catalog in it. The "
                + "hierarchical menu still lists Deals under Catalog, because "
                + "@Menu(parent = CatalogView.class) regroups the menu entry "
                + "without moving the route."));
        add(new RouterLink("← Back to the hierarchical menu",
                HierarchicalMenuView.class));
    }
}
