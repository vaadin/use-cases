package com.example.uc1;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.breadcrumbs.Breadcrumbs;
import com.vaadin.flow.component.breadcrumbs.BreadcrumbsTester;
import com.vaadin.flow.component.html.H1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = SubcategoryView.class)
class UrlPrefixTrailTest extends SpringBrowserlessTest {

    @Test
    void rootShowsSingleCurrentCrumb() {
        navigate(CatalogView.class);

        assertTrue(findInView(H1.class).all().stream()
                .anyMatch(h -> "Catalog".equals(h.getText())));
        // A single crumb: in ROUTER mode the last (here only) item is the
        // current page, so there is nothing to link back to.
        assertEquals(List.of("Catalog"), crumbs());
    }

    @Test
    void categoryShowsParentLinkAndCurrentPage() {
        navigate(CategoryView.class);
        assertEquals(List.of("Catalog", "Electronics"), crumbs());
    }

    @Test
    void leafWalksTwoUrlSegmentsToTheRoot() {
        navigate(SubcategoryView.class);
        assertEquals(List.of("Catalog", "Electronics", "Laptops"), crumbs());
    }

    @Test
    void clickingAnAncestorCrumbNavigatesUp() {
        navigate(SubcategoryView.class);

        BreadcrumbsTester<Breadcrumbs> breadcrumbs = test(
                find(Breadcrumbs.class).single());
        breadcrumbs.clickItem("Catalog");

        assertEquals(List.of("Catalog"), crumbs(),
                "clicking the Catalog crumb must navigate to the root");
    }

    private List<String> crumbs() {
        BreadcrumbsTester<Breadcrumbs> breadcrumbs = test(
                find(Breadcrumbs.class).single());
        return breadcrumbs.getItemTexts();
    }
}
