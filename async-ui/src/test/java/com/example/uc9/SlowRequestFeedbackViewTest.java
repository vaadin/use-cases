package com.example.uc9;

import java.util.List;

import com.example.ManualLatency;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = SlowRequestFeedbackView.class)
class SlowRequestFeedbackViewTest extends SpringBrowserlessTest {

    @Autowired
    private ManualLatency latency;

    @BeforeEach
    void resetLatency() {
        latency.reset();
    }

    @Test
    void viewRendersTheThreeTools() {
        navigate(SlowRequestFeedbackView.class);

        assertEquals("UC9 — Feedback on slow requests",
                findInView(H1.class).single().getText());
        assertTrue(button("Place order").isDisableOnClick());
        assertEquals(ValueChangeMode.LAZY,
                findInView(TextField.class).single().getValueChangeMode());
    }

    @Test
    void protectedButtonPlacesAnOrderAndComesBack() {
        navigate(SlowRequestFeedbackView.class);
        Button order = button("Place order");

        test(order).click();

        assertEquals(List.of(SlowRequestFeedbackView.ORDER_LATENCY),
                latency.blocked());
        assertTrue(order.isEnabled(), "the button is re-enabled when done");
        assertTrue(hasText("Orders placed: 1"));
    }

    @Test
    void loadingIndicatorDelayFollowsTheField() {
        navigate(SlowRequestFeedbackView.class);
        IntegerField delay = findInView(IntegerField.class).single();
        assertEquals(UI.getCurrent().getLoadingIndicatorConfiguration()
                .getFirstDelay(), delay.getValue());

        test(delay).setValue(1200);

        assertEquals(1200, UI.getCurrent().getLoadingIndicatorConfiguration()
                .getFirstDelay());
    }

    @Test
    void searchModeAndRequestCounter() {
        navigate(SlowRequestFeedbackView.class);
        TextField search = findInView(TextField.class).single();

        test(findInView(Checkbox.class).single()).click();
        assertEquals(ValueChangeMode.EAGER, search.getValueChangeMode());

        test(search).setValue("l");
        test(search).setValue("la");
        assertTrue(hasText("Search requests sent: 2"));
    }

    private Button button(String text) {
        return findInView(Button.class).all().stream()
                .filter(button -> text.equals(button.getText())).findFirst()
                .orElseThrow();
    }

    private boolean hasText(String text) {
        return findInView(Span.class).all().stream()
                .anyMatch(span -> text.equals(span.getText()));
    }
}
