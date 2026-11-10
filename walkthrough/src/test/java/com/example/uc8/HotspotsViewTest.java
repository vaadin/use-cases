package com.example.uc8;

import java.util.List;

import com.example.TourTestSupport;
import com.example.home.HomeView;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.popover.PopoverTester;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = { HotspotsView.class, HomeView.class })
class HotspotsViewTest extends TourTestSupport {

    @Test
    void aHintOpensOnlyWhenItsDotIsClicked() {
        navigate(HotspotsView.class);
        assertEquals(
                List.of("New: Saved filters", "New: Export", "New: Dark mode"),
                dots());
        assertTrue(openPopovers().isEmpty());

        PopoverTester.forTarget(dot("New: Export")).clickTarget();

        assertEquals("Export", stepTitle());
    }

    @Test
    void gotItRemovesTheDotForTheSession() {
        navigate(HotspotsView.class);
        PopoverTester.forTarget(dot("New: Export")).clickTarget();

        clickInStep("Got it");

        assertTrue(openPopovers().isEmpty());
        assertEquals(List.of("New: Saved filters", "New: Dark mode"), dots());
        assertEquals("2 of 3 hints left", status());

        navigate(HomeView.class);
        navigate(HotspotsView.class);
        assertEquals(List.of("New: Saved filters", "New: Dark mode"), dots());
    }

    @Test
    void theHintsCanBeShownAgain() {
        navigate(HotspotsView.class);
        for (String dot : dots()) {
            PopoverTester.forTarget(dot(dot)).clickTarget();
            clickInStep("Got it");
        }
        assertEquals("No hints left", status());

        clickInView("Show the hints again");

        assertEquals(3, dots().size());
        assertEquals("3 of 3 hints left", status());
    }

    private List<String> dots() {
        return findInView(Button.class).withClassName("hotspot").all().stream()
                .map(Button::getAriaLabel).map(label -> label.orElse(""))
                .toList();
    }

    private Button dot(String label) {
        return findInView(Button.class).withClassName("hotspot")
                .withAriaLabel(label).single();
    }
}
