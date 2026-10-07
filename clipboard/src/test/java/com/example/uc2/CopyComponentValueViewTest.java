package com.example.uc2;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.textfield.TextField;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = CopyComponentValueView.class)
class CopyComponentValueViewTest extends SpringBrowserlessTest {

    @Test
    void viewRendersHeadingFieldAndButton() {
        navigate(CopyComponentValueView.class);

        assertTrue(findInView(H1.class).all().stream()
                .anyMatch(h -> "UC2 — Copy the current value of a component"
                        .equals(h.getText())));
        assertTrue(findInView(TextField.class).all().stream()
                .anyMatch(f -> "Share link".equals(f.getLabel())));
        assertTrue(findInView(Button.class).all().stream()
                .anyMatch(b -> "Copy".equals(b.getText())));
    }

    @Test
    void editingTheTextFieldUpdatesItsValue() {
        navigate(CopyComponentValueView.class);

        TextField field = findInView(TextField.class).all().stream()
                .filter(f -> "Share link".equals(f.getLabel())).findFirst()
                .orElseThrow();
        assertEquals("https://example.com/share/abc123", field.getValue());

        test(field).setValue("https://example.com/share/xyz999");
        assertEquals("https://example.com/share/xyz999", field.getValue());
    }
}
