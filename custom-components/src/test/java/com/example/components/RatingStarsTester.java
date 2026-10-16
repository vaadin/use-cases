package com.example.components;

import com.vaadin.browserless.ComponentTester;
import com.vaadin.browserless.Tests;

/**
 * The browserless tester that ships with {@link RatingStars} (UC9): it rates
 * the way a user does, by clicking a star, so read-only and the low-rating
 * event behave as in the browser. Tests pick it up with
 * {@code @ComponentTesterPackages("com.example.components")}.
 */
@Tests(RatingStars.class)
public class RatingStarsTester extends ComponentTester<RatingStars> {

    public RatingStarsTester(RatingStars component) {
        super(component);
    }

    /** Clicks the given star, 1 to 5. */
    public void rate(int stars) {
        ensureComponentIsUsable();
        if (stars < 1 || stars > RatingStars.MAX) {
            throw new IllegalArgumentException("No star " + stars);
        }
        getComponent().stars().get(stars - 1).click();
    }

    /** The number of stars that are lit. */
    public int litStars() {
        return (int) getComponent().stars().stream()
                .filter(star -> star.hasClassName("on")).count();
    }
}
