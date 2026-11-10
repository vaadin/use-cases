package com.example.uc4;

import java.util.List;

import com.example.TourTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.select.Select;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = WhatsNewView.class)
class WhatsNewViewTest extends TourTestSupport {

    @Test
    void comingFromTheOldestVersionShowsAllNewFeatures() {
        navigate(WhatsNewView.class);

        assertEquals("Step 1 of 3", stepProgress());
        assertEquals("Saved filters", stepTitle());
        assertEquals(List.of("Saved filters", "Export", "Dark mode"), badged());
        assertEquals("3 features are new since 2.0.", status());
    }

    @Test
    void finishingTheTourMakesTheCurrentVersionSeen() {
        navigate(WhatsNewView.class);
        clickInStep("Next");
        clickInStep("Next");
        clickInStep("Done");

        assertEquals(List.of(), badged());
        assertEquals(WhatsNewView.CURRENT_VERSION,
                findInView(Select.class).single().getValue());

        clickInView("Open the app");

        assertTrue(openPopovers().isEmpty());
        assertEquals("Nothing is new since 2.2, so no tour opens.", status());
    }

    @Test
    void comingFromTheLastVersionShowsOnlyWhatCameAfterIt() {
        navigate(WhatsNewView.class);

        test(findInView(Select.class).single()).selectItem("2.1");
        clickInView("Open the app");

        assertEquals("Step 1 of 1", stepProgress());
        assertEquals("Dark mode", stepTitle());
        assertEquals(List.of("Dark mode"), badged());
    }

    private List<String> badged() {
        return findInView(Button.class).withClassName("tour-new").all().stream()
                .map(b -> b.getText().isEmpty() ? b.getTooltip().getText()
                        : b.getText())
                .toList();
    }
}
