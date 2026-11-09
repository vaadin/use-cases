package com.example.uc8;

import com.example.FullscreenTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.fullscreen.FullscreenState;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.select.Select;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = OverlaysFullscreenView.class)
class OverlaysFullscreenViewTest extends SpringBrowserlessTest {

    @Test
    void selectingFromDropdownRecordsTheAction() {
        navigate(OverlaysFullscreenView.class);

        @SuppressWarnings("unchecked")
        Select<String> select = findInView(Select.class).single();
        test(select).selectItem("Green");

        assertTrue(
                findInView(Span.class).all().stream()
                        .anyMatch(s -> s.hasClassName("uc8-last-action")
                                && "Select: Green".equals(s.getText())),
                "selecting a value should record the overlay action");
    }

    @Test
    void panelFillsTheScreenOnlyWhileFullscreen() {
        navigate(OverlaysFullscreenView.class);
        Div panel = findInView(Div.class).all().stream()
                .filter(d -> d.hasClassName("uc8-panel")).findFirst()
                .orElseThrow();

        FullscreenTestSupport.setFullscreenState(FullscreenState.FULLSCREEN);
        runPendingSignalsTasks();
        assertTrue(panel.hasClassName("uc8-panel-fullscreen"));

        FullscreenTestSupport
                .setFullscreenState(FullscreenState.NOT_FULLSCREEN);
        runPendingSignalsTasks();
        assertFalse(panel.hasClassName("uc8-panel-fullscreen"));
    }
}
