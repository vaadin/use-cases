package com.example.uc8;

import com.example.FullscreenTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.fullscreen.FullscreenState;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.select.Select;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = OverlaysFullscreenView.class)
class OverlaysFullscreenViewTest extends SpringBrowserlessTest {

    @Test
    void viewRendersHeadingPanelAndOverlayControls() {
        navigate(OverlaysFullscreenView.class);

        assertTrue(findInView(H1.class).all().stream().anyMatch(
                h -> "UC8 — Overlays in fullscreen".equals(h.getText())),
                "expected the UC8 heading");

        assertEquals(1,
                findInView(Div.class).all().stream()
                        .filter(d -> d.getClassNames().contains("overlay-panel"))
                        .count(),
                "expected the fullscreenable overlay panel");

        assertEquals(1, findInView(MenuBar.class).all().size(),
                "expected the MenuBar overlay control");
        assertEquals(1, findInView(Select.class).all().size(),
                "expected the Select dropdown overlay control");
    }

    @Test
    void selectingFromDropdownRecordsTheAction() {
        navigate(OverlaysFullscreenView.class);
        runPendingSignalsTasks();

        @SuppressWarnings("unchecked")
        Select<String> select = findInView(Select.class).single();
        select.setValue("Green");
        runPendingSignalsTasks();

        assertTrue(findInView(Span.class).all().stream()
                .anyMatch(s -> s.getClassNames().contains("last-action")
                        && "Select: Green".equals(s.getText())),
                "selecting a value should record the overlay action");
    }

    @Test
    void stateBadgeReflectsFullscreenSignal() {
        navigate(OverlaysFullscreenView.class);
        runPendingSignalsTasks();

        assertBadgeContains("Detecting");

        FullscreenTestSupport
                .setFullscreenState(FullscreenState.NOT_FULLSCREEN);
        runPendingSignalsTasks();
        assertBadgeContains("Enter fullscreen");

        FullscreenTestSupport.setFullscreenState(FullscreenState.UNSUPPORTED);
        runPendingSignalsTasks();
        assertBadgeContains("not supported");
    }

    private void assertBadgeContains(String fragment) {
        assertTrue(findInView(Span.class).all().stream()
                .anyMatch(s -> s.getClassNames().contains("status-badge")
                        && s.getText() != null
                        && s.getText().contains(fragment)),
                "expected status badge to contain \"" + fragment + "\"");
    }
}
