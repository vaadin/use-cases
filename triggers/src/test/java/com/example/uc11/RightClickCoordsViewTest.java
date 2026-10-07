package com.example.uc11;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = RightClickCoordsView.class)
class RightClickCoordsViewTest extends SpringBrowserlessTest {

    @Test
    void viewRendersWithPanelAndCoords() {
        navigate(RightClickCoordsView.class);

        assertTrue(findInView(H1.class).all().stream().anyMatch(
                h -> "UC11 — Right-click coordinates".equals(h.getText())));
        assertNotNull(findInView(Div.class).id("panel"));
        assertEquals("(no right-click yet)",
                findInView(Span.class).id("last").getText());
    }
}
