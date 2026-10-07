package com.example.uc3;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.breadcrumbs.Breadcrumbs;
import com.vaadin.flow.component.breadcrumbs.BreadcrumbsTester;
import com.vaadin.flow.component.html.H1;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ViewPackages(classes = UsersView.class)
class DynamicLeafLabelTest extends SpringBrowserlessTest {

    @Test
    void leafUsesGeneratorTitleNotClassName() {
        navigate(UserProfileView.class, Map.of("userId", "ada"));

        // The H1 and the breadcrumb leaf both resolve the person's name from
        // the PageTitleGenerator — never the bare class name.
        assertEquals("Ada Lovelace", heading());
        assertEquals(List.of("Users", "Ada Lovelace"), crumbs());
    }

    @Test
    void leafLabelTracksTheRouteParameter() {
        navigate(UserProfileView.class, Map.of("userId", "grace"));
        assertEquals("Grace Hopper", heading());
        assertEquals(List.of("Users", "Grace Hopper"), crumbs());
    }

    private String heading() {
        return findInView(H1.class).single().getText();
    }

    private List<String> crumbs() {
        BreadcrumbsTester<Breadcrumbs> breadcrumbs = test(
                find(Breadcrumbs.class).single());
        return breadcrumbs.getItemTexts();
    }
}
