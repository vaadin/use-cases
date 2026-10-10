package com.example.uc8;

import java.util.List;
import java.util.Map;

import com.example.AppearanceSetup;
import com.example.MediaQueries;
import com.example.MissingAPI;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = AccessibleThemingView.class)
class AccessibleThemingViewTest extends SpringBrowserlessTest {

    @Test
    void highContrastRaisesContrastAndFocusRing() {
        AccessibleThemingView view = navigate(AccessibleThemingView.class);
        runPendingSignalsTasks();
        assertEquals("UC8 — Accessible theming",
                findInView(H1.class).single().getText());

        test(view.highContrast()).click();
        runPendingSignalsTasks();

        Map<String, String> root = MissingAPI.rootProperties(UI.getCurrent());
        assertEquals("3", root.get("--aura-contrast-level"));
        assertEquals("3px", root.get("--vaadin-focus-ring-width"));
    }

    @Test
    void reduceMotionAddsItsStylesheet() {
        AccessibleThemingView view = navigate(AccessibleThemingView.class);

        test(view.reduceMotion()).click();
        runPendingSignalsTasks();

        assertEquals(List.of("aura/aura.css", "reduce-motion.css"),
                AppearanceSetup.styleSheets(UI.getCurrent()));
    }

    @Test
    void systemSettingsCanBeTakenOver() {
        AccessibleThemingView view = navigate(AccessibleThemingView.class);
        MediaQueries.answer(view, AccessibleThemingView.MORE_CONTRAST, true);
        MediaQueries.answer(view, AccessibleThemingView.REDUCED_MOTION, false);

        test(findInView(Button.class).all().stream()
                .filter(b -> "Use my system settings".equals(b.getText()))
                .findFirst().orElseThrow()).click();
        runPendingSignalsTasks();

        assertTrue(view.highContrast().getValue());
        assertFalse(view.reduceMotion().getValue());
    }

    @Test
    void contrastCheckFlagsLightAccents() {
        AccessibleThemingView view = navigate(AccessibleThemingView.class);
        runPendingSignalsTasks();

        List<String> checks = view.checkTexts();
        assertTrue(checks.contains("AaBrand (UC1) #0f766e · 5.5:1 AA"),
                checks.toString());
        assertTrue(
                checks.contains(
                        "AaGlobex Bank dark mode #a5b4fc · 2.0:1 Fails AA"),
                checks.toString());
    }
}
