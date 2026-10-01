package com.example.uc6;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.TextField;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = PassengerView.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class PassengerViewTest extends SpringBrowserlessTest {

    @Test
    void viewShowsTheSeatPickedInTheFirstStep() {
        navigate(PassengerView.class);
        assertEquals("2. Who is coming?",
                findInView(H2.class).single().getText());
        assertTrue(hasText("No seat picked yet."));

        navigate(SeatSelectionView.class);
        runPendingSignalsTasks();
        test(findInView(Button.class).all().stream()
                .filter(b -> "A3".equals(b.getText())).findFirst()
                .orElseThrow()).click();

        navigate(PassengerView.class);
        assertTrue(hasText("Seat A3 is held for you."));
        test(findInView(TextField.class).single()).setValue("Alex Doe");
    }

    private boolean hasText(String text) {
        return findInView(Span.class).all().stream()
                .anyMatch(s -> text.equals(s.getText()));
    }
}
