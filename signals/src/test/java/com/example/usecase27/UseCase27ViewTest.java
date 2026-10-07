package com.example.usecase27;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = UseCase27View.class)
@WithMockUser
class UseCase27ViewTest extends SpringBrowserlessTest {

    @Test
    void overviewRendersBreadcrumbForLandingRoute() {
        navigate(UseCase27View.class);
        runPendingSignalsTasks();

        String breadcrumbText = breadcrumbText();
        assertTrue(breadcrumbText.contains("Use Case 27"),
                "breadcrumb should mention 'Use Case 27' but was: "
                        + breadcrumbText);
        assertTrue(breadcrumbText.contains("Overview"),
                "breadcrumb should mention 'Overview' but was: "
                        + breadcrumbText);
    }

    @Test
    void detailsRouteUpdatesBreadcrumbWithRouteParameter() {
        navigate(UseCase27View.class);
        runPendingSignalsTasks();
        int countAfterOverview = updateCount();

        navigate(UseCase27DetailsView.class, Map.of("id", "42"));
        runPendingSignalsTasks();

        String breadcrumb = breadcrumbText();
        assertTrue(breadcrumb.contains("Details"),
                "breadcrumb should mention 'Details' but was: " + breadcrumb);
        assertTrue(breadcrumb.contains("#42"),
                "breadcrumb should include the :id parameter '#42' but was: "
                        + breadcrumb);

        assertTrue(updateCount() > countAfterOverview,
                "routerStateSignal effect must run again on navigation");
    }

    @Test
    void sameViewWithDifferentParameterStillFiresSignal() {
        navigate(UseCase27DetailsView.class, Map.of("id", "42"));
        runPendingSignalsTasks();
        int firstCount = updateCount();
        assertTrue(breadcrumbText().contains("#42"));

        navigate(UseCase27DetailsView.class, Map.of("id", "100"));
        runPendingSignalsTasks();

        assertTrue(breadcrumbText().contains("#100"),
                "breadcrumb must reflect the new :id, but was: "
                        + breadcrumbText());
        assertTrue(updateCount() > firstCount,
                "Navigating to the same view class with a different route "
                        + "parameter must still fire routerStateSignal");
    }

    @Test
    void settingsRouteUpdatesBreadcrumb() {
        navigate(UseCase27View.class);
        runPendingSignalsTasks();

        navigate(UseCase27SettingsView.class);
        runPendingSignalsTasks();

        String breadcrumb = breadcrumbText();
        assertTrue(breadcrumb.contains("Settings"),
                "breadcrumb should mention 'Settings' but was: " + breadcrumb);
    }

    @Test
    void parentLayoutInstanceIsReusedAcrossSiblingNavigations() {
        navigate(UseCase27View.class);
        runPendingSignalsTasks();
        UseCase27Layout firstLayout = find(UseCase27Layout.class).single();

        navigate(UseCase27DetailsView.class, Map.of("id", "7"));
        runPendingSignalsTasks();
        UseCase27Layout secondLayout = find(UseCase27Layout.class).single();

        assertEquals(firstLayout, secondLayout,
                "UseCase27Layout must be reused across sibling navigations — "
                        + "that is the precondition for routerStateSignal's "
                        + "value as a parent-layout API");
    }

    private String breadcrumbText() {
        Div breadcrumb = find(Div.class).withId(UseCase27Layout.BREADCRUMB_ID)
                .single();
        return breadcrumb.getElement().getTextRecursively();
    }

    private int updateCount() {
        Span badge = find(Span.class).withId(UseCase27Layout.UPDATE_COUNT_ID)
                .single();
        String text = badge.getText();
        int colon = text.lastIndexOf(':');
        return Integer.parseInt(text.substring(colon + 1).trim());
    }
}
