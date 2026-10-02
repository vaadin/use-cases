package com.example.uc8;

import java.util.List;
import java.util.Optional;

import com.example.home.HomeView;
import com.example.uc1.CatalogView;
import com.example.uc2.OrdersView;
import com.example.uc3.UsersView;
import com.example.uc4.ProjectsView;
import com.example.uc5.SettingsView;
import com.example.uc6.DashboardView;
import com.example.uc7.SitemapView;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Registers every use-case package so the whole menu is populated, then checks
 * that the flat main navigation and UC8's hierarchical menu render the same
 * {@code @Menu} set two different ways.
 */
@SpringBootTest
@ViewPackages(classes = { HierarchicalMenuView.class, HomeView.class,
        CatalogView.class, OrdersView.class, UsersView.class,
        ProjectsView.class, SettingsView.class, DashboardView.class,
        SitemapView.class })
class HierarchicalMenuTest extends SpringBrowserlessTest {

    @Test
    void treeNestsChildRoutesUnderTheirParents() {
        navigate(HierarchicalMenuView.class);

        assertTrue(findInView(H1.class).all().stream()
                .anyMatch(h -> "Hierarchical menu".equals(h.getText())));

        SideNav tree = find(SideNav.class).withId(HierarchicalMenuView.MENU_ID)
                .single();

        SideNavItem catalog = item(tree.getItems(), "UC1 — URL-prefix trail");
        assertTrue(childLabels(catalog).contains("Electronics"),
                childLabels(catalog).toString());
        SideNavItem electronics = item(catalog.getItems(), "Electronics");
        assertTrue(childLabels(electronics).contains("Laptops"),
                childLabels(electronics).toString());

        SideNavItem settings = item(tree.getItems(),
                "UC5 — Up-one-level button");
        assertTrue(childLabels(settings).contains("Security"));
        assertTrue(childLabels(item(settings.getItems(), "Security"))
                .contains("Active sessions"));

        SideNavItem dashboard = item(tree.getItems(),
                "UC6 — Layout-wide breadcrumbs");
        assertTrue(childLabels(dashboard).contains("Team"));
    }

    @Test
    void mainNavListsOnlyTopLevelUseCases() {
        navigate(CatalogView.class);

        // CatalogView adds no SideNav of its own, so this is the main nav.
        SideNav mainNav = find(SideNav.class).single();
        List<String> labels = mainNav.getItems().stream()
                .map(SideNavItem::getLabel).toList();

        assertTrue(labels.contains("UC1 — URL-prefix trail"),
                labels.toString());
        assertTrue(labels.contains("UC8 — Hierarchical menu"),
                labels.toString());
        // The nested children are reachable only via UC8's tree.
        assertFalse(labels.contains("Electronics"), labels.toString());
        assertFalse(labels.contains("Laptops"), labels.toString());
        assertFalse(labels.contains("Active sessions"), labels.toString());
    }

    private static SideNavItem item(List<SideNavItem> items, String label) {
        return findItem(items, label).orElseThrow(() -> new AssertionError(
                "no menu item labelled '" + label + "'"));
    }

    private static Optional<SideNavItem> findItem(List<SideNavItem> items,
            String label) {
        for (SideNavItem item : items) {
            if (label.equals(item.getLabel())) {
                return Optional.of(item);
            }
            Optional<SideNavItem> nested = findItem(item.getItems(), label);
            if (nested.isPresent()) {
                return nested;
            }
        }
        return Optional.empty();
    }

    private static List<String> childLabels(SideNavItem item) {
        return item.getItems().stream().map(SideNavItem::getLabel).toList();
    }
}
