package com.example.uc7;

import com.example.uc1.SubcategoryView;
import com.example.uc2.OrdersView;
import com.example.uc5.SessionsView;
import com.example.uc6.TeamView;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.router.RouterLink;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = { SitemapView.class, SubcategoryView.class,
        OrdersView.class, SessionsView.class, TeamView.class })
class SitemapTreeTest extends SpringBrowserlessTest {

    @Test
    void rendersOneNodePerRouteInTheMergedChains() {
        navigate(SitemapView.class);

        // 9 distinct routes across the four merged leaf chains.
        long links = findInView(RouterLink.class).all().size();
        assertEquals(9, links, "sitemap must render one link per route node");

        String text = find(UnorderedList.class).withId(SitemapView.ROOT_LIST_ID)
                .single().getElement().getTextRecursively();
        for (String title : new String[] { "Catalog", "Electronics", "Laptops",
                "Settings", "Security", "Active sessions", "Dashboard", "Team",
                "Orders" }) {
            assertTrue(text.contains(title),
                    "sitemap should list '" + title + "' but was: " + text);
        }
    }

    @Test
    void mergesChainsIntoFourRoots() {
        navigate(SitemapView.class);

        UnorderedList root = find(UnorderedList.class)
                .withId(SitemapView.ROOT_LIST_ID).single();
        long rootItems = root.getChildren().filter(ListItem.class::isInstance)
                .count();
        assertEquals(4, rootItems,
                "four leaf chains share no common root, so the tree has four "
                        + "top-level nodes");
    }

    @Test
    void nestsDescendantsUnderTheirAncestor() {
        navigate(SitemapView.class);

        ListItem catalog = find(ListItem.class).all().stream()
                .filter(item -> item.getChildren()
                        .anyMatch(child -> child instanceof RouterLink link
                                && "Catalog".equals(link.getText())))
                .findFirst().orElseThrow();

        String subtree = catalog.getElement().getTextRecursively();
        assertTrue(subtree.contains("Electronics"),
                "Electronics must nest under Catalog: " + subtree);
        assertTrue(subtree.contains("Laptops"),
                "Laptops must nest under Catalog: " + subtree);
    }
}
