package com.example.uc21;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Pre;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = ShortcutDownloadView.class)
class ShortcutDownloadViewTest extends SpringBrowserlessTest {

    @Test
    void viewRendersWithCsvPreview() {
        navigate(ShortcutDownloadView.class);

        assertTrue(findInView(H1.class).all().stream()
                .anyMatch(h -> "UC21 — Shortcut download".equals(h.getText())));
        Pre preview = findInView(Pre.class).first();
        assertNotNull(preview);
        assertTrue(preview.getText().contains("ada@example.com"));

        assertTrue(findInView(Button.class).all().stream()
                .anyMatch(b -> "Download CSV".equals(b.getText())));
    }
}
