package com.example.uc8;

import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;

import com.vaadin.flow.component.breadcrumbs.Breadcrumbs;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.menu.MenuConfiguration;
import com.vaadin.flow.server.menu.MenuEntry;

/**
 * UC8 — Hierarchical menu.
 * <p>
 * The main navigation (in {@code BaseMainLayout}) lists only the top-level menu
 * items — every use case, flat. This view renders the <em>same</em> menu
 * entries as a nested tree by calling
 * {@link MenuConfiguration#getMenuEntriesTree()} (flow#24556), which groups the
 * {@code @Menu} entries by route hierarchy and hands back each
 * {@link MenuEntry} with its {@link MenuEntry#children()} populated. So the
 * param-free nested use cases — Catalog › Electronics › Laptops, Settings ›
 * Security › Active sessions, Dashboard › Team — show their structure here.
 * <p>
 * One entry is nested by the menu alone: {@link DealsView} is a top-level route
 * at {@code /deals}, but {@code @Menu(parent = CatalogView.class)} places it
 * under Catalog in this tree while its breadcrumb stays Home › Deals.
 * <p>
 * Like the breadcrumb's walker ({@code RouteConfiguration.getRouteHierarchy}),
 * {@code getMenuEntriesTree()} is public, supported API — here in
 * {@code com.vaadin.flow.server.menu}.
 */
@Route(value = "uc8", layout = MainLayout.class)
@PageTitle("Hierarchical menu")
@UseCaseDescription("Showing the menu as a tree that follows the route hierarchy")
@Menu(order = 8, title = "UC8 — Hierarchical menu")
public class HierarchicalMenuView extends VerticalLayout {

    public static final String MENU_ID = "uc8-menu-tree";

    public HierarchicalMenuView() {
        add(new Breadcrumbs());
        add(new H1("Hierarchical menu"));
        add(new Paragraph(
                "The drawer on the left lists every use case flat — only "
                        + "top-level menu items. The tree below is the same "
                        + "@Menu set returned by MenuConfiguration."
                        + "getMenuEntriesTree(), nested by route hierarchy: open "
                        + "Catalog, Settings or Dashboard to see their child "
                        + "routes that the flat navigation hides. Deals sits "
                        + "under Catalog too, but only in the menu: its URL is "
                        + "/deals, and @Menu(parent = CatalogView.class) "
                        + "groups it there without moving the route — open it "
                        + "and its breadcrumb reads Home › Deals."));

        SideNav tree = new SideNav();
        tree.setId(MENU_ID);
        MenuConfiguration.getMenuEntriesTree()
                .forEach(entry -> tree.addItem(toItem(entry)));
        add(tree);
    }

    /**
     * Renders a menu entry and its children as a nested {@link SideNavItem}; an
     * entry without a path (a menu-only grouping) becomes a plain label.
     */
    public static SideNavItem toItem(MenuEntry entry) {
        SideNavItem item = entry.path() != null
                ? new SideNavItem(entry.title(), entry.path())
                : new SideNavItem(entry.title());
        entry.children().forEach(child -> item.addItem(toItem(child)));
        return item;
    }
}
