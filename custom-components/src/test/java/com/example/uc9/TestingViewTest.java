package com.example.uc9;

import java.util.List;

import com.example.components.RatingStarsTester;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.ComponentTesterPackages;
import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.Anchor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The component's own tester in use, as any application test would use it. */
@SpringBootTest
@ViewPackages(classes = TestingView.class)
@ComponentTesterPackages("com.example.components")
class TestingViewTest extends SpringBrowserlessTest {

    @Test
    void testerRatesLikeAUser() {
        TestingView view = navigate(TestingView.class);

        test(RatingStarsTester.class, view.rating()).rate(3);

        assertEquals("3", view.ratingStatus());
        assertEquals(3,
                test(RatingStarsTester.class, view.rating()).litStars());
    }

    @Test
    void showsTheTestSupportThatShipsWithTheComponent() {
        navigate(TestingView.class);

        List<String> files = findInView(Details.class).all().stream()
                .map(Details::getSummaryText).toList();
        assertTrue(
                files.stream().anyMatch(
                        f -> f.startsWith("RatingStarsTester.java · ")),
                files.toString());
        assertTrue(findInView(Anchor.class).all().stream().anyMatch(a -> a
                .getHref()
                .equals("https://github.com/vaadin/use-cases/blob/main/"
                        + "custom-components/src/test/java/com/"
                        + "example/components/RatingStarsElement.java")));
    }

    @Test
    void testerRefusesWhatAUserCannotDo() {
        TestingView view = navigate(TestingView.class);
        RatingStarsTester stars = test(RatingStarsTester.class, view.rating());

        assertThrows(IllegalArgumentException.class, () -> stars.rate(6));
        view.rating().setReadOnly(true);
        assertThrows(IllegalStateException.class, () -> stars.rate(2));
        assertEquals("0", view.ratingStatus());
    }
}
