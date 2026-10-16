package com.example.components;

import java.util.List;

import com.vaadin.flow.component.button.testbench.ButtonElement;
import com.vaadin.testbench.TestBenchElement;
import com.vaadin.testbench.elementsbase.Element;

/**
 * The TestBench element that ships with {@link RatingStars} (UC9), so that
 * end-to-end tests write {@code $(RatingStarsElement.class).first().rate(4)}
 * instead of knowing how the stars are built.
 */
@Element("rating-stars")
public class RatingStarsElement extends TestBenchElement {

    public void rate(int stars) {
        stars().get(stars - 1).click();
    }

    public int getRating() {
        return (int) stars().stream()
                .filter(star -> star.getClassNames().contains("on")).count();
    }

    private List<ButtonElement> stars() {
        return $(ButtonElement.class).all();
    }
}
