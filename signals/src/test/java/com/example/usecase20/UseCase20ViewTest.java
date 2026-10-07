package com.example.usecase20;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ViewPackages(classes = UseCase20View.class)
@WithMockUser
class UseCase20ViewTest extends SpringBrowserlessTest {

    @Test
    void viewRendersWithRadioButtonGroup() {
        navigate(UseCase20View.class);

        assertEquals(1, findInView(RadioButtonGroup.class).all().size());
    }

    @SuppressWarnings("unchecked")
    @Test
    void defaultColorIsThemeBase() {
        navigate(UseCase20View.class);
        runPendingSignalsTasks();

        RadioButtonGroup<String> colorPicker = findInView(
                RadioButtonGroup.class).single();
        assertEquals("var(--aura-background-color)", colorPicker.getValue());
    }

    @SuppressWarnings("unchecked")
    @Test
    void selectingColorUpdatesValue() {
        navigate(UseCase20View.class);

        RadioButtonGroup<String> colorPicker = findInView(
                RadioButtonGroup.class).single();
        test(colorPicker).selectItem("#ffd54f");
        runPendingSignalsTasks();

        assertEquals("#ffd54f", colorPicker.getValue());
    }
}
