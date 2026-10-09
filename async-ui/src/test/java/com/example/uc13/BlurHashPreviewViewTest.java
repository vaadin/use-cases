package com.example.uc13;

import java.util.List;

import com.example.BlurHash;
import com.example.uc13.BlurHashPreviewView.Placeholder;
import com.example.uc13.BlurHashPreviewView.Tile;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.ComponentTester;
import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = BlurHashPreviewView.class)
class BlurHashPreviewViewTest extends SpringBrowserlessTest {

    @Test
    void previewsArriveWithThePage() {
        BlurHashPreviewView view = navigate(BlurHashPreviewView.class);

        assertEquals("UC13 — Slow images with a preview",
                findInView(H1.class).single().getText());
        assertEquals(4, view.tiles().size());
        for (Tile tile : view.tiles()) {
            assertTrue(tile.preview().isVisible());
            assertTrue(
                    tile.preview().getSrc()
                            .startsWith("data:image/png;base64,"),
                    "the preview must not need a request of its own");
            assertFalse(tile.full().getSrc().startsWith("data:"));
            assertEquals(BlurHash.averageColor(tile.photo().blurHash()),
                    tile.frame().getStyle().get("background-color"));
            assertTrue(tile.hasClassName("fade-in"));
            assertFalse(tile.isLoaded());
        }
        assertTrue(statusTexts().getFirst().startsWith("Loading ("));
    }

    @Test
    void photoFadesInWhenTheBrowserHasLoadedIt() {
        BlurHashPreviewView view = navigate(BlurHashPreviewView.class);
        Tile first = view.tiles().getFirst();

        fireLoad(first.full());

        assertTrue(first.isLoaded());
        assertTrue(first.hasClassName("loaded"));
        assertEquals("Loaded", statusTexts().getFirst());
        assertFalse(view.tiles().get(1).isLoaded());
    }

    @Test
    void switchingThePlaceholderLoadsThePhotosAgain() {
        BlurHashPreviewView view = navigate(BlurHashPreviewView.class);
        Tile first = view.tiles().getFirst();
        fireLoad(first.full());
        String firstSrc = first.full().getSrc();
        @SuppressWarnings("unchecked")
        RadioButtonGroup<Placeholder> placeholder = findInView(
                RadioButtonGroup.class).single();

        test(placeholder).selectItem("Average colour");
        assertFalse(first.isLoaded());
        assertNotEquals(firstSrc, first.full().getSrc(),
                "a reload needs a new URL, or the browser answers from cache");
        assertFalse(first.preview().isVisible());
        assertEquals(BlurHash.averageColor(first.photo().blurHash()),
                first.frame().getStyle().get("background-color"));
        assertTrue(first.hasClassName("fade-in"));

        test(placeholder).selectItem("Nothing");
        assertNull(first.frame().getStyle().get("background-color"));
        assertFalse(first.hasClassName("fade-in"),
                "without a placeholder the photo paints as it arrives");
    }

    private void fireLoad(Image image) {
        new ComponentTester<Image>(image) {
            void load() {
                fireDomEvent("load");
            }
        }.load();
    }

    private List<String> statusTexts() {
        return find(Span.class).all().stream()
                .filter(span -> span.hasClassName("photo-status"))
                .map(Span::getText).toList();
    }
}
