package com.example.uc3;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = CopyRichContentView.class)
class CopyRichContentViewTest extends SpringBrowserlessTest {

    @Test
    void viewRendersHeadingAndCopyButton() {
        navigate(CopyRichContentView.class);

        assertTrue(findInView(H1.class).all().stream().anyMatch(
                h -> "UC3 — Copy rich content (HTML + plain-text fallback)"
                        .equals(h.getText())));
        assertTrue(findInView(Button.class).all().stream()
                .anyMatch(b -> "Copy rich content".equals(b.getText())));
    }
}
