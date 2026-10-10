package com.example.uc6;

import com.example.SupportedLocales;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.textfield.TextField;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = RightToLeftView.class)
class RightToLeftViewTest extends SpringBrowserlessTest {

    @Test
    void rendersLeftToRightInEnglish() {
        RightToLeftView view = navigate(RightToLeftView.class);
        runPendingSignalsTasks();

        assertEquals("UC6 — Right-to-left layout",
                findInView(H1.class).single().getText());
        assertEquals("Shipping details",
                findInView(H2.class).single().getText());
        assertEquals("dir=\"ltr\"", view.direction());
        assertTrue(view.sample().hasClassName("mirror-icons"));
        // Only the arrows mirror; the check mark, phone and search do not.
        assertEquals(2, findInView(Icon.class).all().stream()
                .filter(i -> i.hasClassName("mirror-rtl")).count());
    }

    @Test
    void arabicAndHebrewSwitchToRightToLeft() {
        RightToLeftView view = navigate(RightToLeftView.class);

        test(button("العربية")).click();
        runPendingSignalsTasks();
        assertEquals("dir=\"rtl\"", view.direction());
        assertEquals(SupportedLocales.ARABIC, UI.getCurrent().getLocale());
        assertEquals("بيانات الشحن", findInView(H2.class).single().getText());

        test(button("עברית")).click();
        runPendingSignalsTasks();
        assertEquals("dir=\"rtl\"", view.direction());
        assertTrue(findInView(TextField.class).all().stream()
                .anyMatch(f -> "טלפון".equals(f.getLabel())));

        test(button("Deutsch")).click();
        runPendingSignalsTasks();
        assertEquals("dir=\"ltr\"", view.direction());
    }

    @Test
    void enteredValuesSurviveALanguageSwitch() {
        navigate(RightToLeftView.class);
        TextField name = findInView(TextField.class).first();
        test(name).setValue("Maria");

        test(button("עברית")).click();
        runPendingSignalsTasks();

        assertEquals("Maria", name.getValue());
        assertEquals("שם", name.getLabel());
    }

    @Test
    void iconMirroringCanBeTurnedOff() {
        RightToLeftView view = navigate(RightToLeftView.class);

        test(view.mirrorIcons()).click();
        runPendingSignalsTasks();

        assertFalse(view.sample().hasClassName("mirror-icons"));
    }

    private Button button(String text) {
        return findInView(Button.class).all().stream()
                .filter(b -> text.equals(b.getText())).findFirst()
                .orElseThrow();
    }
}
