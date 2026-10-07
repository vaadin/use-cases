package com.example.uc18;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.TextArea;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = AutoSaveSignalView.class)
class AutoSaveSignalViewTest extends SpringBrowserlessTest {

    @Test
    void viewRendersWithTextareaAndCounts() {
        navigate(AutoSaveSignalView.class);

        assertTrue(findInView(H1.class).all().stream()
                .anyMatch(h -> "UC18 — Auto-save signal".equals(h.getText())));
        assertNotNull(findInView(TextArea.class).id("draft"));
        assertEquals("0 characters",
                findInView(Span.class).id("chars").getText());
        assertEquals("0 words", findInView(Span.class).id("words").getText());
    }
}
