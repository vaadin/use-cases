package com.example.uc4;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = JsTriggerView.class)
class JsTriggerViewTest extends SpringBrowserlessTest {

    @Test
    void viewRendersTarget() {
        navigate(JsTriggerView.class);

        assertTrue(findInView(H1.class).all().stream().anyMatch(
                h -> "UC4 — Double-click to copy".equals(h.getText())));
        assertNotNull(findInView(Div.class).id("target"));
    }
}
