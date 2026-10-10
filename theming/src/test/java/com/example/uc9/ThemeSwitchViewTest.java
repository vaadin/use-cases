package com.example.uc9;

import java.util.List;

import com.example.Appearance;
import com.example.AppearanceSetup;
import com.example.BaseTheme;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.H1;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ViewPackages(classes = ThemeSwitchView.class)
class ThemeSwitchViewTest extends SpringBrowserlessTest {

    @Test
    void switchingReplacesTheThemeStylesheet() {
        ThemeSwitchView view = navigate(ThemeSwitchView.class);
        runPendingSignalsTasks();
        assertEquals("UC9 — Aura or Lumo",
                findInView(H1.class).single().getText());
        assertEquals(BaseTheme.AURA, view.themeSelect().getValue());

        test(view.themeSelect()).selectItem("Lumo");
        runPendingSignalsTasks();

        assertEquals(List.of("lumo/lumo.css"),
                AppearanceSetup.styleSheets(UI.getCurrent()));
    }

    @Test
    void brandStaysOnTopOfTheNewTheme() {
        ThemeSwitchView view = navigate(ThemeSwitchView.class);
        Appearance.current().setBrand(true);
        runPendingSignalsTasks();

        test(view.themeSelect()).selectItem("Lumo");
        runPendingSignalsTasks();

        assertEquals(List.of("lumo/lumo.css", "brand.css"),
                AppearanceSetup.styleSheets(UI.getCurrent()));
    }
}
