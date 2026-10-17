package com.example.uc4;

import com.example.FullscreenTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.fullscreen.FullscreenState;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = ReactiveLayoutView.class)
class ReactiveLayoutViewTest extends SpringBrowserlessTest {

    @Test
    void dashboardClassFollowsFullscreenSignal() {
        navigate(ReactiveLayoutView.class);
        runPendingSignalsTasks();

        FullscreenTestSupport
                .setFullscreenState(FullscreenState.NOT_FULLSCREEN);
        runPendingSignalsTasks();
        assertDashboardHas("uc4-compact", true);
        assertDashboardHas("uc4-spacious", false);

        FullscreenTestSupport.setFullscreenState(FullscreenState.FULLSCREEN);
        runPendingSignalsTasks();
        assertDashboardHas("uc4-compact", false);
        assertDashboardHas("uc4-spacious", true);

        FullscreenTestSupport.setFullscreenState(FullscreenState.UNSUPPORTED);
        runPendingSignalsTasks();
        assertDashboardHas("uc4-compact", false);
        assertDashboardHas("uc4-spacious", false);
    }

    @Test
    void densityAsideTextChangesWithSignal() {
        navigate(ReactiveLayoutView.class);
        runPendingSignalsTasks();

        FullscreenTestSupport.setFullscreenState(FullscreenState.FULLSCREEN);
        runPendingSignalsTasks();
        assertTrue(findInView(Span.class).all().stream()
                .anyMatch(s -> s.getClassNames().contains("uc4-density")
                        && "Density: spacious (3 columns)".equals(s.getText())),
                "density aside should announce spacious layout");

        FullscreenTestSupport
                .setFullscreenState(FullscreenState.NOT_FULLSCREEN);
        runPendingSignalsTasks();
        assertTrue(findInView(Span.class).all().stream()
                .anyMatch(s -> s.getClassNames().contains("uc4-density")
                        && "Density: compact (2 columns)".equals(s.getText())),
                "density aside should announce compact layout");
    }

    private void assertDashboardHas(String cls, boolean expected) {
        Div dashboard = findInView(Div.class).all().stream()
                .filter(d -> d.hasClassName("uc4-dashboard")).findFirst()
                .orElseThrow();
        assertEquals(expected, dashboard.getClassNames().contains(cls),
                "dashboard should " + (expected ? "have" : "not have")
                        + " class \"" + cls + "\"");
    }
}
