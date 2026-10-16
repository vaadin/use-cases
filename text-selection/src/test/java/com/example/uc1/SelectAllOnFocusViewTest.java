package com.example.uc1;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.textfield.TextField;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = SelectAllOnFocusView.class)
class SelectAllOnFocusViewTest extends SpringBrowserlessTest {

    @Test
    void viewRendersWithExpectedHeadingAndPrefilledQuantity() {
        navigate(SelectAllOnFocusView.class);

        assertTrue(findInView(H1.class).all().stream().anyMatch(
                h -> "UC1 — Select all on focus".equals(h.getText())));
        assertEquals("10", find(TextField.class).single().getValue());
    }

    @Test
    void autoselectIsEnabled() {
        navigate(SelectAllOnFocusView.class);
        assertTrue(find(TextField.class).single().isAutoselect(),
                "the field should drive selection via setAutoselect(true)");
    }
}
