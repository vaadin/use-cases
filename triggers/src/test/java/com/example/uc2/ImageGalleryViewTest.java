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

@SpringBootTest
@ViewPackages(classes = ImageGalleryView.class)
class ImageGalleryViewTest extends SpringBrowserlessTest {

    // The selection happens in the browser only (API-GAPS.md #7), so the
    // test can check what is rendered, not the click.
    @Test
    void viewRendersThreeTilesAndResetButton() {
        navigate(ImageGalleryView.class);

        assertEquals("UC2 — Image gallery select",
                findInView(H1.class).single().getText());
        assertNotNull(findInView(Image.class).id("a"));
        assertNotNull(findInView(Image.class).id("b"));
        assertNotNull(findInView(Image.class).id("c"));
        assertEquals("Reset", findInView(Button.class).id("reset").getText());
    }
}
