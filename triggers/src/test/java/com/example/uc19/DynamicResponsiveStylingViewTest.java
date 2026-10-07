package com.example.uc19;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = DynamicResponsiveStylingView.class)
class DynamicResponsiveStylingViewTest extends SpringBrowserlessTest {

    @Test
    void viewRendersWithThreePickersAndATarget() {
        navigate(DynamicResponsiveStylingView.class);

        assertTrue(findInView(H1.class).all().stream().anyMatch(
                h -> "UC19 — Dynamic responsive styling".equals(h.getText())));
        Input small = findInView(Input.class).id("small");
        Input medium = findInView(Input.class).id("medium");
        Input large = findInView(Input.class).id("large");
        assertNotNull(small);
        assertNotNull(medium);
        assertNotNull(large);
        assertEquals("color", small.getType());
        assertNotNull(findInView(Div.class).id("target"));
    }
}
