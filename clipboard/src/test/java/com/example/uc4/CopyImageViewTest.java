package com.example.uc4;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Image;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = CopyImageView.class)
class CopyImageViewTest extends SpringBrowserlessTest {

    @Test
    void viewRendersHeadingImageAndCopyButton() {
        navigate(CopyImageView.class);

        assertTrue(findInView(H1.class).all().stream()
                .anyMatch(h -> "UC4 — Copy image".equals(h.getText())));
        assertTrue(findInView(Image.class).all().stream()
                .anyMatch(img -> "Sample chart"
                        .equals(img.getElement().getAttribute("alt"))));
        assertTrue(findInView(Button.class).all().stream()
                .anyMatch(b -> "Copy chart".equals(b.getText())));
    }
}
