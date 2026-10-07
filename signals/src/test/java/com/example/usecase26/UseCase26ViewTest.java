package com.example.usecase26;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.TextField;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = UseCase26View.class)
@WithMockUser
class UseCase26ViewTest extends SpringBrowserlessTest {

    @SuppressWarnings("unchecked")
    private ComboBox<Country> countryCombo() {
        return (ComboBox<Country>) findInView(ComboBox.class).all().stream()
                .filter(c -> "Country".equals(c.getLabel())).findFirst()
                .orElseThrow();
    }

    private void selectCountry(String country) {
        test(countryCombo()).selectItem(country);
        runPendingSignalsTasks();
    }

    @Test
    void noAddressFormShownInitially() {
        navigate(UseCase26View.class);
        runPendingSignalsTasks();

        // Only the country ComboBox should be visible, no address TextFields
        assertEquals(1, findInView(ComboBox.class).all().size());
        assertEquals(0, findInView(TextField.class).all().size());
    }

    @Test
    void selectingUSCreatesUSForm() {
        navigate(UseCase26View.class);
        runPendingSignalsTasks();

        selectCountry("US");

        // US fields should appear
        assertTrue(findInView(TextField.class).all().stream()
                .anyMatch(f -> "Street".equals(f.getLabel())));
        assertTrue(findInView(TextField.class).all().stream()
                .anyMatch(f -> "ZIP".equals(f.getLabel())));
        // State ComboBox should appear
        assertTrue(findInView(ComboBox.class).all().stream()
                .anyMatch(c -> "State".equals(c.getLabel())));
    }

    @Test
    void selectingJapanCreatesJapanForm() {
        navigate(UseCase26View.class);
        runPendingSignalsTasks();

        selectCountry("JAPAN");

        assertTrue(findInView(TextField.class).all().stream()
                .anyMatch(f -> "Postal Code".equals(f.getLabel())));
        assertTrue(findInView(ComboBox.class).all().stream()
                .anyMatch(c -> "Prefecture".equals(c.getLabel())));
    }

    @Test
    void switchingCountryHidesPreviousForm() {
        navigate(UseCase26View.class);
        runPendingSignalsTasks();

        selectCountry("US");
        assertTrue(findInView(TextField.class).all().stream()
                .anyMatch(f -> "Street".equals(f.getLabel())));

        selectCountry("JAPAN");
        // US fields hidden
        assertFalse(findInView(TextField.class).all().stream()
                .anyMatch(f -> "Street".equals(f.getLabel())));
        // Japan fields visible
        assertTrue(findInView(TextField.class).all().stream()
                .anyMatch(f -> "Postal Code".equals(f.getLabel())));
    }

    @Test
    void createOncePatternKeepsComponents() {
        navigate(UseCase26View.class);
        runPendingSignalsTasks();

        // US -> Japan -> US
        selectCountry("US");
        selectCountry("JAPAN");
        selectCountry("US");

        // Only 1 "Created US" log entry (create-once pattern)
        long usCreationCount = findInView(Span.class).all().stream()
                .filter(s -> "Created US address form".equals(s.getText()))
                .count();
        assertEquals(1, usCreationCount);
    }

    @Test
    void destroyPatternRecreatesComponents() {
        navigate(UseCase26View.class);
        runPendingSignalsTasks();

        // Japan -> US -> Japan
        selectCountry("JAPAN");
        selectCountry("US");
        selectCountry("JAPAN");

        // 2 creation events + 1 destroy event for Japan
        long jpCreationCount = findInView(Span.class).all().stream()
                .filter(s -> "Created Japan address form".equals(s.getText()))
                .count();
        assertEquals(2, jpCreationCount);

        long jpDestroyCount = findInView(Span.class).all().stream()
                .filter(s -> "Destroyed Japan address form".equals(s.getText()))
                .count();
        assertEquals(1, jpDestroyCount);
    }

    @Test
    void creationLogUpdatesOnCountryChange() {
        navigate(UseCase26View.class);
        runPendingSignalsTasks();

        // Log starts empty
        long logEntries = findInView(Span.class).all().stream().filter(
                s -> s.getText() != null && s.getText().startsWith("Created"))
                .count();
        assertEquals(0, logEntries);

        // Select a country -> log gains an entry
        selectCountry("US");

        logEntries = findInView(Span.class).all().stream().filter(
                s -> s.getText() != null && s.getText().startsWith("Created"))
                .count();
        assertEquals(1, logEntries);
    }

    @Test
    void switchingBetweenNonJapanCountriesNeverDestroysJapan() {
        navigate(UseCase26View.class);
        runPendingSignalsTasks();

        selectCountry("US");
        selectCountry("JAPAN");
        selectCountry("US");
        selectCountry("JAPAN");
        selectCountry("US");

        // No spurious destroy messages when switching away from US
        long destroyCount = findInView(Span.class).all().stream().filter(
                s -> s.getText() != null && s.getText().startsWith("Destroyed"))
                .count();
        // Japan was switched away from twice
        assertEquals(2, destroyCount);
    }

    @Test
    void japanDestroyOnlyOnSwitchAwayFromJapan() {
        navigate(UseCase26View.class);
        runPendingSignalsTasks();

        selectCountry("JAPAN");
        selectCountry("US");
        selectCountry("JAPAN");
        selectCountry("US");

        // Japan created twice, destroyed twice
        long jpCreated = findInView(Span.class).all().stream()
                .filter(s -> "Created Japan address form".equals(s.getText()))
                .count();
        assertEquals(2, jpCreated);

        long jpDestroyed = findInView(Span.class).all().stream()
                .filter(s -> "Destroyed Japan address form".equals(s.getText()))
                .count();
        assertEquals(2, jpDestroyed);

        // Create-once US: created exactly once
        assertEquals(1,
                findInView(Span.class).all().stream().filter(
                        s -> "Created US address form".equals(s.getText()))
                        .count());
    }
}
