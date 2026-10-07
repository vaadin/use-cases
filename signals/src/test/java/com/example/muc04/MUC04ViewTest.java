package com.example.muc04;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.textfield.TextField;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = MUC04View.class)
@WithMockUser
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class MUC04ViewTest extends SpringBrowserlessTest {

    private static final SignalFieldHighlighter.User OTHER_USER = new SignalFieldHighlighter.User(
            99, "otherUser", 1);

    @Autowired
    private MUC04Signals muc04Signals;

    private TextField getFieldByLabel(String label) {
        return findInView(TextField.class).all().stream()
                .filter(f -> label.equals(f.getLabel())).findFirst()
                .orElseThrow();
    }

    @Test
    void viewRendersWithThreeEditableFields() {
        navigate(MUC04View.class);

        assertEquals(3, findInView(TextField.class).all().size());
        assertTrue(findInView(Button.class).all().stream()
                .anyMatch(b -> "Save Changes".equals(b.getText())));
    }

    @Test
    void fieldsEnabledWhenNoEditors() {
        navigate(MUC04View.class);
        runPendingSignalsTasks();

        findInView(TextField.class).all().forEach(f -> assertTrue(f.isEnabled(),
                "Field '" + f.getLabel() + "' should be enabled"));
    }

    @Test
    void fieldsRemainEnabledWhenLockingDisabled() {
        navigate(MUC04View.class);
        runPendingSignalsTasks();

        muc04Signals.startEditing("companyName", OTHER_USER);
        runPendingSignalsTasks();

        assertTrue(getFieldByLabel("Company Name").isEnabled(),
                "Field should remain enabled when locking is off");
        assertTrue(getFieldByLabel("Address").isEnabled());
        assertTrue(getFieldByLabel("Phone Number").isEnabled());
    }

    @Test
    void fieldDisabledWhenLockingEnabledAndOtherUserEdits() {
        navigate(MUC04View.class);
        runPendingSignalsTasks();

        Checkbox lockingCheckbox = findInView(Checkbox.class).first();
        test(lockingCheckbox).click();
        runPendingSignalsTasks();

        muc04Signals.startEditing("companyName", OTHER_USER);
        runPendingSignalsTasks();

        assertFalse(getFieldByLabel("Company Name").isEnabled(),
                "Field should be disabled when locking is on and another user is editing");
        assertTrue(getFieldByLabel("Address").isEnabled());
        assertTrue(getFieldByLabel("Phone Number").isEnabled());
    }

    @Test
    void fieldReenabledWhenLockingToggledOff() {
        navigate(MUC04View.class);
        runPendingSignalsTasks();

        Checkbox lockingCheckbox = findInView(Checkbox.class).first();
        test(lockingCheckbox).click();
        muc04Signals.startEditing("companyName", OTHER_USER);
        runPendingSignalsTasks();

        assertFalse(getFieldByLabel("Company Name").isEnabled());

        test(lockingCheckbox).click();
        runPendingSignalsTasks();

        assertTrue(getFieldByLabel("Company Name").isEnabled(),
                "Field should be re-enabled when locking is toggled off");
    }

    @Test
    void editingFieldUpdatesSharedSignalValue() {
        navigate(MUC04View.class);
        runPendingSignalsTasks();

        TextField companyNameField = getFieldByLabel("Company Name");
        test(companyNameField).setValue("Acme Corp");
        runPendingSignalsTasks();

        assertEquals("Acme Corp", muc04Signals.getCompanyNameSignal().peek());
    }

    @Test
    void sharedValueFromOtherUserAppearsInField() {
        navigate(MUC04View.class);
        runPendingSignalsTasks();

        muc04Signals.getAddressSignal().set("123 Main St");
        runPendingSignalsTasks();

        TextField addressField = getFieldByLabel("Address");
        assertEquals("123 Main St", addressField.getValue());
    }
}
