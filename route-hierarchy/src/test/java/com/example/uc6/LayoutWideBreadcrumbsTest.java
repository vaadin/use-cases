package com.example.uc6;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.breadcrumbs.Breadcrumbs;
import com.vaadin.flow.component.breadcrumbs.BreadcrumbsTester;
import com.vaadin.flow.component.html.Span;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = DashboardView.class)
class LayoutWideBreadcrumbsTest extends SpringBrowserlessTest {

    @Test
    void sharedBarRendersForTheRootChild() {
        navigate(DashboardView.class);
        assertEquals(List.of("Dashboard"), crumbs());
    }

    @Test
    void sharedBarTracksDeeperChildrenIncludingDynamicLeaf() {
        navigate(MemberView.class, Map.of("member", "kim"));
        assertEquals(List.of("Dashboard", "Team", "Kim Park"), crumbs());
    }

    @Test
    void counterRebuildsOncePerNavigationAndLayoutIsReused() {
        navigate(DashboardView.class);
        TeamLayout firstLayout = find(TeamLayout.class).single();
        int afterFirst = rebuildCount();

        navigate(TeamView.class);
        assertEquals(List.of("Dashboard", "Team"), crumbs());
        int afterSecond = rebuildCount();
        assertTrue(afterSecond > afterFirst,
                "routerStateSignal effect must rebuild the trail on navigation");

        navigate(MemberView.class, Map.of("member", "lee"));
        assertEquals(List.of("Dashboard", "Team", "Lee Wong"), crumbs());
        assertTrue(rebuildCount() > afterSecond);

        TeamLayout lastLayout = find(TeamLayout.class).single();
        assertEquals(firstLayout, lastLayout,
                "the parent layout instance must be reused across navigations");
    }

    private List<String> crumbs() {
        BreadcrumbsTester<Breadcrumbs> breadcrumbs = test(
                find(Breadcrumbs.class).single());
        return breadcrumbs.getItemTexts();
    }

    private int rebuildCount() {
        Span badge = find(Span.class).withId(TeamLayout.REBUILD_BADGE_ID)
                .single();
        String text = badge.getText();
        return Integer
                .parseInt(text.substring(text.lastIndexOf(':') + 1).trim());
    }
}
