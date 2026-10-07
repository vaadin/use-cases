package com.example.uc1;

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
@ViewPackages(classes = ShortcutSaveView.class)
class ShortcutSaveViewTest extends SpringBrowserlessTest {

    @Test
    void viewRendersWithNotesAndStatus() {
        navigate(ShortcutSaveView.class);

        assertTrue(findInView(H1.class).all().stream()
                .anyMatch(h -> "UC1 — Ctrl+S writes a snapshot to the clipboard"
                        .equals(h.getText())));
        assertNotNull(findInView(TextArea.class).id("notes"));
        assertEquals("(no snapshot yet)",
                findInView(Span.class).id("status").getText());
    }
}
