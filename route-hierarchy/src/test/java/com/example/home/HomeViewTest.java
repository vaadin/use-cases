package com.example.home;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.example.uc1.CatalogView;
import com.example.uc2.OrdersView;
import com.example.uc3.UsersView;
import com.example.uc4.ProjectsView;
import com.example.uc5.SettingsView;
import com.example.uc6.DashboardView;
import com.example.uc7.SitemapView;
import com.example.uc8.HierarchicalMenuView;
import com.example.uc9.ReportsView;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Section;
import com.vaadin.flow.component.sidenav.SideNavItem;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ViewPackages(classes = { HomeView.class, CatalogView.class, OrdersView.class,
        UsersView.class, ProjectsView.class, SettingsView.class,
        DashboardView.class, SitemapView.class, HierarchicalMenuView.class,
        ReportsView.class })
class HomeViewTest extends SpringBrowserlessTest {

    private record UseCase(String tag, String title, String problem) {
    }

    private record Group(String heading, List<UseCase> useCases) {
    }

    private static final UseCase UC1 = new UseCase("UC1", "URL-prefix trail",
            "Building breadcrumbs from URL prefixes with no configuration");
    private static final UseCase UC2 = new UseCase("UC2",
            "@RouteParent override",
            "Overriding the route parent when the URL does not nest");
    private static final UseCase UC3 = new UseCase("UC3", "Dynamic leaf label",
            "Showing a dynamic page title as the last breadcrumb");
    private static final UseCase UC4 = new UseCase("UC4",
            "Parameter-preserving links",
            "Keeping route parameters in links to ancestor pages");
    private static final UseCase UC5 = new UseCase("UC5", "Up-one-level button",
            "Adding an up-one-level button from the route parent");
    private static final UseCase UC6 = new UseCase("UC6",
            "Layout-wide breadcrumbs",
            "Sharing one breadcrumb trail across a whole layout");
    private static final UseCase UC7 = new UseCase("UC7", "Route-tree sitemap",
            "Generating a sitemap from the route hierarchy");
    private static final UseCase UC8 = new UseCase("UC8", "Hierarchical menu",
            "Showing the menu as a tree that follows the route hierarchy");
    private static final UseCase UC9 = new UseCase("UC9", "Menu-only nesting",
            "Grouping a page in the menu without moving its route");

    private static final List<UseCase> USE_CASES = List.of(UC1, UC2, UC3, UC4,
            UC5, UC6, UC7, UC8, UC9);

    @Test
    void cardsAreGroupedByKindOfProblemInMenuOrder() {
        HomeView home = navigate(HomeView.class);

        List<Group> groups = home.getChildren()
                .filter(Section.class::isInstance)
                .map(section -> new Group(
                        find(H2.class).from(section).single().getText(),
                        find(Card.class).from(section).all().stream()
                                .map(HomeViewTest::useCaseOf).toList()))
                .toList();

        assertEquals(List.of(
                new Group("Finding a page's parents", List.of(UC1, UC2)),
                new Group("Getting each crumb right", List.of(UC3, UC4)),
                new Group("Navigation beyond one view", List.of(UC5, UC6, UC7)),
                new Group("Building the menu", List.of(UC8, UC9))), groups);
    }

    private static UseCase useCaseOf(Card card) {
        return new UseCase(card.getHeaderPrefix().getElement().getText(),
                card.getTitleAsText(),
                card.getChildren().filter(Paragraph.class::isInstance)
                        .map(p -> ((Paragraph) p).getText()).findFirst()
                        .orElse(null));
    }

    @Test
    void navItemsKeepTheShortNameAndShowTheProblemAsTooltip() {
        navigate(HomeView.class);

        Map<String, SideNavItem> items = find(SideNavItem.class).all().stream()
                .collect(Collectors.toMap(SideNavItem::getLabel,
                        Function.identity()));

        USE_CASES.forEach(useCase -> assertEquals(useCase.problem(),
                items.get(useCase.tag() + " — " + useCase.title()).getTooltip()
                        .getText(),
                "the nav tooltip and the home card read the same text"));
    }
}
