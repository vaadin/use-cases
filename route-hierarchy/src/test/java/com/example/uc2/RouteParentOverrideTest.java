package com.example.uc2;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.breadcrumbs.Breadcrumbs;
import com.vaadin.flow.component.breadcrumbs.BreadcrumbsTester;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ViewPackages(classes = OrdersView.class)
class RouteParentOverrideTest extends SpringBrowserlessTest {

    @Test
    void parentShowsOnlyItself() {
        navigate(OrdersView.class);
        assertEquals(List.of("Orders"), crumbs());
    }

    @Test
    void detailLinksBackToOrdersViaRouteParentAnnotation() {
        navigate(OrderDetailView.class, Map.of("orderId", "1001"));

        // "Orders" comes from @RouteParent (not the URL); the leaf uses the
        // PageTitleGenerator label.
        assertEquals(List.of("Orders", "Order #1001"), crumbs());
    }

    @Test
    void detailLeafReflectsTheOrderParameter() {
        navigate(OrderDetailView.class, Map.of("orderId", "1002"));
        assertEquals(List.of("Orders", "Order #1002"), crumbs());
    }

    private List<String> crumbs() {
        BreadcrumbsTester<Breadcrumbs> breadcrumbs = test(
                find(Breadcrumbs.class).single());
        return breadcrumbs.getItemTexts();
    }
}
