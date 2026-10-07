package com.example.uc10;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = HighlightView.class)
class HighlightViewTest extends SpringBrowserlessTest {

    @Test
    void viewRendersWithTargetAndButtons() {
        navigate(HighlightView.class);

        assertTrue(findInView(H1.class).all().stream().anyMatch(
                h -> "UC10 — Parameterised highlight".equals(h.getText())));
        assertNotNull(findInView(Div.class).id("target"));
        assertNotNull(findInView(Button.class).id("gold"));
        assertNotNull(findInView(Button.class).id("red"));
        assertNotNull(findInView(Button.class).id("cyan"));
    }
}
