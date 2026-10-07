package com.example.usecase05;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.combobox.ComboBox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = UseCase05View.class)
@WithMockUser
class UseCase05ViewTest extends SpringBrowserlessTest {

    @Test
    void viewRendersThreeComboBoxes() {
        navigate(UseCase05View.class);
        assertEquals(3, findInView(ComboBox.class).all().size());
    }

    @SuppressWarnings("unchecked")
    @Test
    void countryHasDefaultValue() {
        navigate(UseCase05View.class);
        runPendingSignalsTasks();

        ComboBox<String> countrySelect = findInView(ComboBox.class).atIndex(1);
        assertEquals("United States", countrySelect.getValue());
    }

    @SuppressWarnings("unchecked")
    @Test
    void stateEnabledWhenCountrySelected() {
        navigate(UseCase05View.class);
        runPendingSignalsTasks();

        ComboBox<String> stateSelect = findInView(ComboBox.class).atIndex(2);
        assertTrue(stateSelect.isEnabled());
    }

    @SuppressWarnings("unchecked")
    @Test
    void cityDisabledWhenNoStateSelected() {
        navigate(UseCase05View.class);
        runPendingSignalsTasks();

        ComboBox<String> citySelect = findInView(ComboBox.class).atIndex(3);
        assertFalse(citySelect.isEnabled());
    }

    @SuppressWarnings("unchecked")
    @Test
    void selectingStateEnablesCity() {
        navigate(UseCase05View.class);
        runPendingSignalsTasks();

        ComboBox<String> stateSelect = findInView(ComboBox.class).atIndex(2);
        test(stateSelect).selectItem("California");
        runPendingSignalsTasks();

        ComboBox<String> citySelect = findInView(ComboBox.class).atIndex(3);
        assertTrue(citySelect.isEnabled());
    }

    @SuppressWarnings("unchecked")
    @Test
    void changingCountryResetsState() {
        navigate(UseCase05View.class);
        runPendingSignalsTasks();

        // Select a state first
        ComboBox<String> stateSelect = findInView(ComboBox.class).atIndex(2);
        test(stateSelect).selectItem("California");
        runPendingSignalsTasks();

        // Change country
        ComboBox<String> countrySelect = findInView(ComboBox.class).atIndex(1);
        test(countrySelect).selectItem("Canada");
        runPendingSignalsTasks();

        // State should be reset (null or empty after country change)
        var stateValue = stateSelect.getValue();
        assertTrue(stateValue == null || stateValue.isEmpty());
    }
}
