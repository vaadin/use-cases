package com.example.uc8;

import com.example.components.RatingStars;
import com.example.components.Sparkline;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.page.ColorScheme;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = StylingView.class)
class StylingViewTest extends SpringBrowserlessTest {

    @Test
    void variantsAreThemeNamesOnTheComponents() {
        navigate(StylingView.class);

        assertTrue(findInView(RatingStars.class).all().stream()
                .anyMatch(r -> r.getThemeNames().contains("large")));
        assertTrue(findInView(Sparkline.class).all().stream()
                .anyMatch(s -> s.getThemeNames().contains("success")
                        && s.getThemeNames().contains("filled")));
        assertTrue(findInView(RatingStars.class).all().stream()
                .anyMatch(r -> r.hasClassName("hearts")));
    }

    @Test
    void darkModeSwitchesTheColorScheme() {
        navigate(StylingView.class);

        test(findInView(Checkbox.class).single()).click();

        assertEquals(ColorScheme.Value.DARK,
                UI.getCurrent().getPage().getColorScheme());
    }
}
