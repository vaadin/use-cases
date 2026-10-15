package com.example.uc6;

import java.time.LocalDate;

import com.example.daterangepicker.DateRangePicker;
import com.example.daterangepicker.DateRangePickerTester;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.TextField;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = OneFieldView.class)
class OneFieldViewTest extends SpringBrowserlessTest {

    private final LocalDate today = LocalDate.now();

    @Test
    void theRangeIsOneInput() {
        navigate(OneFieldView.class);
        assertTrue(findInView(DateRangePicker.class).single().getElement()
                .getProperty("singleInput", false));
        assertEquals("40 orders", count());
    }

    @Test
    void periodAndCustomerFilterTogether() {
        navigate(OneFieldView.class);
        new DateRangePickerTester(findInView(DateRangePicker.class).single())
                .pick(today.minusDays(14), today);
        // Today and every third day back to 12 days ago.
        assertEquals("5 orders", count());

        // The orders cycle through six customers, starting with this one.
        test(findInView(TextField.class).single()).setValue("nordwind");
        assertEquals("1 order", count());
    }

    private String count() {
        return findInView(Span.class).withClassName("summary").single()
                .getText();
    }
}
