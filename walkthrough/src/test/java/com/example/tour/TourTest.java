package com.example.tour;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TourTest {

    @Test
    void aTourWithoutStepsDoesNotStart() {
        Tour tour = new Tour(List.of());

        tour.start();

        assertEquals(Tour.Status.NOT_STARTED, tour.state().peek().status());
    }
}
