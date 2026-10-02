package com.example.uc9;

import java.util.List;
import java.util.Optional;

import com.example.home.HomeView;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.breadcrumbs.Breadcrumbs;
import com.vaadin.flow.component.breadcrumbs.BreadcrumbsTester;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.server.menu.MenuConfiguration;
import com.vaadin.flow.server.menu.MenuEntry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UC9 asserts that {@code @Menu(parent = ...)} moves an entry in the menu tree
 * and nowhere else: {@link RevenueForecastView} nests under Reports in the menu
 * while its route hierarchy — and so its breadcrumb — keeps it at the top
 * level.
 * <p>
 * The landing page is registered alongside the use case so the menu tree has
 * the same shape as the running app, with Home at the root and the use cases
 * beneath it. Every UC9 assertion lives in this one class on purpose: two test
 * classes in the same package with different {@code @ViewPackages} leak their
 * registrations into each other, so a second class listing other views would
 * silently change the routes this one sees.
 */
@SpringBootTest
@ViewPackages(classes = { HomeView.class, ReportsView.class })
class MenuOnlyNestingTest extends SpringBrowserlessTest {

    @Test
    void rootRendersItsOwnMenuBranch() {
        navigate(ReportsView.class);

        assertTrue(findInView(H1.class).all().stream()
                .anyMatch(h -> "Reports".equals(h.getText())));

        SideNav subtree = find(SideNav.class).withId(ReportsView.MENU_ID)
                .single();
        assertEquals(List.of("Sales report", "Revenue forecast"),
                subtree.getItems().stream().map(SideNavItem::getLabel).toList(),
                "both reports belong to the Reports branch of the menu tree");
    }

    @Test
    void routeChildNestsInBothHierarchies() {
        navigate(SalesReportView.class);

        // Route hierarchy: /uc9/sales is a URL child of /uc9.
        assertEquals(List.of("Home", "Reports", "Sales report"), crumbs());
        // Menu hierarchy: same shape, derived from that same route hierarchy.
        assertEquals(ReportsView.class,
                menuParentOf(SalesReportView.class).orElseThrow());
    }

    @Test
    void menuParentDoesNotMoveTheRouteOrTheBreadcrumb() {
        navigate(RevenueForecastView.class);

        // The menu nests it under Reports...
        assertEquals(ReportsView.class,
                menuParentOf(RevenueForecastView.class).orElseThrow());
        // ...while the route hierarchy puts it straight under the landing
        // page, so the trail never mentions Reports.
        assertEquals(List.of("Home", "Revenue forecast"), crumbs());
        assertEquals(HomeView.class,
                RouteConfiguration.forSessionScope()
                        .getRouteParent(RevenueForecastView.class,
                                RouteParameters.empty())
                        .orElseThrow().navigationTarget(),
                "the menu nesting must come from @Menu(parent = ...), not from "
                        + "the route hierarchy");
    }

    @Test
    void flatMenuIsUnaffected() {
        navigate(ReportsView.class);

        // @Menu(parent = ...) regroups the tree only; the flat list still
        // carries every entry, Revenue forecast included.
        List<String> flat = MenuConfiguration.getMenuEntries().stream()
                .map(MenuEntry::title).toList();
        assertTrue(flat.contains("Revenue forecast"), flat.toString());
        assertTrue(flat.contains("Sales report"), flat.toString());
    }

    @Test
    void menuNestedEntryStaysOutOfTheMainNav() {
        navigate(RevenueForecastView.class);

        // The main nav lists roots and their direct children, so an entry that
        // @Menu(parent = ...) pushed a level deeper drops out of it: without
        // the annotation the top-level /forecast route would have sat here
        // next to the use cases themselves.
        assertEquals(List.of("Home", "UC9 — Menu-only nesting"), mainNav());
        assertFalse(mainNav().contains("Revenue forecast"),
                mainNav().toString());
    }

    private List<String> mainNav() {
        return find(SideNav.class).all().stream()
                .filter(nav -> !ReportsView.MENU_ID
                        .equals(nav.getId().orElse("")))
                .flatMap(nav -> nav.getItems().stream())
                .map(SideNavItem::getLabel).toList();
    }

    private List<String> crumbs() {
        BreadcrumbsTester<Breadcrumbs> breadcrumbs = test(
                find(Breadcrumbs.class).single());
        return breadcrumbs.getItemTexts();
    }

    private static Optional<Class<?>> menuParentOf(Class<?> menuClass) {
        return parentIn(MenuConfiguration.getMenuEntriesTree(), null, menuClass);
    }

    private static Optional<Class<?>> parentIn(List<MenuEntry> entries,
            Class<?> parent, Class<?> menuClass) {
        for (MenuEntry entry : entries) {
            if (menuClass.equals(entry.menuClass())) {
                return Optional.ofNullable(parent);
            }
            Optional<Class<?>> nested = parentIn(entry.children(),
                    entry.menuClass(), menuClass);
            if (nested.isPresent()) {
                return nested;
            }
        }
        return Optional.empty();
    }
}
