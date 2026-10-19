package com.example.uc11;

import java.util.Map;

import com.example.TriggerCallbacks;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ViewPackages(classes = RightClickCoordsView.class)
class RightClickCoordsViewTest extends SpringBrowserlessTest {

    @Test
    void serverShowsThePositionOfTheRightClick() {
        navigate(RightClickCoordsView.class);
        assertEquals("UC11 — Right-click coords",
                findInView(H1.class).single().getText());
        Span last = findInView(Span.class).id("last");
        assertEquals("(no right-click yet)", last.getText());

        // The {x, y} object that PointInput builds in the browser.
        TriggerCallbacks.fire(findInView(Div.class).id("panel"),
                Map.of("x", 120, "y", 45));

        assertEquals("Last right-click: x=120 y=45", last.getText());
    }
}
