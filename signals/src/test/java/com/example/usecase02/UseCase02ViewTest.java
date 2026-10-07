package com.example.usecase02;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.textfield.TextField;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = UseCase02View.class)
@WithMockUser
class UseCase02ViewTest extends SpringBrowserlessTest {

    // findInView() only returns VISIBLE components. Hidden sections' children
    // are not found. We test visibility by checking component presence.

    @Test
    void visaSectionHiddenByDefault() {
        navigate(UseCase02View.class);
        runPendingSignalsTasks();

        // Only needsVisa checkbox visible; visa type ComboBox is in hidden
        // section
        assertEquals(1, findInView(Checkbox.class).all().size());
        assertFalse(findInView(ComboBox.class).all().stream()
                .anyMatch(c -> "Visa Type".equals(c.getLabel())));
    }

    @Test
    void checkingVisaShowsVisaSection() {
        navigate(UseCase02View.class);

        test(findInView(Checkbox.class).single()).click();
        runPendingSignalsTasks();

        // Visa type ComboBox now visible (in visaSection)
        assertTrue(findInView(ComboBox.class).all().stream()
                .anyMatch(c -> "Visa Type".equals(c.getLabel())));
        // H1B section also visible (default type is H1B)
        // -> hasH1BPreviouslyCheckbox visible
        assertEquals(2, findInView(Checkbox.class).all().size());
    }

    @Test
    void h1bFieldsVisibleWhenVisaCheckedAndH1BSelected() {
        navigate(UseCase02View.class);

        test(findInView(Checkbox.class).single()).click();
        runPendingSignalsTasks();

        // H1B is default -> "Specialty Occupation" field should be visible
        assertTrue(findInView(TextField.class).all().stream()
                .anyMatch(f -> "Specialty Occupation".equals(f.getLabel())));
    }

    @Test
    void previousH1BFieldsHiddenByDefault() {
        navigate(UseCase02View.class);

        test(findInView(Checkbox.class).single()).click();
        runPendingSignalsTasks();

        // hasH1BPreviously is false -> previous employer fields hidden
        assertFalse(findInView(TextField.class).all().stream()
                .anyMatch(f -> "Previous Employer".equals(f.getLabel())));
    }

    @Test
    void previousH1BFieldsVisibleWhenChecked() {
        navigate(UseCase02View.class);

        // Check needsVisa
        test(findInView(Checkbox.class).single()).click();
        runPendingSignalsTasks();

        // Check hasH1BPreviously
        Checkbox hasH1BPreviously = findInView(Checkbox.class).all().stream()
                .filter(c -> c.getLabel().contains("H1-B")).findFirst()
                .orElseThrow();
        test(hasH1BPreviously).click();
        runPendingSignalsTasks();

        // Previous employer fields now visible
        assertTrue(findInView(TextField.class).all().stream()
                .anyMatch(f -> "Previous Employer".equals(f.getLabel())));
        assertTrue(findInView(TextField.class).all().stream().anyMatch(
                f -> "Previous Petition Number".equals(f.getLabel())));
    }

    @SuppressWarnings("unchecked")
    @Test
    void l1FieldsVisibleWhenL1Selected() {
        navigate(UseCase02View.class);

        test(findInView(Checkbox.class).single()).click();
        runPendingSignalsTasks();

        // Change visa type to L1
        ComboBox<VisaType> visaTypeSelect = (ComboBox<VisaType>) findInView(
                ComboBox.class).all().stream()
                .filter(c -> "Visa Type".equals(c.getLabel())).findFirst()
                .orElseThrow();
        test(visaTypeSelect).selectItem("L1");
        runPendingSignalsTasks();

        // L1 fields visible, H1B fields hidden
        assertTrue(findInView(TextField.class).all().stream()
                .anyMatch(f -> "Parent Company Name".equals(f.getLabel())));
        assertFalse(findInView(TextField.class).all().stream()
                .anyMatch(f -> "Specialty Occupation".equals(f.getLabel())));
    }

    @SuppressWarnings("unchecked")
    @Test
    void o1FieldsVisibleWhenO1Selected() {
        navigate(UseCase02View.class);

        test(findInView(Checkbox.class).single()).click();
        runPendingSignalsTasks();

        ComboBox<VisaType> visaTypeSelect = (ComboBox<VisaType>) findInView(
                ComboBox.class).all().stream()
                .filter(c -> "Visa Type".equals(c.getLabel())).findFirst()
                .orElseThrow();
        test(visaTypeSelect).selectItem("O1");
        runPendingSignalsTasks();

        assertTrue(findInView(TextField.class).all().stream().anyMatch(
                f -> "Field of Extraordinary Ability".equals(f.getLabel())));
    }

    @Test
    void uncheckingVisaHidesAllSections() {
        navigate(UseCase02View.class);

        // Check visa
        test(findInView(Checkbox.class).single()).click();
        runPendingSignalsTasks();
        assertTrue(findInView(ComboBox.class).all().size() > 0);

        // Uncheck visa
        Checkbox needsVisa = findInView(Checkbox.class).all().stream()
                .filter(c -> c.getLabel().contains("visa sponsorship"))
                .findFirst().orElseThrow();
        test(needsVisa).click();
        runPendingSignalsTasks();

        // No ComboBoxes visible (visa type is hidden)
        assertFalse(findInView(ComboBox.class).all().stream()
                .anyMatch(c -> "Visa Type".equals(c.getLabel())));
        // Only 1 checkbox remains visible
        assertEquals(1, findInView(Checkbox.class).all().size());
    }
}
