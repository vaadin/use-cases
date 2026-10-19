package com.example.uc9;

import com.example.components.RatingStarsElement;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;

import com.vaadin.testbench.BrowserTest;
import com.vaadin.testbench.BrowserTestBase;
import com.vaadin.testbench.TestBenchElement;

/**
 * The component's TestBench element in an end-to-end test: the test speaks
 * about stars, not about the buttons inside.
 */
class RatingStarsIT extends BrowserTestBase {

    @BeforeEach
    void open() {
        getDriver().get(System.getProperty("app.url") + "/uc9");
    }

    @BrowserTest
    void ratingReachesTheServer() {
        RatingStarsElement rating = $(RatingStarsElement.class).waitForFirst();

        rating.rate(4);

        waitUntil(driver -> rating.getRating() == 4);
        Assertions.assertEquals("4",
                $(TestBenchElement.class).id("rating-status").getText());
    }
}
