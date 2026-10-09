package com.example.uc6;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.textfield.TextField;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = BookingSummaryView.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class BookingSummaryViewTest extends SpringBrowserlessTest {

    @Autowired
    private SeatInventory inventory;

    @Test
    void confirmIsDisabledUntilTheBookingIsComplete() {
        navigate(BookingSummaryView.class);
        assertFalse(button("Confirm booking").isEnabled());
    }

    @Test
    void confirmingSellsTheHeldSeat() {
        navigate(SeatSelectionView.class);
        runPendingSignalsTasks();
        test(button("B1")).click();
        navigate(PassengerView.class);
        test(findInView(TextField.class).single()).setValue("Alex Doe");

        navigate(BookingSummaryView.class);
        assertTrue(button("Confirm booking").isEnabled());
        test(button("Confirm booking")).click();

        assertEquals(SeatInventory.SOLD, inventory.holders().peek().get("B1"));
        assertFalse(button("Confirm booking").isEnabled());
    }

    private Button button(String text) {
        return findInView(Button.class).all().stream()
                .filter(b -> text.equals(b.getText())).findFirst()
                .orElseThrow();
    }
}
