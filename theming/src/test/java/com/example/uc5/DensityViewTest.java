package com.example.uc5;

import com.example.Appearance;
import com.example.BaseTheme;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.html.H1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = DensityView.class)
class DensityViewTest extends SpringBrowserlessTest {

    @Test
    void densityIsSetOnTheWorkAreaOnly() {
        DensityView view = navigate(DensityView.class);
        runPendingSignalsTasks();
        assertEquals("UC5 — Compact mode",
                findInView(H1.class).single().getText());
        assertNull(view.workArea().getElement().getAttribute("theme"));

        test(view.densitySelect()).selectItem("Compact");
        runPendingSignalsTasks();
        assertEquals("small",
                view.workArea().getElement().getAttribute("theme"));
        assertFalse(view.getThemeNames().contains("small"));

        test(view.densitySelect()).selectItem("Comfortable");
        runPendingSignalsTasks();
        assertEquals("large",
                view.workArea().getElement().getAttribute("theme"));
        assertFalse(view.lumoCompactLoaded());
    }

    @Test
    void lumoCompactPresetIsGlobalAndRemovedOnLeave() {
        DensityView view = navigate(DensityView.class);
        Appearance.current().setTheme(BaseTheme.LUMO);

        test(view.densitySelect()).selectItem("Compact");
        runPendingSignalsTasks();
        assertTrue(view.lumoCompactLoaded());

        test(view.densitySelect()).selectItem("Default");
        runPendingSignalsTasks();
        assertFalse(view.lumoCompactLoaded());

        test(view.densitySelect()).selectItem("Compact");
        runPendingSignalsTasks();
        view.getElement().removeFromParent();
        assertFalse(view.lumoCompactLoaded());
    }
}
