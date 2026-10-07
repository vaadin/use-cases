package com.example.uc12;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = AccessibleSaveView.class)
class AccessibleSaveViewTest extends SpringBrowserlessTest {

    @Test
    void viewRendersWithLiveRegionAndSaveButton() {
        navigate(AccessibleSaveView.class);

        assertTrue(findInView(H1.class).all().stream().anyMatch(
                h -> "UC12 — Accessibility announce".equals(h.getText())));
        Div live = findInView(Div.class).id("live");
        assertNotNull(live);
        assertEquals("polite", live.getElement().getAttribute("aria-live"));
        assertNotNull(findInView(Button.class).id("save"));
    }

    @Test
    void clickUpdatesVisibleBadgeServerSide() {
        navigate(AccessibleSaveView.class);
        test(findInView(Button.class).id("save")).click();
        assertTrue(findInView(Span.class).id("badge").getText()
                .startsWith("Saved at "));
    }
}
