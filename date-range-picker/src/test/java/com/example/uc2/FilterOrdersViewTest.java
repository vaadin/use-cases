package com.example.uc2;

import java.time.LocalDate;

import com.example.daterangepicker.DateRangePicker;
import com.example.daterangepicker.DateRangePickerTester;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.datepicker.DateRange;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = FilterOrdersView.class)
class FilterOrdersViewTest extends SpringBrowserlessTest {

    private final LocalDate today = LocalDate.now();

    @Test
    void allOrdersAreShownWithoutAPeriod() {
        navigate(FilterOrdersView.class);
        assertEquals(40, rows());
        assertTrue(summary().startsWith("All orders: 40 orders"), summary());
    }

    @Test
    void aQuickButtonSetsThePeriodAndFilters() {
        navigate(FilterOrdersView.class);
        test(findInView(Button.class).withText("Last 7 days").single()).click();

        assertEquals(new DateRange(today.minusDays(6), today),
                findInView(DateRangePicker.class).single().getValue());
        // An order every third day: today, 3 and 6 days ago.
        assertEquals(3, rows());
    }

    @Test
    void aPeriodWithOnlyAStartFiltersFromThatDay() {
        navigate(FilterOrdersView.class);
        new DateRangePickerTester(findInView(DateRangePicker.class).single())
                .pick(today.minusDays(10), null);

        // Today, 3, 6 and 9 days ago.
        assertEquals(4, rows());
        assertTrue(summary().startsWith("Since "), summary());
    }

    @Test
    void periodsAreDescribedForEitherOpenEnd() {
        LocalDate day = LocalDate.of(2026, 9, 1);
        assertEquals("Until 1 Sep 2026", FilterOrdersView.describe(null, day));
        assertEquals("1 Sep 2026 – 1 Sep 2026",
                FilterOrdersView.describe(day, day));
    }

    private int rows() {
        @SuppressWarnings("unchecked")
        Grid<Object> grid = findInView(Grid.class).single();
        return test(grid).size();
    }

    private String summary() {
        return findInView(Span.class).withClassName("summary").single()
                .getText();
    }
}
