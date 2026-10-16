package com.example.uc4;

import java.util.List;

import com.example.components.Sparkline;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = SparklineView.class)
class SparklineViewTest extends SpringBrowserlessTest {

    @Test
    void eachRowDrawsItsValuesAsAnSvgPolyline() {
        SparklineView view = navigate(SparklineView.class);

        Sparkline chart = (Sparkline) test(view.grid()).getCellComponent(0,
                "chart");

        assertEquals("svg", chart.getElement().getTag());
        assertEquals(14, chart.getValues().size());
        String points = chart.getElement().getChild(1).getAttribute("points");
        assertEquals(14, points.split(" ").length);
        assertTrue(chart.getThemeNames().contains("error"));
        assertEquals("Open tickets, last 14 days",
                chart.getElement().getAttribute("aria-label"));
    }

    @Test
    void nextDayShiftsTheData() {
        SparklineView view = navigate(SparklineView.class);
        List<Double> before = view.metrics().getFirst().values();

        test(findInView(Button.class).single()).click();

        List<Double> after = view.metrics().getFirst().values();
        assertEquals(before.subList(1, 14), after.subList(0, 13));
        assertNotEquals(before.getLast(), after.getLast());
    }
}
