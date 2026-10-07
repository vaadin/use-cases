package com.example.uc3;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.TextField;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = LiveSignalCounterView.class)
class LiveSignalCounterViewTest extends SpringBrowserlessTest {

    @Test
    void viewRendersWithSlugAndConfirmation() {
        navigate(LiveSignalCounterView.class);

        assertTrue(findInView(H1.class).all().stream()
                .anyMatch(h -> "UC3 — Copy a share link that's never rendered"
                        .equals(h.getText())));
        assertNotNull(findInView(TextField.class).id("slug"));
        assertNotNull(findInView(Button.class).id("copy"));
        assertEquals("(no copy yet)",
                findInView(Span.class).id("confirmation").getText());
    }
}
