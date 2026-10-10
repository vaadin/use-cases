package com.example.uc4;

import java.util.Map;

import com.example.FontSize;
import com.example.MissingAPI;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = UserPreferencesView.class)
class UserPreferencesViewTest extends SpringBrowserlessTest {

    @Test
    void startsWithTheThemeDefaults() {
        UserPreferencesView view = navigate(UserPreferencesView.class);
        runPendingSignalsTasks();

        assertEquals("UC4 — User's own look",
                findInView(H1.class).single().getText());
        assertEquals(FontSize.DEFAULT, view.fontSize().getValue());
        assertEquals(Map.of(), MissingAPI.rootProperties(UI.getCurrent()));
        assertEquals("Theme accent color", view.contrast());
    }

    @Test
    void pickedAccentIsSetOnTheRootForBothThemes() {
        UserPreferencesView view = navigate(UserPreferencesView.class);

        test(swatch("#7c3aed")).click();
        runPendingSignalsTasks();

        Map<String, String> root = MissingAPI.rootProperties(UI.getCurrent());
        assertEquals("#7c3aed", root.get("--aura-accent-color-light"));
        assertEquals("#7c3aed", root.get("--lumo-primary-color"));
        assertTrue(swatch("#7c3aed").hasClassName("selected"));
        assertEquals("#7c3aed", view.custom().getValue());
        assertEquals("#7c3aed · contrast with white text 5.7:1",
                view.contrast());
    }

    @Test
    void customColorAndTextSizeAreApplied() {
        UserPreferencesView view = navigate(UserPreferencesView.class);

        test(view.custom()).setValue("#ff8800");
        test(view.fontSize()).selectItem("Large");
        runPendingSignalsTasks();

        Map<String, String> root = MissingAPI.rootProperties(UI.getCurrent());
        assertEquals("#ff8800", root.get("--aura-accent-color-dark"));
        assertEquals("16", root.get("--aura-base-font-size"));
    }

    @Test
    void resetRemovesTheOverrides() {
        UserPreferencesView view = navigate(UserPreferencesView.class);
        test(swatch("#0f766e")).click();
        test(view.fontSize()).selectItem("Extra large");
        runPendingSignalsTasks();

        test(findInView(Button.class).all().stream()
                .filter(b -> "Reset to default".equals(b.getText())).findFirst()
                .orElseThrow()).click();
        runPendingSignalsTasks();

        assertEquals(Map.of(), MissingAPI.rootProperties(UI.getCurrent()));
    }

    private Button swatch(String color) {
        return findInView(Button.class).all().stream()
                .filter(b -> ("Accent " + color)
                        .equals(b.getElement().getAttribute("aria-label")))
                .findFirst().orElseThrow();
    }
}
