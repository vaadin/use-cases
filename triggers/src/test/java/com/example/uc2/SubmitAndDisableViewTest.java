package com.example.uc2;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Image;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = SubmitAndDisableView.class)
class SubmitAndDisableViewTest extends SpringBrowserlessTest {

    @Test
    void viewRendersThreeTilesAndResetButton() {
        navigate(SubmitAndDisableView.class);

        assertTrue(findInView(H1.class).all().stream()
                .anyMatch(h -> "UC2 — Click an image to dim its siblings"
                        .equals(h.getText())));
        assertNotNull(findInView(Image.class).id("a"));
        assertNotNull(findInView(Image.class).id("b"));
        assertNotNull(findInView(Image.class).id("c"));
        assertEquals("Reset", findInView(Button.class).id("reset").getText());
    }
}
