package com.example.uc1;

import com.example.FullscreenTestSupport;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.fullscreen.FullscreenState;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = ImageLightboxView.class)
class ImageLightboxViewTest extends SpringBrowserlessTest {

    @Test
    void clickingAThumbnailShowsThatPhotoOnTheStage() {
        navigate(ImageLightboxView.class);

        test(divWithClass("uc1-thumb", "Ocean")).click();

        assertTrue(
                findInView(Span.class).all().stream()
                        .anyMatch(s -> "Ocean".equals(s.getText())),
                "the current selection should name the clicked photo");
        assertTrue(
                divWithClass("uc1-image", null).getStyle().get("--uc1-image-bg")
                        .contains("#2980b9"),
                "the stage should show the ocean gradient");
    }

    @Test
    void stageIsShownOnlyWhileFullscreen() {
        navigate(ImageLightboxView.class);
        Div stage = divWithClass("uc1-stage", null);

        FullscreenTestSupport
                .setFullscreenState(FullscreenState.NOT_FULLSCREEN);
        runPendingSignalsTasks();
        assertFalse(stage.hasClassName("uc1-stage-active"));

        FullscreenTestSupport.setFullscreenState(FullscreenState.FULLSCREEN);
        runPendingSignalsTasks();
        assertTrue(stage.hasClassName("uc1-stage-active"));

        FullscreenTestSupport
                .setFullscreenState(FullscreenState.NOT_FULLSCREEN);
        runPendingSignalsTasks();
        assertFalse(stage.hasClassName("uc1-stage-active"));
    }

    @Test
    void badgeSaysWhenFullscreenIsNotSupported() {
        navigate(ImageLightboxView.class);

        FullscreenTestSupport
                .setFullscreenState(FullscreenState.NOT_FULLSCREEN);
        runPendingSignalsTasks();
        assertEquals("Click a thumbnail to enlarge", badgeText());

        FullscreenTestSupport.setFullscreenState(FullscreenState.UNSUPPORTED);
        runPendingSignalsTasks();
        assertEquals("Fullscreen is not supported in this browser",
                badgeText());
    }

    private Div divWithClass(String className, @Nullable String text) {
        return findInView(Div.class).all().stream()
                .filter(d -> d.hasClassName(className))
                .filter(d -> text == null || text.equals(d.getText()))
                .findFirst().orElseThrow();
    }

    private String badgeText() {
        return findInView(Span.class).all().stream()
                .filter(s -> s.hasClassName("status-badge")).findFirst()
                .orElseThrow().getText();
    }
}
