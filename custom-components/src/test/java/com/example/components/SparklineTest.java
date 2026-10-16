package com.example.components;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The scaling of values to the SVG view box, without any UI. */
class SparklineTest {

    @Test
    void pointsAreScaledToTheViewBox() {
        assertEquals("0.0,28.0 50.0,2.0 100.0,15.0",
                Sparkline.points(List.of(0.0, 10.0, 5.0)));
    }

    @Test
    void flatAndEmptySeriesAreDrawnToo() {
        assertEquals("0.0,28.0 100.0,28.0",
                Sparkline.points(List.of(7.0, 7.0)));
        assertEquals("", Sparkline.points(List.of()));
    }
}
