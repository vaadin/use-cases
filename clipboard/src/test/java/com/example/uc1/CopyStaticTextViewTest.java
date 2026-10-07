package com.example.uc1;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Pre;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = CopyStaticTextView.class)
class CopyStaticTextViewTest extends SpringBrowserlessTest {

    @Test
    void viewRendersHeadingAndCopyButton() {
        navigate(CopyStaticTextView.class);

        assertTrue(findInView(H1.class).all().stream().anyMatch(
                h -> "UC1 — Copy static text on click".equals(h.getText())));
        assertTrue(findInView(Button.class).all().stream()
                .anyMatch(b -> "Copy link".equals(b.getText())));
    }

    @Test
    void textToCopyIsShown() {
        navigate(CopyStaticTextView.class);

        assertTrue(findInView(Pre.class).all().stream().anyMatch(
                p -> "https://example.com/share/abc123".equals(p.getText())));
    }
}
