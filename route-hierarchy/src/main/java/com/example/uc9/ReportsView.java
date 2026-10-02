package com.example.uc9;

import java.util.List;
import java.util.Optional;

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
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.menu.MenuConfiguration;
import com.vaadin.flow.server.menu.MenuEntry;

/**
 * UC9 — Menu-only nesting (root, {@code uc9}).
 * <p>
 * Every other use case here has one hierarchy: the route hierarchy nests the
 * breadcrumb <em>and</em> the menu. This one splits them. {@link
 * RevenueForecastView} is a top-level route at {@code /forecast} — its URL sits
 * nowhere near {@code uc9}, and neither does its breadcrumb — but in the menu
 * it belongs under Reports. {@code @Menu(parent = ReportsView.class)} says so,
 * and it moves the entry in {@link MenuConfiguration#getMenuEntriesTree()}
 * only: navigation, the breadcrumb trail and the flat
 * {@link MenuConfiguration#getMenuEntries()} are all untouched.
 * <p>
 * That is the difference from UC2's {@code @RouteParent}, which relocates the
 * route itself and so moves the breadcrumb with it. Use {@code @RouteParent}
 * when the page really does live under another page; use
 * {@code @Menu(parent = ...)} when only the navigation menu should group it
 * that way.
 * <p>
 * {@link SalesReportView} is the control: a plain route child of {@code uc9}
 * that needs no annotation, because for it the two hierarchies already agree.
 */
@Route(value = "uc9", layout = MainLayout.class)
@PageTitle("Reports")
@Menu(order = 9, title = "UC9 — Menu-only nesting")
public class ReportsView extends VerticalLayout {

    public static final String MENU_ID = "uc9-menu-subtree";

    public ReportsView() {
        add(new Breadcrumbs());
        add(new H1("Reports"));
        add(new Paragraph(
                "The tree below is this entry's own branch of "
                        + "getMenuEntriesTree(). It holds both reports, even "
                        + "though only one of them is a route child of /uc9: "
                        + "Revenue forecast lives at /forecast and is pulled in "
                        + "here by @Menu(parent = ReportsView.class). Open it "
                        + "and its breadcrumb will not mention Reports at all — "
                        + "the menu was regrouped, the route hierarchy was "
                        + "not."));

        SideNav subtree = new SideNav();
        subtree.setId(MENU_ID);
        findEntry(MenuConfiguration.getMenuEntriesTree(), ReportsView.class)
                .ifPresent(reports -> reports.children()
                        .forEach(child -> subtree.addItem(toItem(child))));
        add(subtree);

        add(new RouterLink("Sales report →", SalesReportView.class));
        add(new RouterLink("Revenue forecast →", RevenueForecastView.class));
    }

    private static Optional<MenuEntry> findEntry(List<MenuEntry> entries,
            Class<?> menuClass) {
        for (MenuEntry entry : entries) {
            if (menuClass.equals(entry.menuClass())) {
                return Optional.of(entry);
            }
            Optional<MenuEntry> nested = findEntry(entry.children(), menuClass);
            if (nested.isPresent()) {
                return nested;
            }
        }
        return Optional.empty();
    }

    private static SideNavItem toItem(MenuEntry entry) {
        SideNavItem item = new SideNavItem(entry.title(), entry.path());
        entry.children().forEach(child -> item.addItem(toItem(child)));
        return item;
    }
}
