package com.example.uc23;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = KonamiCodeView.class)
class KonamiCodeViewTest extends SpringBrowserlessTest {

    @Test
    void viewRendersWithHint() {
        navigate(KonamiCodeView.class);

        assertTrue(findInView(H1.class).all().stream()
                .anyMatch(h -> "UC23 — Konami code".equals(h.getText())));
        assertEquals("(no unlocks yet)",
                findInView(Span.class).id("hint").getText());
    }
}
