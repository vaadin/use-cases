package com.example.uc3;

import com.example.components.RatingStarsTester;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.ComponentTesterPackages;
import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = RatingView.class)
@ComponentTesterPackages("com.example.components")
class RatingViewTest extends SpringBrowserlessTest {

    @Test
    void ratingLightsTheStarsAndReachesTheSignal() {
        RatingView view = navigate(RatingView.class);
        runPendingSignalsTasks();
        assertEquals("Not rated yet", view.status());

        RatingStarsTester stars = test(RatingStarsTester.class, view.rating());
        stars.rate(4);
        runPendingSignalsTasks();

        assertEquals(4, view.rating().getValue());
        assertEquals(4, stars.litStars());
        assertEquals("4 of 5 stars", view.status());
        assertFalse(view.feedback().isVisible());
    }

    @Test
    void lowRatingEventAsksWhatWentWrong() {
        RatingView view = navigate(RatingView.class);

        test(RatingStarsTester.class, view.rating()).rate(2);

        assertTrue(view.feedback().isVisible());

        test(RatingStarsTester.class, view.rating()).rate(5);
        assertFalse(view.feedback().isVisible());
    }

    @Test
    void starsAreAnAccessibleRadioGroup() {
        RatingView view = navigate(RatingView.class);
        test(RatingStarsTester.class, view.rating()).rate(3);

        var root = view.rating().getElement();
        assertEquals("radiogroup", root.getAttribute("role"));
        assertEquals("Your rating", root.getAttribute("aria-label"));
        assertEquals("3 of 5 stars",
                root.getChild(2).getAttribute("aria-label"));
        assertEquals("true", root.getChild(2).getAttribute("aria-checked"));
        assertEquals("false", root.getChild(3).getAttribute("aria-checked"));
    }
}
