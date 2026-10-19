package com.example.uc1;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.textfield.TextField;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = SelectAllOnFocusView.class)
class SelectAllOnFocusViewTest extends SpringBrowserlessTest {

    @Test
    void quantityIsSelectedOnEveryFocusWithAutoselect() {
        navigate(SelectAllOnFocusView.class);

        TextField quantity = find(TextField.class).single();
        assertEquals("10", quantity.getValue());
        // The browser selects the value on focus because of this property;
        // the view makes no server-side selection call.
        assertTrue(quantity.isAutoselect());
    }
}
